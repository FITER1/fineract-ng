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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.EnableAspectJAutoProxy;



/**
 * Fineract main() application which launches Fineract in an embedded Tomcat HTTP
 * (using Spring Boot).
 *
 * The DataSource used is a to a "normal" external database (not use MariaDB4j).
 *
 * You can easily launch this via Debug as Java Application in your IDE -
 * without needing command line Gradle stuff, no need to build and deploy a WAR,
 * remote attachment etc.
 *
 * It's the old/classic Mifos (non-X) Workspace 2.0 reborn for Fineract! ;-)
 *
 */
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
/*
@ComponentScans({
	@ComponentScan("org.apache.fineract.accounting"),
	@ComponentScan("org.apache.fineract.commands.provider"),
	@ComponentScan("org.apache.fineract.commands.handler"),
	@ComponentScan("org.apache.fineract.commands.service"),
	@ComponentScan("org.apache.fineract.commands"),
	@ComponentScan("org.apache.fineract.audit"),
	@ComponentScan("org.apache.fineract.infrastructure.core.boot"),
	@ComponentScan("org.apache.fineract.infrastructure.creditbureau"),
	@ComponentScan("org.apache.fineract.infrastructure"),
	@ComponentScan("org.apache.fineract.scheduledjobs"),
	@ComponentScan("org.apache.fineract.organisation"),
	@ComponentScan("org.apache.fineract.interoperation"),
	@ComponentScan("org.apache.fineract.portfolio.loanaccount"),
	@ComponentScan("org.apache.fineract.portfolio.savingsaccount"),
	@ComponentScan("org.apache.fineract.portfolio"),
	@ComponentScan("org.apache.fineract.useradministration"),
	@ComponentScan("org.apache.fineract.mix"),
	@ComponentScan("org.apache.fineract.notification"),
	@ComponentScan("org.apache.fineract.template"),
	@ComponentScan("org.apache.fineract.template.service"),
	@ComponentScan("org.apache.fineract.useradministration"),
	@ComponentScan("org.apache.fineract.batch,"),
	@ComponentScan("org.apache.fineract.adhocquery"),
	@ComponentScan("org.apache.fineract.infrastructure.campaigns"),
	@ComponentScan("org.apache.fineract.spm")
	@ComponentScan(excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, pattern = "org.springframework.stereotype.Controller")
	})
})
*/

// TODO: @aleks find out if we really need to exclude these
/*
exclude = {
	DataSourceAutoConfiguration.class,
	HibernateJpaAutoConfiguration.class,
	DataSourceTransactionManagerAutoConfiguration.class,
	FlywayAutoConfiguration.class
}*/
@EnableAutoConfiguration(exclude = {
	HibernateJpaAutoConfiguration.class,
	MetricsAutoConfiguration.class,
})
@ComponentScans({
	@ComponentScan("org.apache.fineract.accounting"),
	@ComponentScan("org.apache.fineract.commands"),
	@ComponentScan("org.apache.fineract.audit"),
	@ComponentScan("org.apache.fineract.infrastructure"),
	@ComponentScan("org.apache.fineract.scheduledjobs"),
	@ComponentScan("org.apache.fineract.organisation"),
	@ComponentScan("org.apache.fineract.interoperation"),
	@ComponentScan("org.apache.fineract.portfolio"),
	@ComponentScan("org.apache.fineract.useradministration"),
	@ComponentScan("org.apache.fineract.mix"),
	@ComponentScan("org.apache.fineract.notification"),
	@ComponentScan("org.apache.fineract.template"),
	@ComponentScan("org.apache.fineract.useradministration"),
	@ComponentScan("org.apache.fineract.batch,"),
	@ComponentScan("org.apache.fineract.adhocquery"),
	@ComponentScan("org.apache.fineract.spm")
})
public class ServerApplication {
	public static void main(String[] args) throws Exception {
		SpringApplication.run(ServerApplication.class, args);
	}
}
