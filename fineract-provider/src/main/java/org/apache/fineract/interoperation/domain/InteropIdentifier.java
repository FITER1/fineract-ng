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
package org.apache.fineract.interoperation.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;

import javax.persistence.*;
import java.util.Date;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "interop_identifier", uniqueConstraints = {@UniqueConstraint(name = "uk_hathor_identifier_account", columnNames = {"account_id", "type"}), @UniqueConstraint(name = "uk_hathor_identifier_value", columnNames = {"type", "a_value", "sub_value_or_type"})})
public class InteropIdentifier extends AbstractPersistableCustom<Long> {

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private SavingsAccount account;

    @Column(name = "type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private InteropIdentifierType type;

    @Column(name = "a_value", nullable = false, length = 128)
    private String value;

    @Column(name = "sub_value_or_type", length = 128)
    private String subValueOrType;

    @Column(name = "created_by", nullable = false, length = 32)
    private String createdBy;

    // @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on", nullable = false)
    private Date createdOn;

    @Column(name = "modified_by", length = 32)
    private String modifiedBy;

    // @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified_on")
    private Date modifiedOn;
}
