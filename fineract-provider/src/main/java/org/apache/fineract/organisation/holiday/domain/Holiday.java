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
package org.apache.fineract.organisation.holiday.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.office.domain.Office;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_holiday", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }, name = "holiday_name") })
public class Holiday extends AbstractPersistableCustom<Long> {

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "from_date", nullable = false)
    // @Temporal(TemporalType.DATE)
    private Date fromDate;

    @Column(name = "to_date", nullable = false)
    // @Temporal(TemporalType.DATE)
    private Date toDate;

    @Column(name = "repayments_rescheduled_to")
    // @Temporal(TemporalType.DATE)
    private Date repaymentsRescheduledTo;
    
    @Column(name = "rescheduling_type", nullable = false)
    private Integer reschedulingType;

    @Column(name = "status_enum", nullable = false)
    private Integer status;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "description", length = 100)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "m_holiday_office", joinColumns = @JoinColumn(name = "holiday_id"), inverseJoinColumns = @JoinColumn(name = "office_id"))
    private Set<Office> offices;

    public LocalDate getRepaymentsRescheduledToLocalDate() {
        LocalDate repaymentsRescheduledTo = null;
        if (this.repaymentsRescheduledTo != null) {
            repaymentsRescheduledTo = new LocalDate(this.repaymentsRescheduledTo);
        }
        return repaymentsRescheduledTo;
    }

    public LocalDate getFromDateLocalDate() {
        LocalDate fromDate = null;
        if (this.fromDate != null) {
            fromDate = new LocalDate(this.fromDate);
        }
        return fromDate;
    }

    public LocalDate getToDateLocalDate() {
        LocalDate toDate = null;
        if (this.toDate != null) {
            toDate = new LocalDate(this.toDate);
        }
        return toDate;
    }
}
