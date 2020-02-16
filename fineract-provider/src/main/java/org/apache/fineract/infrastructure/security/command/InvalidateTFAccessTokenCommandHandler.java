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
package org.apache.fineract.infrastructure.security.command;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.constants.TwoFactorConstants;
import org.apache.fineract.infrastructure.security.domain.TFAccessToken;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.TwoFactorService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.*;


@Service
@CommandType(entity = "TWOFACTOR_ACCESSTOKEN", action = "INVALIDATE")
@Profile("twofactor")
@RequiredArgsConstructor
public class InvalidateTFAccessTokenCommandHandler implements NewCommandSourceHandler {


    private final TwoFactorService twoFactorService;
    private final PlatformSecurityContext securityContext;
    private final FromJsonHelper fromJsonHelper;

    @Transactional
    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        validateJson(command.json());

        final AppUser user = securityContext.authenticatedUser();

        final TFAccessToken accessToken = twoFactorService.invalidateAccessToken(user, command);

        return CommandProcessingResult.builder()
                .commandId(command.commandId())
                .resourceIdentifier(accessToken.getToken())
                .build();
    }

    private void validateJson(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                new HashSet<>(Collections.singletonList("token")));
        final JsonElement element = this.fromJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(TwoFactorConstants.ACCESSTOKEN_RESOURCE_NAME);

        final String token = this.fromJsonHelper.extractStringNamed("token", element);
        baseDataValidator.reset().parameter("token").value(token).notNull().notBlank();

        if(!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
