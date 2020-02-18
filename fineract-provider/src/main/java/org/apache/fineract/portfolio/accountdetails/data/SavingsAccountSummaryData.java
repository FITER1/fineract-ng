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
package org.apache.fineract.portfolio.accountdetails.data;

import lombok.*;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountApplicationTimelineData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountStatusEnumData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountSubStatusEnumData;
import org.joda.time.LocalDate;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SavingsAccountSummaryData {
    private Long id;
    private String accountNo;
    private String externalId;
    private Long productId;
    private String productName;
    private String shortProductName;
    private SavingsAccountStatusEnumData status;
    private CurrencyData currency;
    private BigDecimal accountBalance;
    //differentiate Individual, JLG or Group account
    private EnumOptionData accountType;
    private SavingsAccountApplicationTimelineData timeline;
    private SavingsAccountSubStatusEnumData subStatus;
    private LocalDate lastActiveTransactionDate;
    //differentiate deposit accounts Savings, FD and RD accounts
    private EnumOptionData depositType;
}