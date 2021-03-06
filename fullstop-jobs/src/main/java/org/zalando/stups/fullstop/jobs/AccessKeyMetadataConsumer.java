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
package org.zalando.stups.fullstop.jobs;

import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.stups.fullstop.violation.ViolationBuilder;
import org.zalando.stups.fullstop.violation.ViolationSink;

import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static org.zalando.stups.fullstop.violation.ViolationType.ACTIVE_KEY_TO_OLD;

/**
 * @author jbellmann
 */
@Component class AccessKeyMetadataConsumer implements Consumer<AccessKeyMetadata> {

    private final ViolationSink violationSink;

    @Autowired AccessKeyMetadataConsumer(final ViolationSink violationSink) {
        this.violationSink = violationSink;
    }

    @Override
    public void accept(final AccessKeyMetadata input) {
        violationSink.put(
                new ViolationBuilder().withType(ACTIVE_KEY_TO_OLD)
                                      .withPluginFullyQualifiedClassName(AccessKeyMetadataConsumer.class)
                                      .withMetaInfo(newArrayList(input.getUserName(), input.getAccessKeyId()))
                                      .build());
    }

}
