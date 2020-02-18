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
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.Collection;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AccountTransferData {
    private Long id;
    private Boolean reversed;
    private CurrencyData currency;
    private BigDecimal transferAmount;
    private LocalDate transferDate;
    private String transferDescription;
    private OfficeData fromOffice;
    private ClientData fromClient;
    private EnumOptionData fromAccountType;
    private PortfolioAccountData fromAccount;
    private OfficeData toOffice;
    private ClientData toClient;
    private EnumOptionData toAccountType;
    private PortfolioAccountData toAccount;
    // template
    private Collection<OfficeData> fromOfficeOptions;
    private Collection<ClientData> fromClientOptions;
    private Collection<EnumOptionData> fromAccountTypeOptions;
    private Collection<PortfolioAccountData> fromAccountOptions;
    private Collection<OfficeData> toOfficeOptions;
    private Collection<ClientData> toClientOptions;
    private Collection<EnumOptionData> toAccountTypeOptions;
    private Collection<PortfolioAccountData> toAccountOptions;
}