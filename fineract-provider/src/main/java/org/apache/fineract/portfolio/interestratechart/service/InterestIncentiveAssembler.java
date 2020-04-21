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
package org.apache.fineract.portfolio.interestratechart.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.interestratechart.domain.InterestIncentives;
import org.apache.fineract.portfolio.interestratechart.domain.InterestIncentivesFields;
import org.apache.fineract.portfolio.interestratechart.domain.InterestRateChartSlab;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.fineract.portfolio.interestratechart.InterestIncentiveApiConstants.*;
import static org.apache.fineract.portfolio.interestratechart.InterestRateChartSlabApiConstants.incentivesParamName;

@Service
@RequiredArgsConstructor
public class InterestIncentiveAssembler {

    private final FromJsonHelper fromApiJsonHelper;

    public Collection<InterestIncentives> assembleIncentivesFrom(final JsonElement element, InterestRateChartSlab interestRateChartSlab,
            final Locale locale) {
        final Collection<InterestIncentives> interestIncentivesSet = new HashSet<>();

        if (element.isJsonObject()) {
            final JsonObject topLevelJsonElement = element.getAsJsonObject();
            if (topLevelJsonElement.has(incentivesParamName) && topLevelJsonElement.get(incentivesParamName).isJsonArray()) {
                final JsonArray array = topLevelJsonElement.get(incentivesParamName).getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    final JsonObject incentiveElement = array.get(i).getAsJsonObject();
                    final InterestIncentives incentives = this.assembleFrom(incentiveElement, interestRateChartSlab, locale);
                    interestIncentivesSet.add(incentives);
                }
            }
        }

        return interestIncentivesSet;
    }

    private InterestIncentives assembleFrom(final JsonElement element, final InterestRateChartSlab interestRateChartSlab,
            final Locale locale) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(INCENTIVE_RESOURCE_NAME);
        InterestIncentivesFields incentivesFields = createInterestIncentiveFields(element, baseDataValidator, locale);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
        InterestIncentives incentives = InterestIncentives.builder()
            .interestIncentivesFields(incentivesFields)
            .interestRateChartSlab(interestRateChartSlab)
            .build();
        if (interestRateChartSlab != null) {
            interestRateChartSlab.addInterestIncentive(incentives);
        }
        return incentives;
    }

    private InterestIncentivesFields createInterestIncentiveFields(final JsonElement element, final DataValidatorBuilder baseDataValidator,
            final Locale locale) {
        Integer entityType = this.fromApiJsonHelper.extractIntegerNamed(entityTypeParamName, element, locale);
        Integer conditionType = this.fromApiJsonHelper.extractIntegerNamed(conditionTypeParamName, element, locale);
        Integer attributeName = this.fromApiJsonHelper.extractIntegerNamed(attributeNameParamName, element, locale);
        String attributeValue = this.fromApiJsonHelper.extractStringNamed(attributeValueParamName, element);
        Integer incentiveType = this.fromApiJsonHelper.extractIntegerNamed(incentiveTypeparamName, element, locale);
        BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalNamed(amountParamName, element, locale);
        InterestIncentivesFields incentivesFields = InterestIncentivesFields.builder()
            .entityType(entityType)
            .attributeName(attributeName)
            .conditionType(conditionType)
            .attributeValue(attributeValue)
            .incentiveType(incentiveType)
            .amount(amount)
            .build();
        incentivesFields.validateIncentiveData(baseDataValidator);
        return incentivesFields;
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }
}
