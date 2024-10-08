/*
 * Copyright 2021-2023 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */

@SpringBootTest(classes = {JobDependencies.class, PropertyPlaceholderAutoConfiguration.class, BatchProperties.class})
@EnableConfigurationProperties({CommonApplicationProperties.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
class TasksInfoControllerTests {

    private final static String BASE_TASK_NAME = "myTask";

    private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

    private final static String TASK_NAME_FOO = BASE_TASK_NAME + "_FOO";

    private final static String TASK_NAME_FOOBAR = BASE_TASK_NAME + "_FOOBAR";

    private boolean initialized = false;

    private static List<String> SAMPLE_ARGUMENT_LIST;

    private static List<String> SAMPLE_CLEANSED_ARGUMENT_LIST;

    @Autowired
    TaskExecutionDao taskExecutionDao;

    @Autowired
	JobRepository jobRepository;

    @Autowired
    TaskDefinitionRepository taskDefinitionRepository;

    @Autowired
    private TaskBatchDao taskBatchDao;

    private MockMvc mockMvc;

    @Autowired
    WebApplicationContext wac;

    @Autowired
    TaskLauncher taskLauncher;

    @Autowired
    LauncherRepository launcherRepository;

    @Autowired
    TaskPlatform taskPlatform;

    @Autowired
    TaskDeploymentRepository taskDeploymentRepository;

	@BeforeEach
	void setupMockMVC() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		assertThat(this.launcherRepository.findByName("default")).isNull();
        Launcher launcher = new Launcher("default", "local", taskLauncher);
        launcherRepository.save(launcher);
        taskPlatform.setLaunchers(Collections.singletonList(launcher));
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
        if (!initialized) {
            SAMPLE_ARGUMENT_LIST = new LinkedList<String>();
            SAMPLE_ARGUMENT_LIST.add("--password=foo");
            SAMPLE_ARGUMENT_LIST.add("password=bar");
            SAMPLE_ARGUMENT_LIST.add("org.woot.password=baz");
            SAMPLE_ARGUMENT_LIST.add("foo.bar=foo");
            SAMPLE_ARGUMENT_LIST.add("bar.baz = boo");
            SAMPLE_ARGUMENT_LIST.add("foo.credentials.boo=bar");
            SAMPLE_ARGUMENT_LIST.add("spring.datasource.username=dbuser");
            SAMPLE_ARGUMENT_LIST.add("spring.datasource.password=dbpass");

            SAMPLE_CLEANSED_ARGUMENT_LIST = new LinkedList<String>();
            SAMPLE_CLEANSED_ARGUMENT_LIST.add("--password=******");
            SAMPLE_CLEANSED_ARGUMENT_LIST.add("password=******");
            SAMPLE_CLEANSED_ARGUMENT_LIST.add("org.woot.password=******");
            SAMPLE_CLEANSED_ARGUMENT_LIST.add("foo.bar=foo");
            SAMPLE_CLEANSED_ARGUMENT_LIST.add("bar.baz = boo");
            SAMPLE_CLEANSED_ARGUMENT_LIST.add("foo.credentials.boo=******");
            SAMPLE_CLEANSED_ARGUMENT_LIST.add("spring.datasource.username=******");
            SAMPLE_CLEANSED_ARGUMENT_LIST.add("spring.datasource.password=******");

            taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));

            TaskExecution taskExecution1 =
				taskExecutionDao.createTaskExecution(TASK_NAME_ORIG, LocalDateTime.now(), SAMPLE_ARGUMENT_LIST, "foobar");
            assertThat(taskExecution1.getExecutionId()).isGreaterThan(0L);
			taskExecutionDao.createTaskExecution(TASK_NAME_ORIG, LocalDateTime.now(), SAMPLE_ARGUMENT_LIST, "foobar", taskExecution1.getExecutionId());
			taskExecutionDao.createTaskExecution(TASK_NAME_FOO, LocalDateTime.now(), SAMPLE_ARGUMENT_LIST, null);
            TaskExecution taskExecution = taskExecutionDao.createTaskExecution(TASK_NAME_FOOBAR, LocalDateTime.now(), SAMPLE_ARGUMENT_LIST,
                    null);
            JobExecution jobExecution = jobRepository.createJobExecution(TASK_NAME_FOOBAR, new JobParameters());
            taskBatchDao.saveRelationship(taskExecution, jobExecution);
            TaskDeployment taskDeployment = new TaskDeployment();
            taskDeployment.setTaskDefinitionName(TASK_NAME_ORIG);
            taskDeployment.setTaskDeploymentId("foobar");
            taskDeployment.setPlatformName("default");
            taskDeployment.setCreatedOn(Instant.now());
            taskDeploymentRepository.save(taskDeployment);
            initialized = true;
        }
    }

	@Test
	void getAllTaskExecutions() throws Exception {
        mockMvc.perform(get("/tasks/info/executions").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExecutions", is(4)));
        mockMvc.perform(get("/tasks/info/executions?completed=true").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExecutions", is(0)));
    }
}
