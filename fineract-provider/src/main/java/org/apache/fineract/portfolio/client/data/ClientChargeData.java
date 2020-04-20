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
package org.apache.fineract.portfolio.client.data;

import lombok.*;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClientChargeData implements Serializable {
    private Long id;
    private Long clientId;
    private Long chargeId;
    private String name;
    private EnumOptionData chargeTimeType;
    private LocalDate dueDate;
    private EnumOptionData chargeCalculationType;
    private CurrencyData currency;
    private BigDecimal amount;
    private BigDecimal amountPaid;
    private BigDecimal amountWaived;
    private BigDecimal amountWrittenOff;
    private BigDecimal amountOutstanding;
    private Boolean penalty;
    @JsonProperty("is_active")
    private Boolean active;
    @JsonProperty("is_paid")
    private Boolean paid;
    @JsonProperty("is_waived")
    private Boolean waived;
    private LocalDate inactivationDate;
    private Collection<ChargeData> chargeOptions;
    private Collection<ClientTransactionData> clientTransactionDatas;
}
