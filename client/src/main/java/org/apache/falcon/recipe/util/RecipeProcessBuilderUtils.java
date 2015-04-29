/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.falcon.recipe.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.falcon.entity.v0.Entity;
import org.apache.falcon.entity.v0.EntityType;
import org.apache.falcon.entity.v0.Frequency;
import org.apache.falcon.entity.v0.SchemaHelper;
import org.apache.falcon.entity.v0.process.*;
import org.apache.falcon.recipe.RecipeToolOptions;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recipe builder utility.
 */
public final class RecipeProcessBuilderUtils {

    private static final Pattern RECIPE_VAR_PATTERN = Pattern.compile("##[A-Za-z0-9_.]*##");

    private RecipeProcessBuilderUtils() {
    }

    public static String createProcessFromTemplate(final String processTemplateFile, final Properties recipeProperties,
                                                   final String processFilename) throws Exception {
        org.apache.falcon.entity.v0.process.Process process = bindAttributesInTemplate(
                processTemplateFile, recipeProperties);
        String recipeProcessFilename = createProcessXmlFile(processFilename, process);

        validateProcessXmlFile(recipeProcessFilename);
        return recipeProcessFilename;
    }

    private static org.apache.falcon.entity.v0.process.Process
    bindAttributesInTemplate(final String templateFile, final Properties recipeProperties)
        throws Exception {
        if (templateFile == null || recipeProperties == null) {
            throw new IllegalArgumentException("Invalid arguments passed");
        }

        Unmarshaller unmarshaller = EntityType.PROCESS.getUnmarshaller();
        // Validation can be skipped for unmarshalling as we want to bind tempalte with the properties. Vaildation is
        // hanles as part of marshalling
        unmarshaller.setSchema(null);
        unmarshaller.setEventHandler(new ValidationEventHandler() {
                public boolean handleEvent(ValidationEvent validationEvent) {
                    return true;
                }
            }
        );

        URL processResourceUrl = new File(templateFile).toURI().toURL();
        org.apache.falcon.entity.v0.process.Process process =
                (org.apache.falcon.entity.v0.process.Process) unmarshaller.unmarshal(processResourceUrl);

        /* For optional properties user might directly set them in the process xml and might not set it in properties
           file. Before doing the submission validation is done to confirm process xml doesn't have RECIPE_VAR_PATTERN
        */

        String processName = recipeProperties.getProperty(RecipeToolOptions.RECIPE_NAME.getName());
        if (StringUtils.isNotEmpty(processName)) {
            process.setName(processName);
        }

        // DR process template has only one cluster
        bindClusterProperties(process.getClusters().getClusters().get(0), recipeProperties);

        // bind scheduling properties
        String processFrequency = recipeProperties.getProperty(RecipeToolOptions.PROCESS_FREQUENCY.getName());
        if (StringUtils.isNotEmpty(processName)) {
            process.setFrequency(Frequency.fromString(processFrequency));
        }

        bindWorkflowProperties(process.getWorkflow(), recipeProperties);
        bindRetryProperties(process.getRetry(), recipeProperties);
        bindCustomProperties(process.getProperties(), recipeProperties);

        return process;
    }

    private static void bindClusterProperties(final Cluster cluster,
                                              final Properties recipeProperties) {
        // DR process template has only one cluster
        String clusterName = recipeProperties.getProperty(RecipeToolOptions.CLUSTER_NAME.getName());
        if (StringUtils.isNotEmpty(clusterName)) {
            cluster.setName(clusterName);
        }

        String clusterStartValidity = recipeProperties.getProperty(RecipeToolOptions.CLUSTER_VALIDITY_START.getName());
        if (StringUtils.isNotEmpty(clusterStartValidity)) {
            cluster.getValidity().setStart(SchemaHelper.parseDateUTC(clusterStartValidity));
        }

        String clusterEndValidity = recipeProperties.getProperty(RecipeToolOptions.CLUSTER_VALIDITY_END.getName());
        if (StringUtils.isNotEmpty(clusterEndValidity)) {
            cluster.getValidity().setEnd(SchemaHelper.parseDateUTC(clusterEndValidity));
        }
    }

    private static void bindWorkflowProperties(final Workflow wf,
                                               final Properties recipeProperties) {
        String wfName = recipeProperties.getProperty(RecipeToolOptions.WORKFLOW_NAME.getName());
        if (StringUtils.isNotEmpty(wfName)) {
            wf.setName(wfName);
        }

        String wfLibPath = recipeProperties.getProperty(RecipeToolOptions.WORKFLOW_LIB_PATH.getName());
        if (StringUtils.isNotEmpty(wfLibPath)) {
            wf.setLib(wfLibPath);
        } else if (wf.getLib().startsWith("##")) {
            wf.setLib("");
        }

        String wfPath = recipeProperties.getProperty(RecipeToolOptions.WORKFLOW_PATH.getName());
        if (StringUtils.isNotEmpty(wfPath)) {
            wf.setPath(wfPath);
        }
    }

    private static void bindRetryProperties(final Retry processRetry,
                                            final Properties recipeProperties) {
        String retryPolicy = recipeProperties.getProperty(RecipeToolOptions.RETRY_POLICY.getName());
        if (StringUtils.isNotEmpty(retryPolicy)) {
            processRetry.setPolicy(PolicyType.fromValue(retryPolicy));
        }

        String retryAttempts = recipeProperties.getProperty(RecipeToolOptions.RETRY_ATTEMPTS.getName());
        if (StringUtils.isNotEmpty(retryAttempts)) {
            processRetry.setAttempts(Integer.parseInt(retryAttempts));
        }

        String retryDelay = recipeProperties.getProperty(RecipeToolOptions.RETRY_DELAY.getName());
        if (StringUtils.isNotEmpty(retryDelay)) {
            processRetry.setDelay(Frequency.fromString(retryDelay));
        }
    }

    private static void bindCustomProperties(final org.apache.falcon.entity.v0.process.Properties customProperties,
                                             final Properties recipeProperties) {
        List<Property> propertyList = new ArrayList<Property>();

        for (Map.Entry<Object, Object> recipeProperty : recipeProperties.entrySet()) {
            if (RecipeToolOptions.OPTIONSMAP.get(recipeProperty.getKey().toString()) == null) {
                addProperty(propertyList, (String) recipeProperty.getKey(), (String) recipeProperty.getValue());
            }
        }

        customProperties.getProperties().addAll(propertyList);
    }

    private static void addProperty(List<Property> propertyList, String name, String value) {
        Property prop = new Property();
        prop.setName(name);
        prop.setValue(value);
        propertyList.add(prop);
    }

    private static String createProcessXmlFile(final String outFilename,
                                               final Entity entity) throws Exception {
        if (outFilename == null || entity == null) {
            throw new IllegalArgumentException("Invalid arguments passed");
        }

        EntityType type = EntityType.PROCESS;
        OutputStream out = null;
        try {
            out = new FileOutputStream(outFilename);
            type.getMarshaller().marshal(entity, out);
        } catch (JAXBException e) {
            throw new Exception("Unable to serialize the entity object " + type + "/" + entity.getName(), e);
        } finally {
            IOUtils.closeQuietly(out);
        }
        return outFilename;
    }

    private static void validateProcessXmlFile(final String processFileName) throws Exception {
        if (processFileName == null) {
            throw new IllegalArgumentException("Invalid arguments passed");
        }

        String line;
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(processFileName));
            while ((line = reader.readLine()) != null) {
                Matcher matcher = RECIPE_VAR_PATTERN.matcher(line);
                if (matcher.find()) {
                    String variable = line.substring(matcher.start(), matcher.end());
                    throw new Exception("Match not found for the template: " + variable
                            + " in recipe template file. Please add it in recipe properties file");
                }
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

    }

}