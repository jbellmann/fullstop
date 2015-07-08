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

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.internal.JsonPolicyReader;
import com.amazonaws.regions.Region;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.stups.fullstop.aws.ClientProvider;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author mrandi
 */
@Service
public class PolicyProvider {

    private final Logger log = LoggerFactory.getLogger(PolicyProvider.class);

    private final ClientProvider clientProvider;

    @Autowired
    public PolicyProvider(final ClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public String getPolicy(final String roleName, final Region region, final String accountId) {

        AmazonIdentityManagementClient iamClient = clientProvider
                .getClient(AmazonIdentityManagementClient.class, accountId, region);

        if (iamClient == null) {
            throw new RuntimeException(String.format(
                    "Somehow we could not create an AmazonIdentityManagementClient with accountId: %s and region: %s",
                    accountId,
                    region.toString()));
        }
        else {

            String assumeRolePolicyDocument = null;
            try {
                GetRoleRequest getRoleRequest = new GetRoleRequest();
                getRoleRequest.setRoleName(roleName);

                GetRoleResult role = iamClient.getRole(getRoleRequest);

                if (role != null && role.getRole() != null && role.getRole().getAssumeRolePolicyDocument() != null) {
                    try {
                        assumeRolePolicyDocument = URLDecoder.decode(role.getRole().getAssumeRolePolicyDocument(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        log.warn("Could not decode policy document for role: {}", roleName);
                    }
                } else {
                    return null;
                }


            }
            catch (AmazonClientException e) {
                log.error(e.getMessage());
            }

            return assumeRolePolicyDocument;

        }
    }

    public Policy toPolicy(String assumeRolePolicyDocument) {
        // create only once?
            JsonPolicyReader jsonPolicyReader = new JsonPolicyReader();
            return jsonPolicyReader.createPolicyFromJsonString(
                    assumeRolePolicyDocument);
    }

}
