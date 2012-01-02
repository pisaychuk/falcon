/*
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

package org.apache.ivory.messaging;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MessageProducerTest {

	private ProcessMessage msgArgs;
	
	@BeforeClass
	public void setArgs(){
		this.msgArgs = new ProcessMessage();
		this.msgArgs.setFeedTopicName("Ivory.process1.click-logs");
		this.msgArgs.setFeedInstance("/click-logs/10/05/05/00/20");
		this.msgArgs.setProcessName("process1");
		this.msgArgs.setCoordinatorName("coord-rerun");
		this.msgArgs.setWorkflowId("workflow-123");
		this.msgArgs.setExternalWorkflowId("external-0001");
		this.msgArgs.setTimeStamp("10:10:10:");
		this.msgArgs.setRunId("1");
	}
	
	@Test
	public void testProcessMessageCreator() {
		MessageProducer.main(ArgumentsResolver
				.resolveToStringArray(this.msgArgs));
  }

}
