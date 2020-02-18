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
package org.apache.fineract.portfolio.account.data;

import lombok.*;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.domain.AccountTransferRecurrenceType;
import org.apache.fineract.portfolio.account.domain.AccountTransferType;
import org.apache.fineract.portfolio.account.domain.StandingInstructionType;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

import java.math.BigDecimal;
import java.util.Collection;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StandingInstructionData {
    private Long id;
    private Long accountDetailId;
    private String name;
    private OfficeData fromOffice;
    private ClientData fromClient;
    private EnumOptionData fromAccountType;
    private PortfolioAccountData fromAccount;
    private OfficeData toOffice;
    private ClientData toClient;
    private EnumOptionData toAccountType;
    private PortfolioAccountData toAccount;
    private EnumOptionData transferType;
    private EnumOptionData priority;
    private EnumOptionData instructionType;
    private EnumOptionData status;
    private BigDecimal amount;
    private LocalDate validFrom;
    private LocalDate validTill;
    private EnumOptionData recurrenceType;
    private EnumOptionData recurrenceFrequency;
    private Integer recurrenceInterval;
    private MonthDay recurrenceOnMonthDay;
    private Page<AccountTransferData> transactions;
    private Collection<OfficeData> fromOfficeOptions;
    private Collection<ClientData> fromClientOptions;
    private Collection<EnumOptionData> fromAccountTypeOptions;
    private Collection<PortfolioAccountData> fromAccountOptions;
    private Collection<OfficeData> toOfficeOptions;
    private Collection<ClientData> toClientOptions;
    private Collection<EnumOptionData> toAccountTypeOptions;
    private Collection<PortfolioAccountData> toAccountOptions;
    private Collection<EnumOptionData> transferTypeOptions;
    private Collection<EnumOptionData> statusOptions;
    private Collection<EnumOptionData> instructionTypeOptions;
    private Collection<EnumOptionData> priorityOptions;
    private Collection<EnumOptionData> recurrenceTypeOptions;
    private Collection<EnumOptionData> recurrenceFrequencyOptions;

    public StandingInstructionType instructionType() {
        StandingInstructionType standingInstructionType = null;
        if (this.instructionType != null) {
            standingInstructionType = StandingInstructionType.fromInt(this.instructionType.getId().intValue());
        }
        return standingInstructionType;
    }

    public AccountTransferRecurrenceType recurrenceType() {
        AccountTransferRecurrenceType recurrenceType = null;
        if (this.recurrenceType != null) {
            recurrenceType = AccountTransferRecurrenceType.fromInt(this.recurrenceType.getId().intValue());
        }
        return recurrenceType;
    }

    public PeriodFrequencyType recurrenceFrequency() {
        PeriodFrequencyType frequencyType = null;
        if (this.recurrenceFrequency != null) {
            frequencyType = PeriodFrequencyType.fromInt(this.recurrenceFrequency.getId().intValue());
        }
        return frequencyType;
    }

    public PortfolioAccountType fromAccountType() {
        PortfolioAccountType accountType = null;
        if (this.fromAccountType != null) {
            accountType = PortfolioAccountType.fromInt(this.fromAccountType.getId().intValue());
        }
        return accountType;
    }

    public PortfolioAccountType toAccountType() {
        PortfolioAccountType accountType = null;
        if (this.toAccountType != null) {
            accountType = PortfolioAccountType.fromInt(this.toAccountType.getId().intValue());
        }
        return accountType;
    }

    public AccountTransferType transferType() {
        AccountTransferType accountTransferType = null;
        if (this.transferType != null) {
            accountTransferType = AccountTransferType.fromInt(this.transferType.getId().intValue());
        }
        return accountTransferType;
    }

    public Integer recurrenceOnDay() {
        Integer recurrenceOnDay = 0;
        if (this.recurrenceOnMonthDay != null) {
            recurrenceOnDay = this.recurrenceOnMonthDay.get(DateTimeFieldType.dayOfMonth());
        }
        return recurrenceOnDay;
    }

    public Integer recurrenceOnMonth() {
        Integer recurrenceOnMonth = 0;
        if (this.recurrenceOnMonthDay != null) {
            recurrenceOnMonth = this.recurrenceOnMonthDay.get(DateTimeFieldType.monthOfYear());
        }
        return recurrenceOnMonth;
    }

    public Integer toTransferType() {
        Integer transferType = null;
        AccountTransferType accountTransferType = transferType();
        if (accountTransferType.isChargePayment()) {
            transferType = LoanTransactionType.CHARGE_PAYMENT.getValue();
        } else if (accountTransferType.isLoanRepayment()) {
            transferType = LoanTransactionType.REPAYMENT.getValue();
        }
        return transferType;
    }
}