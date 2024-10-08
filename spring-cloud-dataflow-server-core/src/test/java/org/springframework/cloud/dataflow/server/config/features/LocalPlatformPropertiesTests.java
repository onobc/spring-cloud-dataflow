/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.config.features;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = LocalPlatformPropertiesTests.TestConfig.class)
@ActiveProfiles("local-platform-properties")
class LocalPlatformPropertiesTests {

	@Autowired
	private LocalPlatformProperties localPlatformProperties;

	@Test
	void deserializationTest() {
		Map<String, LocalDeployerProperties> localAccounts = this.localPlatformProperties.getAccounts();
		assertThat(localAccounts)
				.hasSize(2)
				.containsKeys("localDev", "localDevDebug");
		assertThat(localAccounts.get("localDev").getShutdownTimeout()).isEqualTo(60);
		assertThat(localAccounts.get("localDevDebug").getJavaOpts()).isEqualTo("-Xdebug");
	}

	@Configuration
	@EnableConfigurationProperties(LocalPlatformProperties.class)
	static class TestConfig {
	}
}
