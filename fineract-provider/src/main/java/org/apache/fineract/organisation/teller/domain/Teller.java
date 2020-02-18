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
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.office.domain.Office;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_tellers", uniqueConstraints = {
        @UniqueConstraint(name = "ux_tellers_name", columnNames = {"name"})
})
public class Teller extends AbstractPersistableCustom<Long> {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "debit_account_id", nullable = true)
    private GLAccount debitAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "credit_account_id", nullable = true)
    private GLAccount creditAccount;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", nullable = true, length = 500)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "valid_from", nullable = true)
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "valid_to", nullable = true)
    private Date endDate;

    @Column(name = "state", nullable = false)
    private Integer status;

    @OneToMany(mappedBy = "teller", fetch = FetchType.LAZY)
    private Set<Cashier> cashiers;
}
