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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.interoperation.domain.InteropInitiatorType;
import org.apache.fineract.interoperation.domain.InteropTransactionRole;
import org.apache.fineract.interoperation.domain.InteropTransactionScenario;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;

import static org.apache.fineract.interoperation.util.InteropUtil.*;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InteropTransactionTypeData {

    public static final String[] PARAMS = {
        PARAM_SCENARIO,
        PARAM_SUB_SCENARIO,
        PARAM_INITIATOR,
        PARAM_INITIATOR_TYPE,
        PARAM_REFUND_INFO,
        PARAM_BALANCE_OF_PAYMENTS
    };

    @NotNull
    private InteropTransactionScenario scenario;
    private String subScenario;
    @NotNull
    private InteropTransactionRole initiator;
    @NotNull
    private InteropInitiatorType initiatorType;
    @Valid
    private InteropRefundData refundInfo;
    private String balanceOfPayments; // 3 digits number, see https://www.imf.org/external/np/sta/bopcode/

    public static InteropTransactionTypeData validateAndParse(DataValidatorBuilder dataValidator, JsonObject element, FromJsonHelper jsonHelper) {
        if (element == null)
            return null;

        jsonHelper.checkForUnsupportedParameters(element, Arrays.asList(PARAMS));

        String scenarioString = jsonHelper.extractStringNamed(PARAM_SCENARIO, element);
        DataValidatorBuilder dataValidatorCopy = dataValidator.reset().parameter(PARAM_SCENARIO).value(scenarioString).notBlank();
        InteropTransactionScenario scenario = InteropTransactionScenario.valueOf(scenarioString);

        String subScenario = jsonHelper.extractStringNamed(PARAM_SUB_SCENARIO, element);

        String initiatorString = jsonHelper.extractStringNamed(PARAM_INITIATOR, element);
        dataValidatorCopy = dataValidatorCopy.reset().parameter(PARAM_INITIATOR).value(initiatorString).notBlank();
        InteropTransactionRole initiator = InteropTransactionRole.valueOf(initiatorString);

        String initiatorTypeString = jsonHelper.extractStringNamed(PARAM_INITIATOR_TYPE, element);
        dataValidatorCopy = dataValidatorCopy.reset().parameter(PARAM_INITIATOR_TYPE).value(initiatorTypeString).notBlank();
        InteropInitiatorType initiatorType = InteropInitiatorType.valueOf(initiatorTypeString);

        dataValidator.merge(dataValidatorCopy);
        return dataValidator.hasError() ? null : InteropTransactionTypeData.builder()
            .scenario(scenario)
            .subScenario(subScenario)
            .initiator(initiator)
            .initiatorType(initiatorType)
            .build();
    }
}
