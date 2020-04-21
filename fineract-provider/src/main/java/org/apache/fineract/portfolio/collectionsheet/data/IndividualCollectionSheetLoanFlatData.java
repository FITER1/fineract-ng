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
package org.apache.fineract.portfolio.collectionsheet.data;

import lombok.*;
import org.apache.fineract.organisation.monetary.data.CurrencyData;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IndividualCollectionSheetLoanFlatData implements Serializable {
    private String clientName;
    private Long clientId;
    private Long loanId;
    private String accountId;
    private Integer accountStatusId;
    private String productShortName;
    private Long productId;
    private CurrencyData currency;
    private BigDecimal disbursementAmount = BigDecimal.ZERO;
    private BigDecimal principalDue = BigDecimal.ZERO;
    private BigDecimal principalPaid = BigDecimal.ZERO;
    private BigDecimal interestDue = BigDecimal.ZERO;
    private BigDecimal interestPaid = BigDecimal.ZERO;
    private BigDecimal chargesDue = BigDecimal.ZERO;
    private BigDecimal feeDue = BigDecimal.ZERO;
    private BigDecimal feePaid = BigDecimal.ZERO;
}