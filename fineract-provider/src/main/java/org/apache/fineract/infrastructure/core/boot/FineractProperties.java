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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Builder
@Data
@NoArgsConstructor // NOTE: see here https://github.com/rzwitserloot/lombok/issues/816
@AllArgsConstructor // NOTE: see here https://github.com/rzwitserloot/lombok/issues/816
@Component
@ConfigurationProperties(prefix = "fineract")
public class FineractProperties {

    private FineractSettingsTenantdb tenantdb;

    private FineractSettingsJdbc jdbc;

    private Map<String, String> flywayPlaceholders;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FineractSettingsTenantdb {
        private Boolean enabled;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FineractSettingsJdbc {
        private String driverClassName;
        private String protocol;
        private String subProtocol;
        private String host;
        private Integer port;
    }
}
