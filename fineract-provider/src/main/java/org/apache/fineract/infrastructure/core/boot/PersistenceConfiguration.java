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
package org.apache.fineract.infrastructure.core.boot;

import org.apache.fineract.infrastructure.core.service.FineractRoutingDatasource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import java.util.Properties;

@Configuration
@EnableJpaRepositories({
    "org.apache.fineract.commands.domain",
    "org.apache.fineract.infrastructure.*.domain",
    "org.apache.fineract.accounting.*.domain",
    "org.apache.fineract.useradministration.domain",
    "org.apache.fineract.organisation.*.domain",
    "org.apache.fineract.portfolio.*",
    "org.apache.fineract.mix.domain",
    "org.apache.fineract.scheduledjobs.domain",
    "org.apache.fineract.template.domain",
    "org.apache.fineract.infrastructure.campaigns.sms.domain",
    "org.apache.fineract.adhocquery.domain",
    "org.apache.fineract.notification.domain",
    "org.apache.fineract.infrastructure.campaigns.email.domain",
    "org.apache.fineract.interoperation.domain",
    "org.apache.fineract.spm.repository"
})
@EnableTransactionManagement
public class PersistenceConfiguration {
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(FineractRoutingDatasource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("org.apache.fineract");

        JpaVendorAdapter vendorAdapter = new OpenJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        // TODO: @aleks add org.apache.fineract.infrastructure.core.domain.AuditorAwareImpl

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);

        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation(){
        return new PersistenceExceptionTranslationPostProcessor();
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        // NOTE: add more properties if needed

        return properties;
    }
}
