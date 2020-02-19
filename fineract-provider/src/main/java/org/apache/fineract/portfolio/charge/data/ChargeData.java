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
package org.apache.fineract.portfolio.charge.data;

import lombok.*;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountChargeData;
import org.apache.fineract.portfolio.shareaccounts.data.ShareAccountChargeData;
import org.apache.fineract.portfolio.tax.data.TaxGroupData;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChargeData implements Comparable<ChargeData>, Serializable {
    private Long id;
    private String name;
    private boolean active;
    private boolean penalty;
    private CurrencyData currency;
    private BigDecimal amount;
    private EnumOptionData chargeTimeType;
    private EnumOptionData chargeAppliesTo;
    private EnumOptionData chargeCalculationType;
    private EnumOptionData chargePaymentMode;
    private MonthDay feeOnMonthDay;
    private Integer feeInterval;
    private BigDecimal minCap;
    private BigDecimal maxCap;
    private EnumOptionData feeFrequency;
    private GLAccountData incomeOrLiabilityAccount;
    private TaxGroupData taxGroup;
    private Collection<CurrencyData> currencyOptions;
    private List<EnumOptionData> chargeCalculationTypeOptions;//
    private List<EnumOptionData> chargeAppliesToOptions;//
    private List<EnumOptionData> chargeTimeTypeOptions;//
    private List<EnumOptionData> chargePaymetModeOptions;//
    private List<EnumOptionData> loanChargeCalculationTypeOptions;
    private List<EnumOptionData> loanChargeTimeTypeOptions;
    private List<EnumOptionData> savingsChargeCalculationTypeOptions;
    private List<EnumOptionData> savingsChargeTimeTypeOptions;
    private List<EnumOptionData> clientChargeCalculationTypeOptions;
    private List<EnumOptionData> clientChargeTimeTypeOptions;
    private List<EnumOptionData> shareChargeCalculationTypeOptions;
    private List<EnumOptionData> shareChargeTimeTypeOptions;
    private List<EnumOptionData> feeFrequencyOptions;
    private Map<String, List<GLAccountData>> incomeOrLiabilityAccountOptions;
    private Collection<TaxGroupData> taxGroupOptions;

    public static ChargeData template(final Collection<CurrencyData> currencyOptions,
            final List<EnumOptionData> chargeCalculationTypeOptions, final List<EnumOptionData> chargeAppliesToOptions,
            final List<EnumOptionData> chargeTimeTypeOptions, final List<EnumOptionData> chargePaymentModeOptions,
            final List<EnumOptionData> loansChargeCalculationTypeOptions, final List<EnumOptionData> loansChargeTimeTypeOptions,
            final List<EnumOptionData> savingsChargeCalculationTypeOptions, final List<EnumOptionData> savingsChargeTimeTypeOptions,
            final List<EnumOptionData> clientChargeCalculationTypeOptions, final List<EnumOptionData> clientChargeTimeTypeOptions,
            final List<EnumOptionData> feeFrequencyOptions, final Map<String, List<GLAccountData>> incomeOrLiabilityAccountOptions,
            final Collection<TaxGroupData> taxGroupOptions, final List<EnumOptionData> shareChargeCalculationTypeOptions,
            final List<EnumOptionData> shareChargeTimeTypeOptions) {
        final GLAccountData account = null;
        final TaxGroupData taxGroupData = null;

        return new ChargeData(null, null, null, null, null, null, null, null, false, false, taxGroupData, currencyOptions,
                chargeCalculationTypeOptions, chargeAppliesToOptions, chargeTimeTypeOptions, chargePaymentModeOptions,
                loansChargeCalculationTypeOptions, loansChargeTimeTypeOptions, savingsChargeCalculationTypeOptions,
                savingsChargeTimeTypeOptions, clientChargeCalculationTypeOptions, clientChargeTimeTypeOptions, null, null, null, null,
                null, feeFrequencyOptions, account, incomeOrLiabilityAccountOptions, taxGroupOptions, shareChargeCalculationTypeOptions,
                shareChargeTimeTypeOptions);
    }

    public static ChargeData withTemplate(final ChargeData charge, final ChargeData template) {
        return new ChargeData(charge.id, charge.name, charge.amount, charge.currency, charge.chargeTimeType, charge.chargeAppliesTo,
                charge.chargeCalculationType, charge.chargePaymentMode, charge.penalty, charge.active, charge.taxGroup,
                template.currencyOptions, template.chargeCalculationTypeOptions, template.chargeAppliesToOptions,
                template.chargeTimeTypeOptions, template.chargePaymetModeOptions, template.loanChargeCalculationTypeOptions,
                template.loanChargeTimeTypeOptions, template.savingsChargeCalculationTypeOptions, template.savingsChargeTimeTypeOptions,
                template.clientChargeCalculationTypeOptions, template.clientChargeTimeTypeOptions, charge.feeOnMonthDay,
                charge.feeInterval, charge.minCap, charge.maxCap, charge.feeFrequency, template.feeFrequencyOptions,
                charge.incomeOrLiabilityAccount, template.incomeOrLiabilityAccountOptions, template.taxGroupOptions,
                template.shareChargeCalculationTypeOptions, template.shareChargeTimeTypeOptions);
    }

    public static ChargeData instance(final Long id, final String name, final BigDecimal amount, final CurrencyData currency,
            final EnumOptionData chargeTimeType, final EnumOptionData chargeAppliesTo, final EnumOptionData chargeCalculationType,
            final EnumOptionData chargePaymentMode, final MonthDay feeOnMonthDay, final Integer feeInterval, final boolean penalty,
            final boolean active, final BigDecimal minCap, final BigDecimal maxCap, final EnumOptionData feeFrequency,
            final GLAccountData accountData, TaxGroupData taxGroupData) {

        final Collection<CurrencyData> currencyOptions = null;
        final List<EnumOptionData> chargeCalculationTypeOptions = null;
        final List<EnumOptionData> chargeAppliesToOptions = null;
        final List<EnumOptionData> chargeTimeTypeOptions = null;
        final List<EnumOptionData> chargePaymentModeOptions = null;
        final List<EnumOptionData> loansChargeCalculationTypeOptions = null;
        final List<EnumOptionData> loansChargeTimeTypeOptions = null;
        final List<EnumOptionData> savingsChargeCalculationTypeOptions = null;
        final List<EnumOptionData> savingsChargeTimeTypeOptions = null;
        final List<EnumOptionData> feeFrequencyOptions = null;
        final List<EnumOptionData> clientChargeCalculationTypeOptions = null;
        final List<EnumOptionData> clientChargeTimeTypeOptions = null;
        final Map<String, List<GLAccountData>> incomeOrLiabilityAccountOptions = null;
        final List<EnumOptionData> shareChargeCalculationTypeOptions = null;
        final List<EnumOptionData> shareChargeTimeTypeOptions = null;
        final Collection<TaxGroupData> taxGroupOptions = null;
        return new ChargeData(id, name, amount, currency, chargeTimeType, chargeAppliesTo, chargeCalculationType, chargePaymentMode,
                penalty, active, taxGroupData, currencyOptions, chargeCalculationTypeOptions, chargeAppliesToOptions,
                chargeTimeTypeOptions, chargePaymentModeOptions, loansChargeCalculationTypeOptions, loansChargeTimeTypeOptions,
                savingsChargeCalculationTypeOptions, savingsChargeTimeTypeOptions, clientChargeCalculationTypeOptions,
                clientChargeTimeTypeOptions, feeOnMonthDay, feeInterval, minCap, maxCap, feeFrequency, feeFrequencyOptions, accountData,
                incomeOrLiabilityAccountOptions, taxGroupOptions, shareChargeCalculationTypeOptions, shareChargeTimeTypeOptions);
    }

    public static ChargeData lookup(final Long id, final String name, final boolean isPenalty) {
        final BigDecimal amount = null;
        final CurrencyData currency = null;
        final EnumOptionData chargeTimeType = null;
        final EnumOptionData chargeAppliesTo = null;
        final EnumOptionData chargeCalculationType = null;
        final EnumOptionData chargePaymentMode = null;
        final MonthDay feeOnMonthDay = null;
        final Integer feeInterval = null;
        final Boolean penalty = isPenalty;
        final Boolean active = false;
        final BigDecimal minCap = null;
        final BigDecimal maxCap = null;
        final Collection<CurrencyData> currencyOptions = null;
        final List<EnumOptionData> chargeCalculationTypeOptions = null;
        final List<EnumOptionData> chargeAppliesToOptions = null;
        final List<EnumOptionData> chargeTimeTypeOptions = null;
        final List<EnumOptionData> chargePaymentModeOptions = null;
        final List<EnumOptionData> loansChargeCalculationTypeOptions = null;
        final List<EnumOptionData> loansChargeTimeTypeOptions = null;
        final List<EnumOptionData> savingsChargeCalculationTypeOptions = null;
        final List<EnumOptionData> savingsChargeTimeTypeOptions = null;
        final List<EnumOptionData> clientChargeCalculationTypeOptions = null;
        final List<EnumOptionData> clientChargeTimeTypeOptions = null;
        final EnumOptionData feeFrequency = null;
        final List<EnumOptionData> feeFrequencyOptions = null;
        final GLAccountData account = null;
        final Map<String, List<GLAccountData>> incomeOrLiabilityAccountOptions = null;
        final List<EnumOptionData> shareChargeCalculationTypeOptions = null;
        final List<EnumOptionData> shareChargeTimeTypeOptions = null;
        final TaxGroupData taxGroupData = null;
        final Collection<TaxGroupData> taxGroupOptions = null;
        return new ChargeData(id, name, amount, currency, chargeTimeType, chargeAppliesTo, chargeCalculationType, chargePaymentMode,
                penalty, active, taxGroupData, currencyOptions, chargeCalculationTypeOptions, chargeAppliesToOptions,
                chargeTimeTypeOptions, chargePaymentModeOptions, loansChargeCalculationTypeOptions, loansChargeTimeTypeOptions,
                savingsChargeCalculationTypeOptions, savingsChargeTimeTypeOptions, clientChargeCalculationTypeOptions,
                clientChargeTimeTypeOptions, feeOnMonthDay, feeInterval, minCap, maxCap, feeFrequency, feeFrequencyOptions, account,
                incomeOrLiabilityAccountOptions, taxGroupOptions, shareChargeCalculationTypeOptions, shareChargeTimeTypeOptions);
    }

    private ChargeData(final Long id, final String name, final BigDecimal amount, final CurrencyData currency,
            final EnumOptionData chargeTimeType, final EnumOptionData chargeAppliesTo, final EnumOptionData chargeCalculationType,
            final EnumOptionData chargePaymentMode, final boolean penalty, final boolean active, final TaxGroupData taxGroupData,
            final Collection<CurrencyData> currencyOptions, final List<EnumOptionData> chargeCalculationTypeOptions,
            final List<EnumOptionData> chargeAppliesToOptions, final List<EnumOptionData> chargeTimeTypeOptions,
            final List<EnumOptionData> chargePaymentModeOptions, final List<EnumOptionData> loansChargeCalculationTypeOptions,
            final List<EnumOptionData> loansChargeTimeTypeOptions, final List<EnumOptionData> savingsChargeCalculationTypeOptions,
            final List<EnumOptionData> savingsChargeTimeTypeOptions, final List<EnumOptionData> clientChargeCalculationTypeOptions,
            final List<EnumOptionData> clientChargeTimeTypeOptions, final MonthDay feeOnMonthDay, final Integer feeInterval,
            final BigDecimal minCap, final BigDecimal maxCap, final EnumOptionData feeFrequency,
            final List<EnumOptionData> feeFrequencyOptions, final GLAccountData account,
            final Map<String, List<GLAccountData>> incomeOrLiabilityAccountOptions, final Collection<TaxGroupData> taxGroupOptions,
            final List<EnumOptionData> shareChargeCalculationTypeOptions, final List<EnumOptionData> shareChargeTimeTypeOptions) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.currency = currency;
        this.chargeTimeType = chargeTimeType;
        this.chargeAppliesTo = chargeAppliesTo;
        this.chargeCalculationType = chargeCalculationType;
        this.chargePaymentMode = chargePaymentMode;
        this.feeInterval = feeInterval;
        this.feeOnMonthDay = feeOnMonthDay;
        this.penalty = penalty;
        this.active = active;
        this.minCap = minCap;
        this.maxCap = maxCap;
        this.currencyOptions = currencyOptions;
        this.chargeCalculationTypeOptions = chargeCalculationTypeOptions;
        this.chargeAppliesToOptions = chargeAppliesToOptions;
        this.chargeTimeTypeOptions = chargeTimeTypeOptions;
        this.chargePaymetModeOptions = chargePaymentModeOptions;
        this.savingsChargeCalculationTypeOptions = savingsChargeCalculationTypeOptions;
        this.savingsChargeTimeTypeOptions = savingsChargeTimeTypeOptions;
        this.clientChargeCalculationTypeOptions = clientChargeCalculationTypeOptions;
        this.clientChargeTimeTypeOptions = clientChargeTimeTypeOptions;
        this.loanChargeCalculationTypeOptions = loansChargeCalculationTypeOptions;
        this.loanChargeTimeTypeOptions = loansChargeTimeTypeOptions;
        this.feeFrequency = feeFrequency;
        this.feeFrequencyOptions = feeFrequencyOptions;
        this.incomeOrLiabilityAccount = account;
        this.incomeOrLiabilityAccountOptions = incomeOrLiabilityAccountOptions;
        this.taxGroup = taxGroupData;
        this.taxGroupOptions = taxGroupOptions;
        this.shareChargeCalculationTypeOptions = shareChargeCalculationTypeOptions;
        this.shareChargeTimeTypeOptions = shareChargeTimeTypeOptions;
    }

    @Override
    public int compareTo(final ChargeData obj) {
        if (obj == null) { return -1; }

        return obj.id.compareTo(this.id);
    }

    public LoanChargeData toLoanChargeData() {

        BigDecimal percentage = null;
        if (this.chargeCalculationType.getId() == 2) {
            percentage = this.amount;
        }

        return LoanChargeData.newLoanChargeDetails(this.id, this.name, this.currency, this.amount, percentage, this.chargeTimeType,
                this.chargeCalculationType, this.penalty, this.chargePaymentMode, this.minCap, this.maxCap);
    }

    public SavingsAccountChargeData toSavingsAccountChargeData() {

        final Long savingsChargeId = null;
        final Long savingsAccountId = null;
        final BigDecimal amountPaid = BigDecimal.ZERO;
        final BigDecimal amountWaived = BigDecimal.ZERO;
        final BigDecimal amountWrittenOff = BigDecimal.ZERO;
        final BigDecimal amountOutstanding = BigDecimal.ZERO;
        final BigDecimal percentage = BigDecimal.ZERO;
        final BigDecimal amountPercentageAppliedTo = BigDecimal.ZERO;
        final Collection<ChargeData> chargeOptions = null;
        final LocalDate dueAsOfDate = null;
        final Boolean isActive = null;
        final LocalDate inactivationDate = null;

        return SavingsAccountChargeData.instance(savingsChargeId, this.id, savingsAccountId, this.name, this.currency, this.amount,
                amountPaid, amountWaived, amountWrittenOff, amountOutstanding, this.chargeTimeType, dueAsOfDate,
                this.chargeCalculationType, percentage, amountPercentageAppliedTo, chargeOptions, this.penalty, this.feeOnMonthDay,
                this.feeInterval, isActive, inactivationDate);
    }

    public ShareAccountChargeData toShareAccountChargeData() {

        final Long shareChargeId = null;
        final Long shareAccountId = null;
        final BigDecimal amountPaid = BigDecimal.ZERO;
        final BigDecimal amountWaived = BigDecimal.ZERO;
        final BigDecimal amountWrittenOff = BigDecimal.ZERO;
        final BigDecimal amountOutstanding = BigDecimal.ZERO;
        final BigDecimal percentage = BigDecimal.ZERO;
        final BigDecimal amountPercentageAppliedTo = BigDecimal.ZERO;
        final Collection<ChargeData> chargeOptions = null;
        final Boolean isActive = null;
        final BigDecimal chargeAmountOrPercentage = BigDecimal.ZERO ;
        
        return new ShareAccountChargeData(shareChargeId, this.id, shareAccountId, this.name, this.currency, this.amount, amountPaid,
                amountWaived, amountWrittenOff, amountOutstanding, this.chargeTimeType, this.chargeCalculationType, percentage,
                amountPercentageAppliedTo, chargeOptions, isActive, chargeAmountOrPercentage);
    }

    public boolean isOverdueInstallmentCharge() {
        boolean isOverdueInstallmentCharge = false;
        if (this.chargeTimeType != null) {
            isOverdueInstallmentCharge = ChargeTimeType.fromInt(this.chargeTimeType.getId().intValue()).isOverdueInstallment();
        }
        return isOverdueInstallmentCharge;
    }
}