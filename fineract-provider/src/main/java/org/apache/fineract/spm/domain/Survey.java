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
package org.apache.fineract.spm.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_surveys")
public class Survey extends AbstractPersistableCustom<Long> {

    @OneToMany(mappedBy = "survey", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval=true)
    @OrderBy("sequenceNo")
    private List<Component> components;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval=true)
    @LazyCollection(LazyCollectionOption.FALSE)
    @OrderBy("sequenceNo")
    private List<Question> questions;

    @Column(name = "a_key", length = 32)
    private String key;

    @Column(name = "a_name", length = 255)
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "valid_from")
    // @Temporal(value = TemporalType.TIMESTAMP)
    private Date validFrom;

    @Column(name = "valid_to")
    // @Temporal(value = TemporalType.TIMESTAMP)
    private Date validTo;
}
