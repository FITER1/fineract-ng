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

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.interoperation.util.MathUtil;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountChargePaidBy;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.service.SavingsEnumerations;
import org.joda.time.LocalDate;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InteropTransactionData extends CommandProcessingResult {
    @NotNull
    private String accountId;
    @NotNull
    private String savingTransactionId;
    @NotNull
    private SavingsAccountTransactionType transactionType;
    @NotNull
    private BigDecimal amount;
    private BigDecimal chargeAmount;
    @NotNull
    private String currency;
    @NotNull
    private BigDecimal accountBalance;
    @NotNull
    private LocalDate bookingDateTime;
    @NotNull
    private LocalDate valueDateTime;
    private String note;

    public static InteropTransactionData build(SavingsAccountTransaction transaction) {
        if (transaction == null)
            return null;

        SavingsAccount savingsAccount = transaction.getSavingsAccount();

        String transactionId = transaction.getId().toString();
        SavingsAccountTransactionType transactionType = SavingsAccountTransactionType.fromInt(transaction.getTypeOf());
        BigDecimal amount = transaction.getAmount();

        BigDecimal chargeAmount = null;
        for (SavingsAccountChargePaidBy charge : transaction.getSavingsAccountChargesPaid()) {
            chargeAmount = MathUtil.add(chargeAmount, charge.getAmount());
        }

        String currency = savingsAccount.getCurrency().getCode();
        BigDecimal runningBalance = transaction.getRunningBalance(savingsAccount.getCurrency()).getAmount();

        LocalDate bookingDateTime = transaction.getTransactionLocalDate();
        LocalDate endOfBalanceLocalDate = transaction.getEndOfBalanceLocalDate();
        LocalDate valueDateTime = endOfBalanceLocalDate == null ? bookingDateTime : endOfBalanceLocalDate;

        StringBuilder sb = new StringBuilder();
        int currLength = 0;
        for (Note note : transaction.getNotes()) {
            String s = note.getNote();
            if (s == null)
                continue;

            int availableLength = 500 - currLength;
            if (availableLength <= 1)
                break;

            if (currLength > 0) {
                sb.append(' ');
                availableLength--;
            }
            if (s.length() > availableLength)
                s = s.substring(availableLength);
            sb.append(s);
            currLength = sb.length();
        }
        if (currLength == 0) {
            sb.append(SavingsEnumerations.transactionType(transactionType).getValue());
        }

        InteropTransactionData transactionData = InteropTransactionData.builder()
            .accountId(savingsAccount.getExternalId())
            .savingTransactionId(transactionId)
            .transactionType(transactionType)
            .amount(amount)
            .chargeAmount(chargeAmount)
            .currency(currency)
            .accountBalance(runningBalance)
            .bookingDateTime(bookingDateTime)
            .valueDateTime(valueDateTime)
            .note(sb.toString())
            .build();

        transactionData.setResourceIdentifier(savingsAccount.getId()+"");
        transactionData.setResourceId(savingsAccount.getId());

        return transactionData;
    }
}
