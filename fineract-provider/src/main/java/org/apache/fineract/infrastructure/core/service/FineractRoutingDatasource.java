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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.boot.FineractSettings;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenantConnection;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FineractRoutingDatasource extends AbstractRoutingDataSource {

    private Map<Long, DataSource> dataSources = new ConcurrentHashMap<>();

    @Autowired
    private FineractSettings settings;

    @Override
    protected Object determineCurrentLookupKey() {
        final FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();

        if(tenant!=null) {
            return tenant.getTenantIdentifier();
        }

        return null;
    }

    @NotNull
    @Override
    protected DataSource determineTargetDataSource() {
        final FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();

        DataSource dataSource = null;

        if (tenant != null) {
            final FineractPlatformTenantConnection connection = tenant.getConnection();

            if (this.dataSources.containsKey(connection.getConnectionId())) {
                dataSource = this.dataSources.get(connection.getConnectionId());
            } else {
                dataSource = createDataSource(connection);
                this.dataSources.put(connection.getConnectionId(), dataSource);
            }
        }

        return dataSource;
    }

    private DataSource createDataSource(FineractPlatformTenantConnection connection) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("%s:%s://%s:%s/%s", settings.getJdbc().getProtocol(), settings.getJdbc().getSubProtocol(), connection.getSchemaServer(), connection.getSchemaServerPort(), connection.getSchemaName())); // TODO: use just one property that contains the whole JDBC url; way easier than splitting this up
        config.setUsername(connection.getSchemaUsername());
        config.setPassword(connection.getSchemaPassword());
        config.setDataSourceClassName(settings.getJdbc().getDriverClassName()); // TODO: this should be configurable in "connection"
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        // TODO: set more properties; see https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby

        return new HikariDataSource(config);
    }
}
