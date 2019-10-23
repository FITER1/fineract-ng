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

import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Profile("basicauth")
@EnableWebSecurity
@Configuration
public class SecurityBasicConfiguration extends WebSecurityConfigurerAdapter {

    @Bean
    public CorsFilter corsFilter(CorsEndpointProperties corsEndpointProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(corsEndpointProperties.getAllowCredentials());
        corsEndpointProperties.getAllowedOrigins().forEach(config::addAllowedOrigin);
        corsEndpointProperties.getAllowedHeaders().forEach(config::addAllowedHeader);
        corsEndpointProperties.getAllowedMethods().forEach(config::addAllowedMethod);
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors()
            .and()
            .requiresChannel().anyRequest().requiresSecure()
            .and()
            .csrf().disable()
            .headers().cacheControl().disable()
            .and()
            .exceptionHandling()
            .and()
            .anonymous()
            .and()
            .httpBasic()
                .realmName("Fineract Platform API")
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
            .antMatchers("/api/**/authentication").permitAll()
            .antMatchers("/api/**/self/authentication").permitAll()
            .antMatchers("/api/**/self/registration").permitAll()
            .antMatchers("/api/**/self/user").permitAll()
            .antMatchers("/api/**").fullyAuthenticated()
            // .antMatchers("/api/**").hasAuthority("TWOFACTOR_AUTHENTICATED")
        ;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring()
            .antMatchers("/", "/index.html")
            .antMatchers("/static/**")
            .antMatchers("/resources/**")
            .antMatchers("/favicon.ico");
    }
}
