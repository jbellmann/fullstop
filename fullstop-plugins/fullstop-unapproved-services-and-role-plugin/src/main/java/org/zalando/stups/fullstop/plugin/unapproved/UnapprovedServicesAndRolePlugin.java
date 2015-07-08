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
package org.zalando.stups.fullstop.plugin.unapproved;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent;
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEventData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.stups.fullstop.plugin.AbstractFullstopPlugin;
import org.zalando.stups.fullstop.plugin.unapproved.config.UnapprovedServicesAndRoleProperties;
import org.zalando.stups.fullstop.violation.ViolationBuilder;
import org.zalando.stups.fullstop.violation.ViolationSink;

import java.io.IOException;

import static java.lang.String.format;
import static org.zalando.stups.fullstop.events.CloudtrailEventSupport.getAccountId;
import static org.zalando.stups.fullstop.events.CloudtrailEventSupport.getRegion;

/**
 * @author mrandi
 */
@Component
public class UnapprovedServicesAndRolePlugin extends AbstractFullstopPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(UnapprovedServicesAndRolePlugin.class);

    private static final String EVENT_SOURCE = "iam.amazonaws.com";

    private final PolicyProvider policyProvider;

    private final ViolationSink violationSink;

    private final UnapprovedServicesAndRoleProperties unapprovedServicesAndRoleProperties;

    private final PolicyTemplateCaching policyTemplateCaching;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public UnapprovedServicesAndRolePlugin(final PolicyProvider policyProvider, final ViolationSink violationSink,
            final PolicyTemplateCaching policyTemplateCaching,
            UnapprovedServicesAndRoleProperties unapprovedServicesAndRoleProperties) {
        this.policyProvider = policyProvider;
        this.violationSink = violationSink;
        this.policyTemplateCaching = policyTemplateCaching;
        this.unapprovedServicesAndRoleProperties = unapprovedServicesAndRoleProperties;
    }

    @Override
    public boolean supports(final CloudTrailEvent event) {
        CloudTrailEventData cloudTrailEventData = event.getEventData();
        String eventSource = cloudTrailEventData.getEventSource();

        return eventSource.equals(EVENT_SOURCE) && (unapprovedServicesAndRoleProperties.getEventNames().contains(
                cloudTrailEventData.getEventName()))
                && (policyTemplateCaching.getS3Objects().contains(getRoleName(event)));
    }

    @Override
    public void processEvent(final CloudTrailEvent event) {

        String roleName = getRoleName(event);

        String policy = policyProvider.getPolicy(roleName, getRegion(event), getAccountId(event));

        String policyTemplate = policyTemplateCaching.getPolicyTemplate(roleName);

        JsonNode policyJson = null;
        JsonNode templatePolicyJson = null;

        try {
            policyJson = objectMapper.readTree(policy);
            templatePolicyJson = objectMapper.readTree(policyTemplate);
        }
        catch (IOException e) {
            LOG.warn("Could not read policy tree! For policy: {} and policy template:  {}", policy, policyTemplate);
        }

        if (policyJson != null && !policyJson.equals(templatePolicyJson)) {
            violationSink.put(
                    new ViolationBuilder(
                            format("Role: %s cannot be modified", roleName)).withEventId(getCloudTrailEventId(event))
                                                                            .withRegion(getCloudTrailEventRegion(event))
                                                                            .withAccountId(
                                                                                    getCloudTrailEventAccountId(
                                                                                            event))
                                                                            .build());

        }

        Policy policyObj = policyProvider.toPolicy(policy);

        for (Statement statement : policyObj.getStatements()) {
            if (statement.getEffect() == Statement.Effect.Allow) {
                for (Action action : statement.getActions()) {
                    action.getActionName().startsWith("ec2");
                }

            }
        }

    }

    private String getRoleName(CloudTrailEvent event) {
        return JsonPath.read(event.getEventData().getRequestParameters(), "$.roleName");
    }

}
