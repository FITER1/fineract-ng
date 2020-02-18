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


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.office.domain.Office;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_cashier_transactions")
public class CashierTransaction extends AbstractPersistableCustom<Long> {

	@Transient
    private Office office;
	
	@Transient
    private Teller teller;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private Cashier cashier;
    
    @Column(name = "txn_type", nullable = false)
    private Integer txnType;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "txn_date", nullable = false)
    private Date txnDate;

    @Column(name = "txn_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal txnAmount;
    
    @Column(name = "txn_note", nullable = true)
    private String txnNote;
    
    @Column(name = "entity_type", nullable = true)
    private String entityType;
    
    @Column(name = "entity_id", nullable = true)
    private Long entityId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false)
    private Date createdDate;
    
    @Column(name = "currency_code", nullable = true)
    private String currencyCode;
}
