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
package org.apache.fineract.interoperation.data;

import com.google.gson.JsonObject;
import lombok.*;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.interoperation.domain.InteropIdentifierType;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;

import static org.apache.fineract.interoperation.util.InteropUtil.PARAM_ACCOUNT_ID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InteropIdentifierRequestData {
    static final String[] PARAMS = {
        PARAM_ACCOUNT_ID
    };

    @NotEmpty
    private InteropIdentifierType idType;
    @NotEmpty
    private String idValue;
    private String subIdOrType;
    @NotEmpty
    private String accountId;

    public static InteropIdentifierRequestData validateAndParse(final DataValidatorBuilder dataValidator, @NotNull InteropIdentifierType idType,
                                                                @NotNull String idValue, String subIdOrType, JsonObject element,
                                                                FromJsonHelper jsonHelper) {
        if (element == null) {
            return null;
        }

        jsonHelper.checkForUnsupportedParameters(element, Arrays.asList(PARAMS));

        String accountId = jsonHelper.extractStringNamed(PARAM_ACCOUNT_ID, element);
        DataValidatorBuilder dataValidatorCopy = dataValidator.reset().parameter(PARAM_ACCOUNT_ID).value(accountId).notBlank();

        dataValidator.merge(dataValidatorCopy);
        return dataValidator.hasError() ? null : new InteropIdentifierRequestData(idType, idValue, subIdOrType, accountId);
    }
}
