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

package org.apache.falcon.regression.security;

import org.apache.falcon.regression.core.bundle.Bundle;
import org.apache.falcon.regression.core.enumsAndConstants.MerlinConstants;
import org.apache.falcon.regression.core.helpers.ColoHelper;
import org.apache.falcon.regression.core.supportClasses.ExecResult;
import org.apache.falcon.regression.core.util.AssertUtil;
import org.apache.falcon.regression.core.util.BundleUtil;
import org.apache.falcon.regression.testHelper.BaseTestClass;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Tests falcon client's working in presence of ACL errors.
 */
@Test(groups = "authorization,embedded")
public class FalconClientTest extends BaseTestClass {
    private static final Logger LOGGER = Logger.getLogger(AclValidationTest.class);
    private final ColoHelper cluster = servers.get(0);

    @BeforeMethod(alwaysRun = true)
    public void setup(Method method) throws Exception {
        LOGGER.info("test name: " + method.getName());
        Bundle bundle = BundleUtil.readELBundle();
        bundles[0] = new Bundle(bundle, cluster);
        bundles[0].generateUniqueBundle();
    }

    /**
     * Test error thrown by falcon client, when acl of the submitted cluster has bad values.
     * @throws Exception
     */
    @Test (enabled = true)
    public void badClusterSubmit() throws Exception {
        bundles[0].setCLusterACL(MerlinConstants.DIFFERENT_USER_NAME,
            MerlinConstants.CURRENT_USER_GROUP, "*");
        final String clusterXml = bundles[0].getClusters().get(0);
        final ExecResult execResult = prism.getClusterHelper().clientSubmit(clusterXml);
        AssertUtil.assertFailed(execResult, "cluster submission failed");
    }

    /**
     * Test error thrown by falcon client, a user tries to delete a cluster that it should not be.
     * able to delete
     * @throws Exception
     */
    @Test(enabled = true)
    public void badClusterDelete() throws Exception {
        bundles[0].submitClusters(prism);
        final String clusterXml = bundles[0].getClusters().get(0);
        final ExecResult execResult =
            prism.getClusterHelper().clientDelete(clusterXml, MerlinConstants.DIFFERENT_USER_NAME);
        AssertUtil.assertFailed(execResult, "cluster deletion failed");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        removeBundles();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() throws IOException {
        cleanTestDirs();
    }
}
