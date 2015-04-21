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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import org.zalando.stups.fullstop.plugin.AbstractFullstopPlugin;
import org.zalando.stups.fullstop.violation.ViolationStore;

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent;

/**
 * Just for testing and showcases on how to use plugin-api and other supported components.
 *
 * @author  jbellmann
 */
@Component
public class HelloEventPlugin extends AbstractFullstopPlugin {

    private final Logger log = LoggerFactory.getLogger(HelloEventPlugin.class);

    private final ViolationStore violationStore;

    @Autowired
    public HelloEventPlugin(final ViolationStore violationStore) {
        this.violationStore = violationStore;
    }

    /**
     * Handles every events.
     */
    @Override
    public boolean supports(final CloudTrailEvent delimiter) {
        return true;
    }

    @Override
    public void processEvent(final CloudTrailEvent event) {
        final String violation = String.format("HELLO EVENT - %s", event.getEventData().getEventId());
        log.info(violation);
        violationStore.save(violation);
    }
}
