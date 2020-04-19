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
import net.minidev.json.annotate.JsonIgnore;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
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
public class ChargeData implements Serializable {
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
    private List<EnumOptionData> chargeCalculationTypeOptions;
    private List<EnumOptionData> chargeAppliesToOptions;
    private List<EnumOptionData> chargeTimeTypeOptions;
    private List<EnumOptionData> chargePaymetModeOptions;
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

    @JsonIgnore
    public boolean isOverdueInstallmentCharge() {
        boolean isOverdueInstallmentCharge = false;
        if (this.chargeTimeType != null) {
            isOverdueInstallmentCharge = ChargeTimeType.fromInt(this.chargeTimeType.getId().intValue()).isOverdueInstallment();
        }
        return isOverdueInstallmentCharge;
    }
}