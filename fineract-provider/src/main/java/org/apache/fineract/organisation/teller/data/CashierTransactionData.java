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
package org.apache.fineract.organisation.teller.data;

import lombok.*;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.teller.domain.CashierTxnType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class CashierTransactionData implements Serializable {
    private Long id;
    private Long cashierId;
    private CashierTxnType txnType;
    private BigDecimal txnAmount;
    private Date txnDate;
    private Long entityId;
    private String entityType;
    private String txnNote;
    private Date createdDate;
    // Template fields
    private Long officeId;
    private String officeName;
    private Long tellerId;
    private String tellerName;
    private String cashierName;
    private CashierData cashierData;
    private Date startDate;
    private Date endDate;
    private Collection<CurrencyData> currencyOptions;
}
