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
package org.apache.fineract.accounting.provisioning.domain;

import lombok.*;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.provisioning.domain.ProvisioningCategory;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;

import javax.persistence.*;
import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_loanproduct_provisioning_entry")
public class LoanProductProvisioningEntry extends AbstractPersistableCustom<Long> {

    @ManyToOne(optional = false)
    @JoinColumn(name = "history_id", referencedColumnName = "id", nullable = false)
    private ProvisioningEntry entry;

    @Column(name = "criteria_id", nullable = false)
    private Long criteriaId;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct loanProduct;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ProvisioningCategory provisioningCategory;

    @Column(name = "overdue_in_days", nullable = false)
    private Long overdueInDays;

    @Column(name = "reseve_amount", nullable = false)
    private BigDecimal reservedAmount;

    @ManyToOne
    @JoinColumn(name = "liability_account", nullable = false)
    private GLAccount liabilityAccount;

    @ManyToOne
    @JoinColumn(name = "expense_account", nullable = false)
    private GLAccount expenseAccount;

    public LoanProductProvisioningEntry(final LoanProduct loanProduct, final Office office, final String currencyCode,
            final ProvisioningCategory provisioningCategory, final Long overdueInDays, final BigDecimal reservedAmount,
            final GLAccount liabilityAccount, final GLAccount expenseAccount, Long criteriaId) {
        this.loanProduct = loanProduct;
        this.office = office;
        this.currencyCode = currencyCode;
        this.provisioningCategory = provisioningCategory;
        this.overdueInDays = overdueInDays;
        this.reservedAmount = reservedAmount;
        this.liabilityAccount = liabilityAccount;
        this.expenseAccount = expenseAccount;
        this.criteriaId = criteriaId ;
    }

    public void addReservedAmount(BigDecimal value) {
        this.reservedAmount = this.reservedAmount.add(value) ;
    }
}
