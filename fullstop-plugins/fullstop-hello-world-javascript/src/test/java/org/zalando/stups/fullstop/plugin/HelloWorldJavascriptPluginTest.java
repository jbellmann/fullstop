/**
 * Copyright (C) 2015 Zalando SE (http://tech.zalando.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zalando.stups.fullstop.plugin;

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent;
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEventData;
import com.amazonaws.services.cloudtrail.processinglibrary.model.internal.CloudTrailEventField;
import com.amazonaws.services.cloudtrail.processinglibrary.model.internal.UserIdentity;
import org.junit.Before;
import org.junit.Test;
import org.zalando.stups.fullstop.events.TestCloudTrailEventData;

/**
 * Created by mrandi.
 */
public class HelloWorldJavascriptPluginTest {

    private HelloWorldJavascriptPlugin helloWorldJavascriptPlugin;


    @Before
    public void setUp() {
        helloWorldJavascriptPlugin = new HelloWorldJavascriptPlugin();
    }

    @Test
    public void testSupports() throws Exception {

    }

    @Test
    public void testProcessEvent() {
        CloudTrailEventData data = new TestCloudTrailEventData("/responseElements.json");
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.add(CloudTrailEventField.accountId.name(), "0234527346");
        data.add(CloudTrailEventField.userIdentity.name(), userIdentity);

        CloudTrailEvent event = new CloudTrailEvent(data, null);

        HelloWorldJavascriptPlugin plugin = new HelloWorldJavascriptPlugin();
        plugin.processEvent(event);

    }

}