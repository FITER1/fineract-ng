/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract;

import org.apache.fineract.infrastructure.core.service.TenantDatabaseUpgradeService;
import org.apache.fineract.infrastructure.jobs.service.JobRegisterService;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

@Configuration
@PropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
@ComponentScan("org.apache.fineract")
public class TestWithoutDatabaseConfiguration {

    /**
     * Override TenantDatabaseUpgradeService binding, because the real one has a @PostConstruct
     * upgradeAllTenants() which accesses the database on start-up.
     */
    @Bean
    public TenantDatabaseUpgradeService tenantDatabaseUpgradeService() {
        TenantDatabaseUpgradeService mockTenantDatabaseUpgradeService = mock(TenantDatabaseUpgradeService.class);
        return mockTenantDatabaseUpgradeService;
    }

    /**
     * Override JobRegisterService binding, because the real
     * JobRegisterServiceImpl has a @PostConstruct loadAllJobs() which accesses
     * the database on start-up.
     */
    @Bean
    public JobRegisterService jobRegisterServiceImpl() {
        JobRegisterService mockJobRegisterService = mock(JobRegisterService.class);
        return mockJobRegisterService;
    }

    /**
     * DataSource with Mockito RETURNS_MOCKS black magic.
     */
    @Bean
    public DataSource tenantDataSourceJndi() {
        DataSource mockDataSource = mock(DataSource.class, Mockito.RETURNS_MOCKS);
        return mockDataSource;
    }
}
