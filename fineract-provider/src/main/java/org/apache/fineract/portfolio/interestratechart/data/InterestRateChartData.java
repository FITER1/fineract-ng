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
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InterestRateChartData {
    private Long id;
    private String name;
    private String description;
    private LocalDate fromDate;
    private LocalDate endDate;
    private Long productId;
    private String productName;
    private boolean primaryGroupingByAmount;
    // associations
    private Collection<InterestRateChartSlabData> chartSlabs;
    // template
    private Collection<EnumOptionData> periodTypes;
    private Collection<EnumOptionData> entityTypeOptions;
    private Collection<EnumOptionData> attributeNameOptions;
    private Collection<EnumOptionData> conditionTypeOptions;
    private Collection<EnumOptionData> incentiveTypeOptions;
    private Collection<CodeValueData> genderOptions;
    private Collection<CodeValueData> clientTypeOptions;
    private Collection<CodeValueData> clientClassificationOptions;

    public void addChartSlab(final InterestRateChartSlabData chartSlab) {
        if (this.chartSlabs == null) {
            this.chartSlabs = new ArrayList<>();
        }

        this.chartSlabs.add(chartSlab);
    }

    public boolean isFromDateAfter(final LocalDate compareDate) {
        return compareDate!=null && this.fromDate.isAfter(compareDate);
    }
}