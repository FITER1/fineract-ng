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

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public BeanConfig beanConfig(Environment environment) {
        BeanConfig config = new BeanConfig();
        config.setTitle("Apache Fineract API Documentation");
        config.setVersion("1.0.0");
        config.setDescription("Apache Fineract is a secure, multi-tenant microfinance platform.&lt;br/&gt;" +
            "The goal of the Apache Fineract API is to empower developers to build apps on top of the Apache Fineract Platform. The reference app [  https://demo.openmf.org  ] (username: mifos, password: password) works on the same demo tenant as the interactive links in this documentation.&lt;br/&gt;" +
            "The API is organized around REST [ https://en.wikipedia.org/wiki/Representational_state_transfer ]&lt;br/&gt;" +
            "Find out more about Apache Fineract on [ https://demo.openmf.org/api-docs/apiLive.htm#top ]&lt;br/&gt;" +
            "You can Try The API From Your Browser itself at [ https://demo.openmf.org/api-docs/apiLive.htm#interact ]&lt;br/&gt;" +
            "The Generic Options are available at [ https://demo.openmf.org/api-docs/apiLive.htm#genopts ]&lt;br/&gt;" +
            "Find out more about Updating Dates and Numbers at [ https://demo.openmf.org/api-docs/apiLive.htm#dates_and_numbers ]&lt;br/&gt;" +
            "For the Authentication and the Basic of HTTP and HTTPS refer [ https://demo.openmf.org/api-docs/apiLive.htm#authentication_overview ]&lt;br/&gt;" +
            "Check about ERROR codes at [ https://demo.openmf.org/api-docs/apiLive.htm#errors ]&lt;br/&gt;" +
            "Please refer to the old documentation for any documentation queries [ https://demo.openmf.org/api-docs/apiLive.htm ]&lt;br/&gt;" +
            "______________________________________________________________________________________________________________________________");
        config.setLicense("Apache 2.0");
        config.setSchemes(new String[]{"https"});
        config.setHost("localhost:" + environment.getProperty("local.server.port"));
        config.setBasePath("api/v1");
        config.setTermsOfServiceUrl("https://demo.openmf.org/api-docs/apiLive.htm");
        config.setContact("https://gitter.im/openMF/mifos");
        config.setResourcePackage("org.apache.fineract");
        config.setScan(true);

        return config;
    }

    @Bean
    public ApiListingResource apiListingResource() {
        return new ApiListingResource();
    }

    @Bean
    public SwaggerSerializers swaggerSerializers() {
        return new SwaggerSerializers();
    }
}
