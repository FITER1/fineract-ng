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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;

@Slf4j
@ApplicationPath("/api/v1")
@Configuration
public class JerseyConfiguration extends ResourceConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        final String[] providerBeans = this.applicationContext.getBeanNamesForAnnotation(Provider.class);
        final String[] resourceBeans = this.applicationContext.getBeanNamesForAnnotation(Path.class);

        // register(RequestContextFilter.class);
        // register(JacksonFeature.class);
        // register(FormDataParamInjectionFeature.class);
        register(MultiPartFeature.class);
        register(SseFeature.class);

        Arrays.stream(providerBeans).forEach(providerBean -> {
            final Object provider = this.applicationContext.getBean(providerBean);

            // log.warn("Register provider: {} - {}", resourceBean, resource.getClass().getName());

            if(provider.getClass().getPackage().toString().startsWith("org.apache.fineract")) {
                register(provider);
            }
        });

        Arrays.stream(resourceBeans).forEach(resourceBean -> {
            final Object resource = this.applicationContext.getBean(resourceBean);

            // log.warn("Register resource: {} - {}", resourceBean, resource.getClass().getName());

            register(resource);
        });
    }
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        // objectMapper.registerModule(new JodaModule()); // TODO: @aleks fix this
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper;
    }
}
