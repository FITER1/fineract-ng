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

package org.apache.fineract.portfolio.savings.data;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.fineract.portfolio.savings.SavingsApiConstants.applicationFlowIdParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.applicationStatusParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.savingsAccountIdParamName;

@Component
public class SavingsAccountCardDataValidator {

    @Autowired
    private FromJsonHelper fromApiJsonHelper;

    public void validate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {
        }.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                SavingsAccountConstant.SAVINGS_ACCOUNT_CARD_SUPPORTED_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SavingsApiConstants.SAVINGS_ACCOUNT_CARD_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final Long savingsAccountId = this.fromApiJsonHelper.extractLongNamed(savingsAccountIdParamName, element);
        baseDataValidator.reset().parameter(savingsAccountIdParamName).value(savingsAccountId).notNull().longGreaterThanZero();

        final Long applicationFlowId = this.fromApiJsonHelper.extractLongNamed(applicationFlowIdParamName, element);
        baseDataValidator.reset().parameter(applicationFlowIdParamName).value(applicationFlowId).notNull().longGreaterThanZero();

        final String applicationStatus = this.fromApiJsonHelper.extractStringNamed(applicationStatusParamName, element);
        baseDataValidator.reset().parameter(applicationStatusParamName).value(applicationStatus).notBlank();

        this.throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
