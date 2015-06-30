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

import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent;
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEventData;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zalando.stups.fullstop.plugin.AbstractFullstopPlugin;
import org.zalando.stups.fullstop.s3.S3Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import static org.joda.time.DateTimeZone.UTC;
import static org.zalando.stups.fullstop.events.CloudtrailEventSupport.*;

/**
 * @author gkneitschel
 */
@Component
public class SaveSecurityGroupsPlugin extends AbstractFullstopPlugin {

    public static final String SECURITY_GROUPS = "security-groups-";

    public static final String JSON = ".json";

    private static final Logger LOG = LoggerFactory.getLogger(SaveSecurityGroupsPlugin.class);

    private static final String EC2_SOURCE_EVENTS = "ec2.amazonaws.com";

    private static final String EVENT_NAME = "RunInstances";

    private final S3Service s3Writer;

    private final SecurityGroupProvider securityGroupProvider;

    @Value("${fullstop.instanceData.bucketName}")
    private String bucketName;

    @Autowired
    public SaveSecurityGroupsPlugin(final SecurityGroupProvider securityGroupProvider, final S3Service s3Writer) {
        this.securityGroupProvider = securityGroupProvider;
        this.s3Writer = s3Writer;
    }

    @Override
    public boolean supports(final CloudTrailEvent event) {
        CloudTrailEventData cloudTrailEventData = event.getEventData();
        String eventSource = cloudTrailEventData.getEventSource();
        String eventName = cloudTrailEventData.getEventName();

        return EC2_SOURCE_EVENTS.equals(eventSource) && EVENT_NAME.equals(eventName);
    }

    @Override
    public void processEvent(final CloudTrailEvent event) {

        List<String> securityGroupIds = readSecurityGroupIds(event);

        Region region = getRegion(event);
        String accountId = getAccountId(event);
        List<String> instanceIds = getInstanceIds(event);
        if (instanceIds.isEmpty()) {
            LOG.warn("No instanceIds for event : {}, skip processing", getCloudTrailEventId(event));
            return;
        }

        DateTime instanceLaunchTime;
        try {

            instanceLaunchTime = new DateTime(getInstanceLaunchTime(event).get(0));
        }
        catch (Exception e) {
            LOG.warn("No 'launchTime' for event : {}, skip processing", getCloudTrailEventId(event));
            return;
        }

        String securityGroup = getSecurityGroup(securityGroupIds, region, accountId);

        String prefix = PrefixBuilder.build(accountId, region.getName(), instanceLaunchTime);

        List<String> s3InstanceObjects = listS3Objects(bucketName, prefix);

        for (String instanceId : instanceIds) {

            List<String> instanceBuckets = Lists.newArrayList();

            for (String s3InstanceObject : s3InstanceObjects) {
                String s = Paths.get(s3InstanceObject).getFileName().toString();
                if (s.startsWith(instanceId)) {
                    instanceBuckets.add(s);
                }
            }

            if (instanceBuckets.isEmpty()) {
                continue;
            }

            String instanceBucketNameControlElement = null;
            DateTime instanceBootTimeControlElement = null;

            for (String instanceBucket : instanceBuckets) {
                List<String> currentBucket = Lists.newArrayList(
                        Splitter.on('-').limit(3).trimResults()
                                .omitEmptyStrings().split(instanceBucket));

                String currentBucketName = currentBucket.get(0) + "-" + currentBucket.get(1);
                DateTime currentBucketDate = new DateTime(currentBucket.get(2), UTC);

                // TODO we should use absolute values
                if (instanceBucketNameControlElement != null || instanceBootTimeControlElement != null) {
                    if (instanceLaunchTime.getMillis() - currentBucketDate.getMillis()
                            < instanceLaunchTime.getMillis() - instanceBootTimeControlElement.getMillis()) {

                        instanceBucketNameControlElement = currentBucketName;
                        instanceBootTimeControlElement = currentBucketDate;
                    }
                }
                else {
                    instanceBucketNameControlElement = currentBucketName;
                    instanceBootTimeControlElement = currentBucketDate;
                }
            }

            prefix = prefix + instanceBucketNameControlElement + "-" + instanceBootTimeControlElement;
            writeToS3(securityGroup, prefix, instanceId);
        }
    }

    public String getSecurityGroup(final List<String> securityGroupIds, final Region region, final String accountId) {
        return securityGroupProvider.getSecurityGroup(securityGroupIds, region, accountId);
    }

    protected List<String> readSecurityGroupIds(final CloudTrailEvent cloudTrailEvent) {
        return read(cloudTrailEvent, SECURITY_GROUP_IDS_JSON_PATH, true);
    }

    protected void writeToS3(final String content, final String prefix, final String instanceId) {
        InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length());

        String fileName = instanceId + SECURITY_GROUPS + new DateTime(UTC) + JSON;
        s3Writer.putObjectToS3(bucketName, fileName, prefix, metadata, stream);
    }

    protected List<String> listS3Objects(final String bucketName, final String prefix) {
        return s3Writer.listS3Objects(bucketName, prefix);
    }

}
