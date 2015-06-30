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
package org.zalando.stups.fullstop.plugin.ssg;

import org.assertj.core.api.Assertions;

import org.junit.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.zalando.stups.fullstop.aws.ClientProvider;
import org.zalando.stups.fullstop.plugin.AbstractPluginTest;
import org.zalando.stups.fullstop.s3.S3Service;

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent;
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEventData;

/**
 * @author  jbellmann
 */
public class SaveSecurityGroupsPluginTest extends AbstractPluginTest {

    @Autowired
    private SaveSecurityGroupsPlugin plugin;

    @Test
    public void testPlugin() {
        CloudTrailEvent event = getMockedEvent();

        Assertions.assertThat(plugin.supports(event)).isTrue();
    }

    protected CloudTrailEvent getMockedEvent() {
        CloudTrailEvent event = Mockito.mock(CloudTrailEvent.class);
        CloudTrailEventData eventData = Mockito.mock(CloudTrailEventData.class);
        Mockito.when(event.getEventData()).thenReturn(eventData);
        Mockito.when(eventData.getEventSource()).thenReturn("ec2.amazonaws.com");
        Mockito.when(eventData.getEventName()).thenReturn("RunInstances");
        return event;
    }

    @Configuration
    static class TestConfig {

        @Bean
        public ClientProvider clientProvider() {
            return Mockito.mock(ClientProvider.class);
        }

        @Bean
        public S3Service s3Service() {
            return Mockito.mock(S3Service.class);
        }
    }
}
