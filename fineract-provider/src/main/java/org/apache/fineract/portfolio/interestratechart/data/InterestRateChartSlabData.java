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
package org.apache.fineract.portfolio.interestratechart.data;

import lombok.*;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InterestRateChartSlabData {
    private Long id;
    private String description;
    private EnumOptionData periodType;
    private Integer fromPeriod;
    private Integer toPeriod;
    private BigDecimal amountRangeFrom;
    private BigDecimal amountRangeTo;
    private BigDecimal annualInterestRate;
    private CurrencyData currency;
    // associations
    private Set<InterestIncentiveData> incentives;
    // template
    private Collection<EnumOptionData> periodTypes;
    private Collection<EnumOptionData> entityTypeOptions;
    private Collection<EnumOptionData> attributeNameOptions;
    private Collection<EnumOptionData> conditionTypeOptions;
    private Collection<EnumOptionData> incentiveTypeOptions;
    private Collection<CodeValueData> genderOptions;
    private Collection<CodeValueData> clientTypeOptions;
    private Collection<CodeValueData> clientClassificationOptions;

    public void addIncentives(final InterestIncentiveData incentiveData) {
        if (this.incentives == null) {
            this.incentives = new HashSet<>();
        }

        this.incentives.add(incentiveData);
    }
}