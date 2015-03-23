/*
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

package org.zalando.stups.fullstop.plugin;

import java.util.Collections;

import com.amazonaws.services.ec2.AmazonEC2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent;
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEventData;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import org.zalando.stups.fullstop.aws.ClientProvider;

/**
 * This plugin only handles EC-2 Events where name of event starts with "Delete".
 *
 * @author  jbellmann
 */
@Component
public class RunInstancePlugin implements FullstopPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(RunInstancePlugin.class);

    private static final String EC2_EVENTS = "ec2.amazonaws.com";
    private static final String DELETE = "Delete";

    private final ClientProvider clientProvider;

    @Autowired
    public RunInstancePlugin(ClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    /**
     * We use this as a filter for events.
     */
    @Override
    public boolean supports(final CloudTrailEvent event) {
        CloudTrailEventData eventData = event.getEventData();

        String eventSource = eventData.getEventSource();
        String eventName = eventData.getEventName();

        return eventSource.equals(EC2_EVENTS) && eventName.startsWith(DELETE);
    }

    @Override
    // @HystrixCommand(fallback = my coole exception)
    // command for account id and client type -> generate new credentials
    public Object processEvent(final CloudTrailEvent event) {

        String parameters = event.getEventData().getRequestParameters();
        String instanceId = getFromParameters(parameters);

        AmazonEC2Client client = clientProvider.getEC2Client(event.getEventData().getUserIdentity().getAccountId(),
                Region.getRegion(Regions.fromName(event.getEventData().getAwsRegion())));

        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setInstanceIds(Collections.singleton(instanceId));

        // try
        DescribeInstancesResult result = client.describeInstances(request);
        // catch credentials are old
        // throw new my coole exception ( account id, CLIENTTYPE.EC2, exception) -> this will trigger hystrix


        LOG.info("SAVING RESULT INTO MAGIC DB", result);
        return null;
    }

    private String getFromParameters(final String parameters) {

        // TODO Auto-generated method stub
        return null;
    }

}