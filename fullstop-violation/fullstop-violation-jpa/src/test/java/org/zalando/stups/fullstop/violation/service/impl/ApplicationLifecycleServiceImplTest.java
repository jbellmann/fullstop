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
package org.zalando.stups.fullstop.violation.service.impl;

import com.opentable.db.postgres.embedded.EmbeddedPostgreSQL;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.stups.fullstop.violation.entity.ApplicationEntity;
import org.zalando.stups.fullstop.violation.entity.LifecycleEntity;
import org.zalando.stups.fullstop.violation.entity.VersionEntity;
import org.zalando.stups.fullstop.violation.repository.ApplicationRepository;
import org.zalando.stups.fullstop.violation.repository.LifecycleRepository;
import org.zalando.stups.fullstop.violation.repository.VersionRepository;
import org.zalando.stups.fullstop.violation.service.ApplicationLifecycleService;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by gkneitschel.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ApplicationLifecycleServiceImplTest.TestConfig.class)
@Transactional
public class ApplicationLifecycleServiceImplTest {

    private VersionEntity snapshot;

    private VersionEntity snapshot2;

    private VersionEntity release;

    private ApplicationEntity fullstop;

    private ApplicationEntity fullstop2;

    private ApplicationEntity yourturn;

    private LifecycleEntity runInstance;

    private LifecycleEntity terminteInstance;

    private LifecycleEntity stopInstance;

    @Autowired
    private ApplicationLifecycleService applicationLifecycleService;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private LifecycleRepository lifecycleRepository;

    @PersistenceContext
    private EntityManager em;

    @Before
    public void setUp() throws Exception {
        snapshot = new VersionEntity("0.5-Snaphot");
        snapshot2 = new VersionEntity("0.5-Snaphot");
        release = new VersionEntity("1.0");

        fullstop = new ApplicationEntity("Fullstop");
        fullstop2 = new ApplicationEntity("Fullstop");
        yourturn = new ApplicationEntity("Yourturn");

        runInstance = new LifecycleEntity();
        runInstance.setRegion("eu-west-1");
        runInstance.setEventDate(new DateTime());
        runInstance.setInstanceId("i-1234");
        runInstance.setEventType("RunInstances");

        terminteInstance = new LifecycleEntity();
        terminteInstance.setEventDate(new DateTime());
        terminteInstance.setEventType("TerminateInstances");
        terminteInstance.setRegion("eu-central-1");
        terminteInstance.setInstanceId("i-7890");

        stopInstance = new LifecycleEntity();
        stopInstance.setEventDate(new DateTime());
        stopInstance.setEventType("StopInstances");
        stopInstance.setInstanceId("i-7890");
        stopInstance.setRegion("eu-north-8");

    }

    @Test
    public void testSave() throws Exception {

        fullstop = applicationRepository.save(fullstop);
        snapshot.getApplicationEntities().add(fullstop);
        snapshot = versionRepository.save(snapshot);

        applicationLifecycleService.saveLifecycle(fullstop2, snapshot2, stopInstance);
        assertThat(versionRepository.findAll()).isNotEmpty().hasSize(1);
    }

    @Test
    public void testSaveAttachVersionToApplication() throws Exception {
        release = versionRepository.save(release);

        fullstop.getVersionEntities().add(release);
        fullstop = applicationRepository.save(fullstop);

        em.flush();
        em.clear();

        VersionEntity versionEntity = versionRepository.findOne(release.getId());
        assertThat(versionEntity.getApplicationEntities()).isNotEmpty().hasSize(1);
    }

    @Test
    public void testSaveMultipleLifecycle() throws Exception {
        applicationLifecycleService.saveLifecycle(fullstop, snapshot, runInstance);
        applicationLifecycleService.saveLifecycle(fullstop2, snapshot2, stopInstance);
        assertThat(versionRepository.findAll()).isNotEmpty().hasSize(1);
        assertThat(applicationRepository.findAll()).isNotEmpty().hasSize(1);
        assertThat(lifecycleRepository.findAll()).isNotEmpty().hasSize(2);
    }

    @Test(expected = RuntimeException.class)
    public void testSaveWithLifecyleNull() {
        applicationLifecycleService.saveLifecycle(fullstop, snapshot, null);
    }

    @Test(expected = RuntimeException.class)
    public void testSaveWithVersionNull() {
        applicationLifecycleService.saveLifecycle(fullstop, null, runInstance);
    }

    @Test(expected = RuntimeException.class)
    public void testSaveWithApplicationNull() {
        applicationLifecycleService.saveLifecycle(null, snapshot, runInstance);
    }

    @Test(expected = RuntimeException.class)
    public void testSaveAllNull() {
        applicationLifecycleService.saveLifecycle(null, null, null);
    }

    @Test
    public void testSaveInstanceLogLifecycle() throws Exception {
        String userdataPath = "URL/to/File";
        DateTime instanceBootTime = DateTime.now();
        String instanceId = "i-1234";
        String region = "eu-west-1";
        String accountId = "123456";
        String userdata = encodeToBase64(
                "#taupage-ami-config\n"
                        + "application_id: Fullstop\n"
                        + "application_version: '0.5-Snaphot'\n"
                        + "environment:\n"
                        + "  xx: xx");
        LifecycleEntity lifecycleEntity = applicationLifecycleService.saveInstanceLogLifecycle(
                instanceId,
                instanceBootTime,
                userdataPath, region, userdata, accountId);
        assertThat(lifecycleEntity.getId()).isNotNull();

    }
    @Test
    public void testSaveNullInstanceLogLifecycle() throws Exception {
        String userdataPath = "URL/to/File";
        DateTime instanceBootTime = DateTime.now();
        String instanceId = "i-1234";
        String region = "eu-west-1";
        String accountId = "123456";
        String userdata = null;
        LifecycleEntity lifecycleEntity = applicationLifecycleService.saveInstanceLogLifecycle(
                instanceId,
                instanceBootTime,
                userdataPath, region, userdata, accountId);
        assertThat(lifecycleEntity).isNull();

    }

    private String encodeToBase64(String toEncode) {
        return Base64.getEncoder().encodeToString(
                (toEncode).getBytes());
    }

    @Test
    public void testSaveInstanceLogLifecycle1() throws Exception {
        String userdataPath = "URL/to/File";
        DateTime instanceBootTime = DateTime.now();
        String instanceId = "i-1234";
        String region = "eu-west-1";
        String accountId = "123456";
        String userdata = encodeToBase64(
                "#taupage-ami-config\n"
                        + "application_id: Fullstop\n"
                        + "application_version: '0.5-Snaphot'\n"
                        + "environment:\n"
                        + "  xx: xx");
        LifecycleEntity lifecycleEntity = applicationLifecycleService.saveInstanceLogLifecycle(
                instanceId,
                instanceBootTime,
                userdataPath, region, userdata, accountId);

        applicationLifecycleService.saveLifecycle(fullstop, snapshot, runInstance);
        assertThat(lifecycleRepository.findAll()).hasSize(1);

    }

    @Test
    public void testBase64withNewlines() throws Exception {
        String userdataPath = "URL/to/File";
        DateTime instanceBootTime = DateTime.now();
        String instanceId = "i-1234";
        String region = "eu-west-1";
        String accountId = "123456";
        String userdata = "YXBwbGljYXRpb25faWQ6IENyYXp5IEFwcGxpY2F0aW9uCmFwcGxpY2F0aW9uX3ZlcnNpb246ICc\n"
                +"xLVNOQVBTSE9UJwplbnZpcm9ubWVudDoge0VOVlZBUjogY29udGVudH0KaW5zdGFuY2VfbG9nc1\n"
                +"91cmw6IGh0dHBzOi8vc2VydmVyLnRsZC9wYXRoL3RvL2VuZHBvaW50Cm5vdGlmeV9jZm46IHtyZ\n"
                +"XNvdXJjZTogQXBwU2VydmVyLCBzdGFjazogY3JhenktYXBwbGljYXRpb24tMS1zbmFwc2hvdH0K\n"
                +"cG9ydHM6IHsxMjM0OiAxMjM0fQpyb290OiBmYWxzZQpydW50aW1lOiBEb2NrZXIKc291cmNlOiB\n"
                +"zb21lLnJlZ2lzdHJ5LnRsZC9maW5kL2NyYXp5LWFwcGxpY2F0aW9uOjAuMS4wLVNOQVBTSE9UCn\n"
                +"Rva2VuX3NlcnZpY2VfdXJsOiBodHRwczovL3Rva2VuLnNlcnZpY2UvYWNjZXNzX3Rva2VuCg==";
        LifecycleEntity lifecycleEntity = applicationLifecycleService.saveInstanceLogLifecycle(
                instanceId,
                instanceBootTime,
                userdataPath, region, userdata, accountId);

        assertThat(lifecycleEntity).isNotNull();


    }

    @Configuration
    @EnableAutoConfiguration
    @EnableJpaRepositories("org.zalando.stups.fullstop.violation.repository")
    @EntityScan("org.zalando.stups.fullstop.violation")
    @EnableJpaAuditing
    static class TestConfig {

        @Bean DataSource dataSource() throws IOException {
            return embeddedPostgres().getPostgresDatabase();
        }

        @Bean EmbeddedPostgreSQL embeddedPostgres() throws IOException {
            return EmbeddedPostgreSQL.start();
        }

        @Bean AuditorAware<String> auditorAware() {
            return () -> "unit-test";
        }

        @Bean ApplicationLifecycleService applicationLifecycleService() {
            return new ApplicationLifecycleServiceImpl();
        }
    }
}