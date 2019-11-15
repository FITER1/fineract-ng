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
package org.apache.fineract.infrastructure.creditbureau.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.*;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_creditbureau_loanproduct_mapping")
public class CreditBureauLoanProductMapping extends AbstractPersistableCustom<Long> {

    @Column(name = "is_CreditCheck_Mandatory")
    private boolean isCreditCheckMandatory;

    @Column(name = "skip_CreditCheck_in_Failure")
    private boolean skipCreditCheckInFailure;

    @Column(name = "stale_Period")
    private int stalePeriod;

    @Column(name = "is_active")
    private boolean active;

    @ManyToOne
    private OrganisationCreditBureau organisation_creditbureau;

    @OneToOne
    @JoinColumn(name = "loan_product_id")
    private LoanProduct loanProduct;

    public static CreditBureauLoanProductMapping fromJson(final JsonCommand command, OrganisationCreditBureau organisation_creditbureau, LoanProduct loanProduct) {
        boolean isCreditCheckMandatory = Boolean.TRUE.equals(command.booleanPrimitiveValueOfParameterNamed("isCreditcheckMandatory"));
        boolean skipCreditCheckInFailure = Boolean.TRUE.equals(command.booleanPrimitiveValueOfParameterNamed("skipCreditcheckInFailure"));
        boolean is_active = Boolean.TRUE.equals(command.booleanPrimitiveValueOfParameterNamed("is_active"));
        int stalePeriod = command.integerValueOfParameterNamed("stalePeriod") == null ? -1 : command.integerValueOfParameterNamed("stalePeriod");

        return CreditBureauLoanProductMapping.builder().isCreditCheckMandatory(isCreditCheckMandatory).skipCreditCheckInFailure(skipCreditCheckInFailure).stalePeriod(stalePeriod).active(is_active).organisation_creditbureau(organisation_creditbureau).loanProduct(loanProduct).build();
    }
}
