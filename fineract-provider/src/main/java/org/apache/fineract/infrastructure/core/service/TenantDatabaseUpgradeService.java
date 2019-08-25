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

import org.apache.fineract.infrastructure.core.boot.FineractSettings;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenantConnection;
import org.apache.fineract.infrastructure.security.service.TenantDetailsService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;

/**
 * A service that picks up on tenants that are configured to auto-update their
 * specific schema on application startup.
 */
@Service
public class TenantDatabaseUpgradeService {

    private final TenantDetailsService tenantDetailsService;
    protected final DataSource tenantDataSource;

    @Autowired
    private FineractSettings settings;
    
    @Autowired
    public TenantDatabaseUpgradeService(final TenantDetailsService detailsService, final DataSource dataSource) {
        this.tenantDetailsService = detailsService;
        this.tenantDataSource = dataSource;
    }

    @PostConstruct
    public void upgradeAllTenants() {
        upgradeTenantDB();
        final List<FineractPlatformTenant> tenants = this.tenantDetailsService.findAllTenants();
        for (final FineractPlatformTenant tenant : tenants) {
            final FineractPlatformTenantConnection connection = tenant.getConnection();
            if (connection.isAutoUpdateEnabled()) {
                final String url = String.format("%s:%s://%s:%s/%s", settings.getJdbc().getProtocol(), settings.getJdbc().getSubProtocol(), connection.getSchemaServer(), connection.getSchemaServerPort(), connection.getSchemaName()); // TODO: use just one property that contains the whole JDBC url; way easier than splitting this up
                final FluentConfiguration configuration = new FluentConfiguration()
                    .dataSource(url, connection.getSchemaUsername(), connection.getSchemaPassword())
                    .locations("sql/migrations/core_db")
                    .outOfOrder(true);
                final Flyway flyway = configuration.load();
                flyway.migrate();
            }
        }
    }

    /**
     * Initializes, and if required upgrades (using Flyway) the Tenant DB
     * itself.
     */
    private void upgradeTenantDB() {
        final FluentConfiguration configuration = new FluentConfiguration()
            .dataSource(tenantDataSource)
            .locations("sql/migrations/list_db")
            .outOfOrder(true);
        final Flyway flyway = configuration.load();
        flyway.migrate();

        // TODO: @aleks why would you need this?!?
        // tenantDataSourcePortFixService.fixUpTenantsSchemaServerPort();
    }
}