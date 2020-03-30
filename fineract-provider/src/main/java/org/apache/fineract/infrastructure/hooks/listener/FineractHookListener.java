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
package org.apache.fineract.infrastructure.hooks.listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.hooks.domain.Hook;
import org.apache.fineract.infrastructure.hooks.event.HookEvent;
import org.apache.fineract.infrastructure.hooks.event.HookEventSource;
import org.apache.fineract.infrastructure.hooks.processor.HookProcessor;
import org.apache.fineract.infrastructure.hooks.processor.HookProcessorProvider;
import org.apache.fineract.infrastructure.hooks.service.HookReadPlatformService;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.template.service.TemplateMergeService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.jersey.core.util.Base64;

@Service
public class FineractHookListener implements HookListener {

    private final HookProcessorProvider hookProcessorProvider;
    private final HookReadPlatformService hookReadPlatformService;
    private final TemplateMergeService templateMergeService;
    private final DefaultToApiJsonSerializer<String> apiJsonSerializerService;
    private final ConfigurationReadPlatformService configurationReadPlatformService;
    private final Environment env;

    @Autowired
    public FineractHookListener(final HookProcessorProvider hookProcessorProvider,
                                final HookReadPlatformService hookReadPlatformService,
                                final TemplateMergeService templateMergeService,
                                final DefaultToApiJsonSerializer<String> apiJsonSerializerService,
                                final ConfigurationReadPlatformService configurationReadPlatformService,
                                final Environment env) {
        this.hookReadPlatformService = hookReadPlatformService;
        this.hookProcessorProvider = hookProcessorProvider;
        this.templateMergeService = templateMergeService;
        this.apiJsonSerializerService = apiJsonSerializerService;
        this.configurationReadPlatformService = configurationReadPlatformService;
        this.env = env;
    }

    @Override
    public void onApplicationEvent(final HookEvent event) {
        final String tenantIdentifier = event.getTenantIdentifier();

        final AppUser appUser = event.getAppUser();
        String authToken = event.getAuthToken();

        if (authToken == null) {
            final byte[] base64EncodedAuthenticationKey = Base64.encode(env.getProperty("fineract.ibanera.hook.user") + ":" + env.getProperty("fineract.ibanera.hook.password"));
            authToken = new String(base64EncodedAuthenticationKey);
        }

        final HookEventSource hookEventSource = event.getSource();
        final String entityName = hookEventSource.getEntityName();
        final String actionName = hookEventSource.getActionName();

        final List<Hook> hooks = this.hookReadPlatformService.retrieveHooksByEvent(hookEventSource.getEntityName(), hookEventSource.getActionName());

        for (final Hook hook : hooks) {
            String payload = event.getPayload();
            final HookProcessor processor = this.hookProcessorProvider.getProcessor(hook);
            if (hook.getUgdTemplate() != null) {
                payload = processUgdTemplate(event.getPayload(), hook, authToken, appUser);
            }
            if (appUser != null && hook.getUgdTemplate() == null) {
                final Map<String, String> jsonMap = new HashMap<>();
                JsonObject payloadObject = new JsonParser().parse(payload).getAsJsonObject();
                jsonMap.put("submittedon_userId", appUser.getId().toString());
                jsonMap.put("submittedon_userName", appUser.getUsername());
                final String jsonString = new Gson().toJson(jsonMap);
                JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
                json.add("payload", payloadObject);
                payload = this.apiJsonSerializerService.serialize(json);
            }
            processor.process(hook, appUser, payload, entityName, actionName, tenantIdentifier, authToken);
        }
    }

    private String processUgdTemplate(final String payload,
                                      final Hook hook, String authToken, final AppUser appUser) {
        JsonObject json = null;
        try {
            @SuppressWarnings("unchecked")
            final HashMap<String, Object> map = new ObjectMapper().readValue(payload, HashMap.class);
            final GlobalConfigurationPropertyData baseUrlConfiguration = this.configurationReadPlatformService.retrieveGlobalConfiguration("Base-URL");

            switch (Integer.parseInt(baseUrlConfiguration.getValue().toString())) {
                case 0:
                    map.put("BASE_URI", env.getProperty("fineract.ibanera.baseUrl.localhost"));
                    break;
                case 1:
                    map.put("BASE_URI", env.getProperty("fineract.ibanera.baseUrl.sandbox"));
                    break;
                case 2:
                    map.put("BASE_URI", env.getProperty("fineract.ibanera.baseUrl.production"));
                    break;
                default:
                    map.put("BASE_URI", env.getProperty("fineract.ibanera.baseUrl.localhost"));
            }

            this.templateMergeService.setAuthToken(authToken);
            final String compiledMessage = this.templateMergeService.compile(hook.getUgdTemplate(), map).replace("<p>", "")
                    .replace("</p>", "").replace("&quot;", "\"");
            final Map<String, String> jsonMap = new HashMap<>();
            jsonMap.put("submittedon_userId", appUser.getId().toString());
            jsonMap.put("submittedon_userName", appUser.getUsername());
            JsonObject payloadObject = new JsonParser().parse(payload).getAsJsonObject();
            JsonObject compiledMessageObject = new JsonParser().parse(compiledMessage).getAsJsonObject();
            final String jsonString = new Gson().toJson(jsonMap);
            json = new JsonParser().parse(jsonString).getAsJsonObject();
            json.add("payload", payloadObject);
            json.add("UGDtemplate", compiledMessageObject);
        } catch (IOException e) {}
        return this.apiJsonSerializerService.serialize(json);
    }

}
