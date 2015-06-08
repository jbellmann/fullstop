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
package org.zalando.stups.fullstop.swagger.api;

import com.wordnik.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.zalando.stups.fullstop.s3.S3Writer;
import org.zalando.stups.fullstop.swagger.model.Acknowledged;
import org.zalando.stups.fullstop.swagger.model.LogObj;
import org.zalando.stups.fullstop.swagger.model.Violation;
import org.zalando.stups.fullstop.violation.entity.ViolationEntity;
import org.zalando.stups.fullstop.violation.service.ViolationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api", produces = {APPLICATION_JSON_VALUE})
@Api(value = "/api", description = "the api API")
@PreAuthorize("#oauth2.hasScope('uid')")
public class FullstopApi {

    private static final Logger logger = LoggerFactory.getLogger(FullstopApi.class);

    @Autowired
    private S3Writer s3Writer;
    @Autowired
    private ViolationService violationService;

    @ApiOperation(value = "account-ids", notes = "Get all account ids", response = String.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List of all account Ids")})
    @RequestMapping(value = "/account-ids", method = RequestMethod.GET)
    public ResponseEntity<List<String>> accountId() throws NotFoundException {
        List<String> accountIds = violationService.findAccountId();
        return new ResponseEntity<>(accountIds, OK);
    }

    @ApiOperation(
        value = "Violations for one account", notes = "Get all violations for one account", response = Violation.class
    )
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List of all violations for one account")})
    @RequestMapping(value = "/account-violations/{account-id}", method = RequestMethod.GET)
    public ResponseEntity<List<Violation>> accountViolations(
            @ApiParam(value = "", required = true)
            @PathVariable("account-id")
            final String accountId) throws NotFoundException {
        List<ViolationEntity> backendViolationsByAccount = violationService.findByAccountId(accountId);
        List<Violation> frontendViolationsByAccount = mapBackendToFrontendViolations(backendViolationsByAccount);
        return new ResponseEntity<>(frontendViolationsByAccount, OK);
    }

    @ApiOperation(
        value = "Post instance instance log in S3", notes = "Add instance log for instance in S3",
        response = Void.class
    )
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Logs saved successfully")})
    @RequestMapping(value = "/instance-logs", method = RequestMethod.POST)
    public ResponseEntity<Void> instanceLogs(
            @ApiParam(value = "instance-log", required = true)
            @RequestBody final LogObj instanceLog) throws NotFoundException {
        saveLog(instanceLog);
        return new ResponseEntity<>(CREATED);
    }

    @ApiOperation(value = "violations", notes = "Get all violations", response = Violation.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List of all violations")})
    @RequestMapping(value = "/violations", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public Page<ViolationEntity> violations(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = ASC) final Pageable pageable,
            @AuthenticationPrincipal(errorOnInvalidType = true) final String uid) throws NotFoundException {
        logger.info("this is my username: {}", uid);

        Page<ViolationEntity> backendViolations = violationService.findAll(pageable);
// List<Violation> frontendViolations = mapBackendToFrontendViolations(backendViolations);

        return backendViolations;
    }

    @ApiOperation(
        value = "Comment and acknowledged violation", notes = "Comment and acknowledged violation",
        response = Void.class
    )
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Violation updated successfully")})
    @RequestMapping(value = "/violations/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> acknowledgedViolations(
            @ApiParam(value = "", required = true)
            @PathVariable("id")
            final Integer id,
            @ApiParam(value = "", required = true) final Acknowledged acknowledged) throws NotFoundException {
        ViolationEntity violation = violationService.findOne(id);
        violation.setChecked(acknowledged.getChecked());
        violation.setComment(acknowledged.getMessage());
        violationService.save(violation);
        return new ResponseEntity<>(OK);
    }

    private List<Violation> mapBackendToFrontendViolations(final List<ViolationEntity> backendViolations) {
        List<Violation> frontendViolations = new ArrayList<>();
        for (ViolationEntity entity : backendViolations) {
            Violation violation = new Violation();

// violation.setId(backendViolation.getId());
// violation.setVersion(backendViolation.getVersion());
//
// backendViolation.getCreated();
// backendViolation.getCreatedBy();
// backendViolation.getLastModified();
// backendViolation.getLastModifiedBy();

            violation.setAccountId(entity.getAccountId());
            violation.setEventId(entity.getEventId());
            violation.setMessage(entity.getMessage());
            violation.setRegion(entity.getRegion());
            violation.setChecked(entity.getChecked());
            violation.setComment(entity.getComment());
            violation.setViolationObject(entity.getViolationObject());
            frontendViolations.add(violation);
        }

        return frontendViolations;
    }

    private void saveLog(final LogObj instanceLog) {
        try {
            s3Writer.writeToS3(instanceLog.getAccountId(), instanceLog.getRegion(), instanceLog.getInstanceBootTime(),
                instanceLog.getLogData(), instanceLog.getLogType(), instanceLog.getInstanceId());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
