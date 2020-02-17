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
import org.apache.fineract.interoperation.domain.InteropAmountType;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

import static org.apache.fineract.interoperation.util.InteropUtil.*;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InteropQuoteRequestData extends InteropRequestData {
    private static final String[] PARAMS = {
        PARAM_TRANSACTION_CODE,
        PARAM_REQUEST_CODE,
        PARAM_ACCOUNT_ID,
        PARAM_AMOUNT,
        PARAM_TRANSACTION_TYPE,
        PARAM_TRANSACTION_ROLE,
        PARAM_NOTE,
        PARAM_GEO_CODE,
        PARAM_EXPIRATION,
        PARAM_EXTENSION_LIST,
        PARAM_QUOTE_CODE,
        PARAM_AMOUNT_TYPE,
        PARAM_FEES,
        PARAM_LOCALE,
        PARAM_DATE_FORMAT
    };

    @NotNull
    private String quoteCode;
    @NotNull
    private InteropAmountType amountType;
    private MoneyData fees; // only for disclosed Payer fees on the Payee side

    public void normalizeAmounts(@NotNull MonetaryCurrency currency) {
        super.normalizeAmounts(currency);
        if (fees != null)
            fees.normalizeAmount(currency);
    }

    public static InteropQuoteRequestData validateAndParse(final DataValidatorBuilder dataValidator, JsonObject element, FromJsonHelper jsonHelper) {
        if (element == null)
            return null;

        jsonHelper.checkForUnsupportedParameters(element, Arrays.asList(PARAMS));

        InteropRequestData interopRequestData = InteropRequestData.validateAndParse(dataValidator, element, jsonHelper);

        String quoteCode = jsonHelper.extractStringNamed(PARAM_QUOTE_CODE, element);
        DataValidatorBuilder dataValidatorCopy = dataValidator.reset().parameter(PARAM_QUOTE_CODE).value(quoteCode).notBlank();

        String amountTypeString = jsonHelper.extractStringNamed(PARAM_AMOUNT_TYPE, element);
        dataValidatorCopy = dataValidatorCopy.reset().parameter(PARAM_AMOUNT_TYPE).value(amountTypeString).notBlank();
        InteropAmountType amountType = InteropAmountType.valueOf(amountTypeString);

        JsonObject feesElement = jsonHelper.extractJsonObjectNamed(PARAM_FEES, element);
        dataValidator.merge(dataValidatorCopy);
        MoneyData fees = MoneyData.validateAndParse(dataValidator, feesElement, jsonHelper);

        String transactionRoleString = jsonHelper.extractStringNamed(PARAM_TRANSACTION_ROLE, element);
        dataValidatorCopy = dataValidator.reset().parameter(PARAM_TRANSACTION_ROLE).value(transactionRoleString).notNull();

        JsonObject transactionTypeElement = jsonHelper.extractJsonObjectNamed(PARAM_TRANSACTION_TYPE, element);
        dataValidatorCopy = dataValidatorCopy.reset().parameter(PARAM_TRANSACTION_TYPE).value(transactionTypeElement).notNull();

        dataValidator.merge(dataValidatorCopy);

        return dataValidator.hasError() ? null : InteropQuoteRequestData.builder()
            .amountType(amountType)
            .fees(fees)
            .quoteCode(quoteCode)
            .requestCode(interopRequestData.getRequestCode())
            .amount(interopRequestData.getAmount())
            .accountId(interopRequestData.getAccountId())
            .transactionRole(interopRequestData.getTransactionRole())
            .transactionType(interopRequestData.getTransactionType())
            .note(interopRequestData.getNote())
            .geoCode(interopRequestData.getGeoCode())
            .expiration(interopRequestData.getExpiration())
            .extensionList(interopRequestData.getExtensionList())
            .build();
    }
}
