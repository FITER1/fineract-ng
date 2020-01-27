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
package org.apache.fineract.portfolio.account.domain;

import lombok.*;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_account_transfer_transaction")
public class AccountTransferTransaction extends AbstractPersistableCustom<Long> {

    @ManyToOne
    @JoinColumn(name = "account_transfer_details_id", nullable = true)
    private AccountTransferDetails accountTransferDetails;

    @ManyToOne
    @JoinColumn(name = "from_savings_transaction_id", nullable = true)
    private SavingsAccountTransaction fromSavingsTransaction;

    @ManyToOne
    @JoinColumn(name = "to_savings_transaction_id", nullable = true)
    private SavingsAccountTransaction toSavingsTransaction;

    @ManyToOne
    @JoinColumn(name = "to_loan_transaction_id", nullable = true)
    private LoanTransaction toLoanTransaction;

    @ManyToOne
    @JoinColumn(name = "from_loan_transaction_id", nullable = true)
    private LoanTransaction fromLoanTransaction;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed = false;

    @Temporal(TemporalType.DATE)
    @Column(name = "transaction_date")
    private Date date;

    @Embedded
    private MonetaryCurrency currency;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "description", length = 100)
    private String description;

    public static AccountTransferTransaction savingsToSavingsTransfer(final AccountTransferDetails accountTransferDetails, final SavingsAccountTransaction withdrawal, final SavingsAccountTransaction deposit, final LocalDate transactionDate, final Money transactionAmount, final String description) {

        return AccountTransferTransaction.builder().accountTransferDetails(accountTransferDetails).fromSavingsTransaction(withdrawal).toSavingsTransaction(deposit).amount(transactionAmount.getAmount()).date(transactionDate.toDate()).description(description).build();
    }

    static AccountTransferTransaction savingsToLoanTransfer(final AccountTransferDetails accountTransferDetails, final SavingsAccountTransaction withdrawal, final LoanTransaction loanRepaymentTransaction, final LocalDate transactionDate, final Money transactionAmount, final String description) {
        return AccountTransferTransaction.builder()
            .accountTransferDetails(accountTransferDetails)
            .fromSavingsTransaction(withdrawal)
            .toLoanTransaction(loanRepaymentTransaction)
            .amount(transactionAmount.getAmount())
            .currency(transactionAmount.getCurrency())
            .amount(transactionAmount.getAmountDefaultedToNullIfZero())
            .date(transactionDate.toDate())
            .description(description)
            .build();
    }

    static AccountTransferTransaction loanToSavingsTransfer(final AccountTransferDetails accountTransferDetails, final SavingsAccountTransaction deposit, final LoanTransaction loanRefundTransaction, final LocalDate transactionDate, final Money transactionAmount, final String description) {
        return AccountTransferTransaction.builder()
            .accountTransferDetails(accountTransferDetails)
            .fromLoanTransaction(loanRefundTransaction)
            .toSavingsTransaction(deposit)
            .amount(transactionAmount.getAmount())
            .currency(transactionAmount.getCurrency())
            .amount(transactionAmount.getAmountDefaultedToNullIfZero())
            .date(transactionDate.toDate())
            .description(description)
            .build();
    }

    static AccountTransferTransaction loanToLoanTransfer(AccountTransferDetails accountTransferDetails, LoanTransaction disburseTransaction, LoanTransaction repaymentTransaction, LocalDate transactionDate, Money transactionMonetaryAmount, String description) {
        return AccountTransferTransaction.builder()
            .accountTransferDetails(accountTransferDetails)
            .fromLoanTransaction(repaymentTransaction)
            .toLoanTransaction(disburseTransaction)
            .amount(transactionMonetaryAmount.getAmount())
            .currency(transactionMonetaryAmount.getCurrency())
            .amount(transactionMonetaryAmount.getAmountDefaultedToNullIfZero())
            .date(transactionDate.toDate())
            .description(description).build();
    }
}