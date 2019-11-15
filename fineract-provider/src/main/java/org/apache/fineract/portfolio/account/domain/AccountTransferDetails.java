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
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_account_transfer_details")
public class AccountTransferDetails extends AbstractPersistableCustom<Long> {

    @ManyToOne
    @JoinColumn(name = "from_office_id", nullable = false)
    private Office fromOffice;

    @ManyToOne
    @JoinColumn(name = "from_client_id", nullable = false)
    private Client fromClient;

    @ManyToOne
    @JoinColumn(name = "from_savings_account_id", nullable = true)
    private SavingsAccount fromSavingsAccount;

    @ManyToOne
    @JoinColumn(name = "to_office_id", nullable = false)
    private Office toOffice;

    @ManyToOne
    @JoinColumn(name = "to_client_id", nullable = false)
    private Client toClient;

    @ManyToOne
    @JoinColumn(name = "to_savings_account_id", nullable = true)
    private SavingsAccount toSavingsAccount;

    @ManyToOne
    @JoinColumn(name = "to_loan_account_id", nullable = true)
    private Loan toLoanAccount;

    @ManyToOne
    @JoinColumn(name = "from_loan_account_id", nullable = true)
    private Loan fromLoanAccount;

    @Column(name = "transfer_type")
    private Integer transferType;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "accountTransferDetails", orphanRemoval = true, fetch=FetchType.EAGER)
    private List<AccountTransferTransaction> accountTransferTransactions = new ArrayList<>();

    @OneToOne(mappedBy = "accountTransferDetails", cascade = CascadeType.ALL, optional = true, orphanRemoval = true, fetch = FetchType.EAGER)
    private AccountTransferStandingInstruction accountTransferStandingInstruction;

    public static AccountTransferDetails savingsToSavingsTransfer(final Office fromOffice, final Client fromClient,
            final SavingsAccount fromSavingsAccount, final Office toOffice, final Client toClient, final SavingsAccount toSavingsAccount,
            Integer transferType) {

        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromSavingsAccount(fromSavingsAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toSavingsAccount(toSavingsAccount)
            .transferType(transferType)
            .build();
    }

    public static AccountTransferDetails savingsToLoanTransfer(final Office fromOffice, final Client fromClient,
            final SavingsAccount fromSavingsAccount, final Office toOffice, final Client toClient, final Loan toLoanAccount,
            Integer transferType) {

        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromSavingsAccount(fromSavingsAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toLoanAccount(toLoanAccount)
            .transferType(transferType)
            .build();
    }

    public static AccountTransferDetails loanToSavingsTransfer(final Office fromOffice, final Client fromClient,
                                                               final Loan fromLoanAccount, final Office toOffice, final Client toClient, final SavingsAccount toSavingsAccount,
                                                               Integer transferType) {
        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromLoanAccount(fromLoanAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toSavingsAccount(toSavingsAccount)
            .transferType(transferType)
            .build();
    }

    public static AccountTransferDetails loanToLoanTransfer(Office fromOffice, Client fromClient, Loan fromLoanAccount, Office toOffice, Client toClient,
                                                            Loan toLoanAccount, Integer transferType) {

        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromLoanAccount(fromLoanAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toLoanAccount(toLoanAccount)
            .transferType(transferType)
            .build();
    }

    public void addAccountTransferTransaction(AccountTransferTransaction accountTransferTransaction) {
        this.accountTransferTransactions.add(accountTransferTransaction);
    }

    public AccountTransferType transferType() {
        return AccountTransferType.fromInt(this.transferType);
    }
}