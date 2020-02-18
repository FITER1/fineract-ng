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
package org.apache.fineract.organisation.office.data;

import lombok.*;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.Collection;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OfficeTransactionData {
    private Long id;
    private LocalDate transactionDate;
    private Long fromOfficeId;
    private String fromOfficeName;
    private Long toOfficeId;
    private String toOfficeName;
    private CurrencyData currency;
    private BigDecimal transactionAmount;
    private String description;
    private Collection<CurrencyData> currencyOptions;
    private Collection<OfficeData> allowedOffices;
}