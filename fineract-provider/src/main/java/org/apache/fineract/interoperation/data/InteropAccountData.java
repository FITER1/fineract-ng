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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.interoperation.domain.InteropIdentifier;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountSubStatusEnum;
import org.apache.fineract.portfolio.savings.domain.SavingsProduct;
import org.joda.time.LocalDate;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InteropAccountData extends CommandProcessingResult {
    @NotNull
    private String accountId;
    @NotNull
    private String savingProductId;
    @NotNull
    private String productName;
    @NotNull
    private String shortProductName;
    @NotNull
    private String currency;
    @NotNull
    private BigDecimal accountBalance;
    @NotNull
    private BigDecimal availableBalance;
    @NotNull
    private SavingsAccountStatusType status;
    private SavingsAccountSubStatusEnum subStatus;
    private AccountType accountType; //differentiate Individual, JLG or Group account
    private DepositAccountType depositType; //differentiate deposit accounts Savings, FD and RD accounts
    @NotNull
    private LocalDate activatedOn;
    private LocalDate statusUpdateOn;
    private LocalDate withdrawnOn;
    private LocalDate balanceOn;
    @NotNull
    private List<InteropIdentifierData> identifiers;

    public static InteropAccountData build(SavingsAccount account) {
        if (account == null)
            return null;

        List<InteropIdentifierData> ids = new ArrayList<>();
        for (InteropIdentifier identifier : account.getIdentifiers()) {
            ids.add(InteropIdentifierData.builder()
                .idValue(identifier.getValue())
                .idType(identifier.getType())
                .subIdOrType(identifier.getSubValueOrType())
                .build());
        }

        SavingsProduct product = account.savingsProduct();
        SavingsAccountSubStatusEnum subStatus = SavingsAccountSubStatusEnum.fromInt(account.getSubStatus());

        return new InteropAccountData(account.getExternalId(), product.getId().toString(), product.getName(),
                product.getShortName(), account.getCurrency().getCode(), account.getAccountBalance(), account.getWithdrawableBalance(),
                account.getStatus(), subStatus, account.getAccountType(), account.depositAccountType(), account.getActivationLocalDate(),
                calcStatusUpdateOn(account), account.getWithdrawnOnDate(), account.retrieveLastTransactionDate(), ids);
    }

    private static LocalDate calcStatusUpdateOn(@NotNull SavingsAccount account) {
        LocalDate date = account.getClosedOnDate();
        if (date != null)
            return date;
        if ((date = account.getWithdrawnOnDate()) != null)
            return date;
        if ((date = account.getActivationLocalDate()) != null)
            return date;
        if ((date = account.getRejectedOnDate()) != null)
            return date;
        if ((date = account.getApprovedOnDate()) != null)
            return date;
        if ((date = account.getSubmittedOnDate()) != null)
            return date;
        return null;
    }
}
