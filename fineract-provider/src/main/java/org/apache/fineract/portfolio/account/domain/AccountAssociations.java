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
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;

import javax.persistence.*;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_portfolio_account_associations")
public class AccountAssociations extends AbstractPersistableCustom<Long> {

    @ManyToOne
    @JoinColumn(name = "loan_account_id", nullable = true)
    private Loan loanAccount;

    @ManyToOne
    @JoinColumn(name = "savings_account_id", nullable = true)
    private SavingsAccount savingsAccount;

    @ManyToOne
    @JoinColumn(name = "linked_loan_account_id", nullable = true)
    private Loan linkedLoanAccount;

    @ManyToOne
    @JoinColumn(name = "linked_savings_account_id", nullable = true)
    private SavingsAccount linkedSavingsAccount;

    @Column(name = "association_type_enum", nullable = false)
    private Integer associationType;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public static AccountAssociations associateSavingsAccount(final Loan loan, final SavingsAccount savingsAccount,
            final Integer associationType, boolean isActive) {
        return AccountAssociations.builder()
            .loanAccount(loan)
            .linkedSavingsAccount(savingsAccount)
            .associationType(associationType)
            .active(isActive)
            .build();
    }

    public static AccountAssociations associateSavingsAccount(final SavingsAccount savingsAccount,
            final SavingsAccount linkedSavingsAccount, final Integer associationType, boolean isActive) {
        return AccountAssociations.builder()
            .savingsAccount(savingsAccount)
            .linkedSavingsAccount(linkedSavingsAccount)
            .associationType(associationType)
            .active(isActive)
            .build();
    }
}
