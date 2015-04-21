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
