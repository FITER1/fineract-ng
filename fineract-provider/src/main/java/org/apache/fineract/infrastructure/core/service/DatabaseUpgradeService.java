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
package org.apache.fineract.infrastructure.core.service;

import org.apache.fineract.infrastructure.core.boot.FineractProperties;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * A service that picks up on tenants that are configured to auto-update their
 * specific schema on application startup.
 */
@Service
public class DatabaseUpgradeService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUpgradeService.class);

    private final DataSource dataSource;

    private final FineractProperties settings;
    
    @Autowired
    public DatabaseUpgradeService(final FineractProperties settings, final DataSource dataSource) {
        this.settings = settings;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void upgrade() {
        try {
            final FluentConfiguration configuration = new FluentConfiguration()
                .dataSource(dataSource)
                .locations("sql/migrations/core_db")
                .placeholders(settings.getFlywayPlaceholders())
                .outOfOrder(true);
            final Flyway flyway = configuration.load();
            flyway.migrate();
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
    }
}