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
package org.apache.fineract.organisation.office.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Represents currencies allowed for this MFI/organisation.
 */
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_organisation_currency")
public class OrganisationCurrency extends AbstractPersistableCustom<Long> {

    @Column(name = "code", nullable = false, length = 3)
    private String code;

    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces;

    @Column(name = "currency_multiplesof")
    private Integer inMultiplesOf;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "internationalized_name_code", nullable = false, length = 50)
    private String nameCode;

    @Column(name = "display_symbol", length = 10)
    private String displaySymbol;
}