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
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

import static org.apache.fineract.interoperation.util.InteropUtil.*;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InteropTransferRequestData extends InteropRequestData {
    static final String[] PARAMS = {
        PARAM_TRANSACTION_CODE,
        PARAM_ACCOUNT_ID,
        PARAM_AMOUNT,
        PARAM_TRANSACTION_ROLE,
        PARAM_TRANSACTION_TYPE,
        PARAM_NOTE,
        PARAM_EXPIRATION,
        PARAM_EXTENSION_LIST,
        PARAM_TRANSFER_CODE,
        PARAM_FSP_FEE,
        PARAM_FSP_COMMISSION,
        PARAM_LOCALE,
        PARAM_DATE_FORMAT
    };

    @NotNull
    private String transferCode;
    // validation: what was specified in quotes step
    private MoneyData fspFee;
    private MoneyData fspCommission;

    public void normalizeAmounts(@NotNull MonetaryCurrency currency) {
        super.normalizeAmounts(currency);
        if (fspFee != null)
            fspFee.normalizeAmount(currency);
    }

    public static InteropTransferRequestData validateAndParse(final DataValidatorBuilder dataValidator, JsonObject element, FromJsonHelper jsonHelper) {
        if (element == null)
            return null;

        jsonHelper.checkForUnsupportedParameters(element, Arrays.asList(PARAMS));

        InteropRequestData interopRequestData = InteropRequestData.validateAndParse(dataValidator, element, jsonHelper);

        String transferCode = jsonHelper.extractStringNamed(PARAM_TRANSFER_CODE, element);
        DataValidatorBuilder dataValidatorCopy = dataValidator.reset().parameter(PARAM_TRANSFER_CODE).value(transferCode).notBlank();

        JsonObject fspFeeElement = jsonHelper.extractJsonObjectNamed(PARAM_FSP_FEE, element);
        dataValidator.merge(dataValidatorCopy);
        MoneyData fspFee = MoneyData.validateAndParse(dataValidator, fspFeeElement, jsonHelper);

        JsonObject fspCommissionElement = jsonHelper.extractJsonObjectNamed(PARAM_FSP_COMMISSION, element);
        dataValidator.merge(dataValidatorCopy);
        MoneyData fspCommission = MoneyData.validateAndParse(dataValidator, fspCommissionElement, jsonHelper);

        String transactionRoleString = jsonHelper.extractStringNamed(PARAM_TRANSACTION_ROLE, element);
        dataValidatorCopy = dataValidator.reset().parameter(PARAM_TRANSACTION_ROLE).value(transactionRoleString).notNull();

        dataValidator.merge(dataValidatorCopy);
        return dataValidator.hasError() ? null : InteropTransferRequestData.builder()
            .transferCode(interopRequestData.getTransactionCode())
            .transferCode(transferCode)
            .accountId(interopRequestData.getAccountId())
            .amount(interopRequestData.getAmount())
            .transactionRole(interopRequestData.getTransactionRole())
            .transactionType(interopRequestData.getTransactionType())
            .note(interopRequestData.getNote())
            .expiration(interopRequestData.getExpiration())
            .extensionList(interopRequestData.getExtensionList())
            .fspFee(fspFee)
            .fspCommission(fspCommission)
            .build();
    }
}
