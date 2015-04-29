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

package org.zalando.stups.fullstop.violation;

import com.google.common.base.MoreObjects;

/**
 * @author  mrandi
 */
public class Violation {

    private String accountId;
    private String region;
    private String message;
    private Object violationObject;

    public Violation(final Object violationObject) {
        this.violationObject = violationObject;
    }

    public Violation(final String accountId, final String region) {
        this.accountId = accountId;
        this.region = region;
    }

    public Violation(final String accountId, final String region, final String message) {
        this.accountId = accountId;
        this.region = region;
        this.message = message;
    }

    public Violation(final String accountId, final String region, final String message, final Object violationObject) {
        this.accountId = accountId;
        this.region = region;
        this.message = message;
        this.violationObject = violationObject;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(final String accountId) {
        this.accountId = accountId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public Object getViolationObject() {
        return violationObject;
    }

    public void setViolationObject(final Object violationObject) {
        this.violationObject = violationObject;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("accountId", accountId).add("region", region)
                          .add("message", message).add("violationObject", violationObject).toString();
    }
}
