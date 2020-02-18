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
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_office", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"}, name = "name_org"), @UniqueConstraint(columnNames = {"external_id"}, name = "externalid_org")})
public class Office extends AbstractPersistableCustom<Long> {

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private List<Office> children = new LinkedList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Office parent;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "hierarchy", nullable = true, length = 50)
    private String hierarchy;

    @Column(name = "opening_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date openingDate;

    @Column(name = "external_id", length = 100)
    private String externalId;

    public LocalDate getOpeningLocalDate() {
        LocalDate openingLocalDate = null;
        if (this.openingDate != null) {
            openingLocalDate = LocalDate.fromDateFields(this.openingDate);
        }
        return openingLocalDate;
    }

    public void generateHierarchy() {

        if (this.parent != null) {
            this.hierarchy = this.parent.hierarchyOf(getId());
        } else {
            this.hierarchy = ".";
        }
    }

    private String hierarchyOf(final Long id) {
        return this.hierarchy + id + ".";
    }

    public boolean doesNotHaveAnOfficeInHierarchyWithId(final Long officeId) {
        return !hasAnOfficeInHierarchyWithId(officeId);
    }

    private boolean hasAnOfficeInHierarchyWithId(final Long officeId) {

        boolean match = false;

        if (getId().equals(officeId)) {
            match = true;
        }

        if (!match) {
            for (final Office child : this.children) {
                final boolean result = child.hasAnOfficeInHierarchyWithId(officeId);

                if (result) {
                    match = result;
                    break;
                }
            }
        }

        return match;
    }
}