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
package org.apache.fineract.organisation.teller.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.portfolio.client.domain.Client;

import javax.persistence.*;
import java.util.Date;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_teller_transactions")
public class TellerTransaction extends AbstractPersistableCustom<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teller_id", nullable = false)
    private Teller teller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private Cashier cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "type", nullable = false)
    private Integer type;

    @Column(name = "amount", nullable = false)
    private Double amount;

    // @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "posting_date", nullable = false)
    private Date postingDate;
}
