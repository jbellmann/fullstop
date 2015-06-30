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
package org.zalando.stups.differentnamespace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.zalando.stups.fullstop.plugin.region.config.RegionPluginProperties;

import javax.annotation.PostConstruct;

import java.util.List;

/**
 * @author jbellmann
 */
@SpringBootApplication
@EnableConfigurationProperties({ RegionPluginProperties.class })
public class SimpleBootApplication {

    @Autowired
    private RegionPluginProperties regionPluginProperties;

    public static void main(final String[] args) {
        SpringApplication.run(SimpleBootApplication.class, args);
    }

    @PostConstruct
    public void init() {
        List<String> whitelistedRegions = regionPluginProperties.getWhitelistedRegions();

        System.out.println(whitelistedRegions.toString());
    }
}
