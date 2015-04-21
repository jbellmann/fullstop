/**
 * Copyright 2015 Zalando SE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zalando.stups.fullstop.plugin.hello;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;

import org.mockito.Mockito;

import org.zalando.stups.fullstop.violation.ViolationStore;
import org.zalando.stups.fullstop.violation.store.slf4j.Slf4jViolationStore;

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent;
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEventData;

/**
 * @author  jbellmann
 */
public class HelloEvenPluginTest {

    @Test
    public void testPlugin() {
        CloudTrailEvent event = mock(CloudTrailEvent.class);
        CloudTrailEventData data = mock(CloudTrailEventData.class);
        when(event.getEventData()).thenReturn(data);
        when(data.getEventId()).thenReturn(UUID.randomUUID());

        HelloEventPlugin plugin = new HelloEventPlugin(new Slf4jViolationStore());

        plugin.processEvent(event);
    }

    @Test
    public void testPluginWithMockstore() {
        CloudTrailEvent event = mock(CloudTrailEvent.class);
        CloudTrailEventData data = mock(CloudTrailEventData.class);
        when(event.getEventData()).thenReturn(data);
        when(data.getEventId()).thenReturn(UUID.randomUUID());

        ViolationStore mockStore = Mockito.mock(ViolationStore.class);

        HelloEventPlugin plugin = new HelloEventPlugin(mockStore);

        plugin.processEvent(event);

        Mockito.verify(mockStore, atLeastOnce()).save(Mockito.anyString());
    }

}
