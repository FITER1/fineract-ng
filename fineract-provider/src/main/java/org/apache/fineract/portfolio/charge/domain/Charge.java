
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
package org.apache.fineract.portfolio.charge.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.tax.domain.TaxGroup;
import org.joda.time.MonthDay;

import javax.persistence.*;
import java.math.BigDecimal;

@SuperBuilder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_charge", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }, name = "name") })
public class Charge extends AbstractPersistableCustom<Long> {

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "charge_applies_to_enum", nullable = false)
    private Integer chargeAppliesTo;

    @Column(name = "charge_time_enum", nullable = false)
    private Integer chargeTimeType;

    @Column(name = "charge_calculation_enum")
    private Integer chargeCalculation;

    @Column(name = "charge_payment_mode_enum")
    private Integer chargePaymentMode;

    @Column(name = "fee_on_day")
    private Integer feeOnDay;

    @Column(name = "fee_interval")
    private Integer feeInterval;

    @Column(name = "fee_on_month")
    private Integer feeOnMonth;

    @Column(name = "is_penalty", nullable = false)
    private boolean penalty;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "min_cap", scale = 6, precision = 19)
    private BigDecimal minCap;

    @Column(name = "max_cap", scale = 6, precision = 19)
    private BigDecimal maxCap;

    @Column(name = "fee_frequency")
    private Integer feeFrequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_or_liability_account_id")
    private GLAccount account;

    @ManyToOne
    @JoinColumn(name = "tax_group_id")
    private TaxGroup taxGroup;

    public boolean isLoanCharge() {
        return ChargeAppliesTo.fromInt(this.chargeAppliesTo).isLoanCharge();
    }

    public boolean isAllowedLoanChargeTime() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isAllowedLoanChargeTime();
    }

    public boolean isAllowedClientChargeTime() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isAllowedClientChargeTime();
    }

    public boolean isSavingsCharge() {
        return ChargeAppliesTo.fromInt(this.chargeAppliesTo).isSavingsCharge();
    }

    public boolean isClientCharge() {
        return ChargeAppliesTo.fromInt(this.chargeAppliesTo).isClientCharge();
    }

    public boolean isAllowedSavingsChargeTime() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isAllowedSavingsChargeTime();
    }

    public boolean isAllowedSavingsChargeCalculationType() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isAllowedSavingsChargeCalculationType();
    }

    public boolean isAllowedClientChargeCalculationType() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isAllowedClientChargeCalculationType();
    }

    public boolean isPercentageOfApprovedAmount() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isPercentageOfAmount();
    }

    public boolean isPercentageOfDisbursementAmount() {
        return ChargeCalculationType.fromInt(this.chargeCalculation).isPercentageOfDisbursementAmount();
    }

    public boolean isMonthlyFee() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isMonthlyFee();
    }

    public boolean isAnnualFee() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isAnnualFee();
    }

    public boolean isOverdueInstallment() {
        return ChargeTimeType.fromInt(this.chargeTimeType).isOverdueInstallment();
    }

    public MonthDay getFeeOnMonthDay() {
        MonthDay feeOnMonthDay = null;
        if (this.feeOnDay != null && this.feeOnMonth != null) {
            feeOnMonthDay = new MonthDay(this.feeOnMonth, this.feeOnDay);
        }
        return feeOnMonthDay;
    }

    public boolean isDisbursementCharge() {
        return ChargeTimeType.fromInt(this.chargeTimeType).equals(ChargeTimeType.DISBURSEMENT)
                || ChargeTimeType.fromInt(this.chargeTimeType).equals(ChargeTimeType.TRANCHE_DISBURSEMENT);
    }
}