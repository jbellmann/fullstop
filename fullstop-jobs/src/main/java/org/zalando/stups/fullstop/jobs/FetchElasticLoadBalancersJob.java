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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zalando.stups.fullstop.aws.ClientProvider;
import org.zalando.stups.fullstop.jobs.config.JobsConfig;
import org.zalando.stups.fullstop.teams.Account;
import org.zalando.stups.fullstop.teams.TeamOperations;
import org.zalando.stups.fullstop.violation.Violation;
import org.zalando.stups.fullstop.violation.ViolationBuilder;
import org.zalando.stups.fullstop.violation.ViolationSink;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.zalando.stups.fullstop.violation.ViolationType.WRONG_ELB_SETTINGS;

/**
 * Created by gkneitschel.
 */
@Component
public class FetchElasticLoadBalancersJob {

    private final Logger log = LoggerFactory.getLogger(FetchElasticLoadBalancersJob.class);

    private ViolationSink violationSink;

    private ClientProvider clientProvider;

    private TeamOperations teamOperations;

    private JobsConfig jobsConfig;

    private List<String> accountIds;

    private Set<Integer> allowedPorts = newHashSet(443, 80);

    @Autowired
    public FetchElasticLoadBalancersJob(ViolationSink violationSink,
            ClientProvider clientProvider, TeamOperations teamOperations, JobsConfig jobsConfig) {
        this.violationSink = violationSink;
        this.clientProvider = clientProvider;
        this.teamOperations = teamOperations;
        this.jobsConfig = jobsConfig;
    }

    @PostConstruct
    public void init() {
        log.info("{} initalized", getClass().getSimpleName());
    }

    @Scheduled(fixedRate = 3_600_000)
    public void check() {
        accountIds = fetchAccountIds();
        log.info("Running job {}", getClass().getSimpleName());
        for (String account : accountIds) {
            for (String region : jobsConfig.getWhitelistedRegions()) {
                DescribeLoadBalancersResult describeLoadBalancersResult = getDescribeLoadBalancersResult(
                        account,
                        region);

                for (LoadBalancerDescription loadBalancerDescription : describeLoadBalancersResult.getLoadBalancerDescriptions()) {
                    List<String> metaData = newArrayList();
                    if (!loadBalancerDescription.getScheme().equals("internet-facing")) {
                        continue;
                    }
                    final String canonicalHostedZoneName = loadBalancerDescription.getCanonicalHostedZoneName();

                    String checkPorts = checkPorts(account, region, loadBalancerDescription);
                    if (checkPorts == null) {
                        metaData.add(checkPorts);
                    }

                    String checkSecurityGroups = checkSecurityGroups(account, region, loadBalancerDescription);
                    if (checkSecurityGroups == null) {
                        metaData.add(checkSecurityGroups);
                    }

                    if (metaData.size() > 0) {
                        writeViolation(account, region, metaData, canonicalHostedZoneName);
                    }

                }

            }

        }

    }

    private String checkSecurityGroups(String account, String region, LoadBalancerDescription loadBalancerDescription) {
        List<String> securityGroups = loadBalancerDescription.getSecurityGroups();
        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest();
        describeSecurityGroupsRequest.setGroupIds(securityGroups);
        AmazonEC2Client amazonEC2Client = clientProvider.getClient(
                AmazonEC2Client.class,
                account,
                Region.getRegion(Regions.fromName(region)));
        DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEC2Client.describeSecurityGroups(
                describeSecurityGroupsRequest);
        for (SecurityGroup securityGroup : describeSecurityGroupsResult.getSecurityGroups()) {
            for (IpPermission ipPermission : securityGroup.getIpPermissions()) {
                if (!ipPermission.getFromPort().equals(ipPermission.getToPort())
                        && !allowedPorts.contains(ipPermission.getFromPort())
                        && !allowedPorts.contains(ipPermission.getToPort())) {
                    return "Illegal securityGroup configuration! Port ranges are not allowed and port must be either 80 or 443";
                }
            }

        }
        return null;
    }

    private String checkPorts(String account, String region, LoadBalancerDescription loadBalancerDescription) {

        for (ListenerDescription listenerDescription : loadBalancerDescription.getListenerDescriptions()) {
            final Integer loadBalancerPort = listenerDescription.getListener().getLoadBalancerPort();

            if (!allowedPorts.contains(loadBalancerPort)) {
                return "Illegal port configuration! Only ports 80 and 443 are allowed";
            }
        }
        return null;
    }

    private void writeViolation(String account, String region, Object metaInfo, String canonicalHostedZoneName) {
        ViolationBuilder violationBuilder = new ViolationBuilder();
        Violation violation = violationBuilder.withAccountId(account)
                                              .withRegion(region)
                                              .withPluginFullyQualifiedClassName(FetchElasticLoadBalancersJob.class)
                                              .withType(WRONG_ELB_SETTINGS)
                                              .withMetaInfo(metaInfo)
                                              .withEventId(canonicalHostedZoneName).build();
        violationSink.put(violation);
    }

    private DescribeLoadBalancersResult getDescribeLoadBalancersResult(String account, String region) {
        AmazonElasticLoadBalancingClient elbClient = clientProvider.getClient(
                AmazonElasticLoadBalancingClient.class,
                account,
                Region.getRegion(
                        Regions.fromName(region)));
        DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest();
        return elbClient.describeLoadBalancers(
                describeLoadBalancersRequest);
    }

    private List<String> fetchAccountIds() {
        List<String> accountIds = newArrayList();
        List<Account> accounts = teamOperations.getAccounts();
        accountIds.addAll(accounts.stream().map(Account::getId).collect(Collectors.toList()));
        return accountIds;

    }
}