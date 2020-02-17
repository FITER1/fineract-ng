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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.interoperation.domain.InteropTransactionRole;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.ext.JodaDeserializers;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.apache.fineract.interoperation.util.InteropUtil.*;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InteropRequestData {
    @NotNull
    private String transactionCode;
    private String requestCode;
    @NotNull
    private String accountId;
    @NotNull
    private MoneyData amount;
    @NotNull
    private InteropTransactionRole transactionRole;
    private InteropTransactionTypeData transactionType;
    private String note;
    private GeoCodeData geoCode;
    @JsonDeserialize(using = JodaDeserializers.LocalDateTimeDeserializer.class)
    private LocalDateTime expiration;
    private List<ExtensionData> extensionList;

    public LocalDate getExpirationLocalDate() {
        return expiration == null ? null : expiration.toLocalDate();
    }

    public void normalizeAmounts(@NotNull MonetaryCurrency currency) {
        amount.normalizeAmount(currency);
    }

    public static InteropRequestData validateAndParse(final DataValidatorBuilder dataValidator, JsonObject element, FromJsonHelper jsonHelper) {
        if (element == null) {
            return null;
        }

        String transactionCode = jsonHelper.extractStringNamed(PARAM_TRANSACTION_CODE, element);
        DataValidatorBuilder  dataValidatorCopy = dataValidator.reset().parameter(PARAM_TRANSACTION_CODE).value(transactionCode).notBlank();

        String requestCode = jsonHelper.extractStringNamed(PARAM_REQUEST_CODE, element);

        String accountId = jsonHelper.extractStringNamed(PARAM_ACCOUNT_ID, element);
        dataValidatorCopy = dataValidatorCopy.reset().parameter(PARAM_ACCOUNT_ID).value(accountId).notBlank();

        JsonObject moneyElement = jsonHelper.extractJsonObjectNamed(PARAM_AMOUNT, element);
        dataValidatorCopy = dataValidatorCopy.reset().parameter(PARAM_AMOUNT).value(moneyElement).notNull();
        dataValidator.merge(dataValidatorCopy);
        MoneyData amount = MoneyData.validateAndParse(dataValidator, moneyElement, jsonHelper);

        JsonObject transactionTypeElement = jsonHelper.extractJsonObjectNamed(PARAM_TRANSACTION_TYPE, element);
        InteropTransactionTypeData transactionType = InteropTransactionTypeData.validateAndParse(dataValidator, transactionTypeElement, jsonHelper);

        String transactionRoleString = jsonHelper.extractStringNamed(PARAM_TRANSACTION_ROLE, element);
        InteropTransactionRole transactionRole = transactionRoleString == null ? InteropTransactionRole.PAYER : InteropTransactionRole.valueOf(transactionRoleString);

        String note = jsonHelper.extractStringNamed(PARAM_NOTE, element);

        JsonObject geoCodeElement = jsonHelper.extractJsonObjectNamed(PARAM_GEO_CODE, element);
        GeoCodeData geoCode = GeoCodeData.validateAndParse(dataValidator, geoCodeElement, jsonHelper);

        String locale = jsonHelper.extractStringNamed(PARAM_LOCALE, element);
        LocalDateTime expiration = locale == null
                ? jsonHelper.extractLocalTimeNamed(PARAM_EXPIRATION, element, ISO8601_DATE_TIME_FORMAT, DEFAULT_LOCALE)
                : jsonHelper.extractLocalTimeNamed(PARAM_EXPIRATION, element); // PARAM_DATE_FORMAT also must be set

        JsonArray extensionArray = jsonHelper.extractJsonArrayNamed(PARAM_EXTENSION_LIST, element);
        ArrayList<ExtensionData> extensionList = null;
        if (extensionArray != null) {
            extensionList = new ArrayList<>(extensionArray.size());
            for (JsonElement jsonElement : extensionArray) {
                if (jsonElement.isJsonObject())
                    extensionList.add(ExtensionData.validateAndParse(dataValidator, jsonElement.getAsJsonObject(), jsonHelper));
            }
        }

        InteropRequestData requestData = new InteropRequestData();
        requestData.setTransactionCode(transactionCode);
        requestData.setRequestCode(requestCode);
        requestData.setAccountId(accountId);
        requestData.setAmount(amount);
        requestData.setTransactionRole(transactionRole);
        requestData.setTransactionType(transactionType);
        requestData.setNote(note);
        requestData.setGeoCode(geoCode);
        requestData.setExpiration(expiration);
        requestData.setExtensionList(extensionList);

        return dataValidator.hasError() ? null : requestData;
    }
}
