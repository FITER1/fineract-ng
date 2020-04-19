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
package org.apache.fineract.portfolio.calendar.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.util.Date;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_calendar_history")
public class CalendarHistory extends AbstractPersistableCustom<Long> {

    @ManyToOne(optional = false)
    @JoinColumn(name = "calendar_id", referencedColumnName = "id", nullable = false)
    private Calendar calendar;

    @Column(name = "title", length = 50, nullable = false)
    private String title;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "start_date", nullable = false)
    // @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(name = "end_date")
    // @Temporal(TemporalType.DATE)
    private Date endDate;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "calendar_type_enum", nullable = false)
    private Integer typeId;

    @Column(name = "repeating", nullable = false)
    private boolean repeating;

    @Column(name = "recurrence", length = 100)
    private String recurrence;

    @Column(name = "remind_by_enum")
    private Integer remindById;

    @Column(name = "first_reminder")
    private Integer firstReminder;

    @Column(name = "second_reminder")
    private Integer secondReminder;

    public LocalDate getStartDateLocalDate() {
        LocalDate startDateLocalDate = null;
        if (this.startDate != null) {
            startDateLocalDate = LocalDate.fromDateFields(this.startDate);
        }
        return startDateLocalDate;
    }

    public LocalDate getEndDateLocalDate() {
        LocalDate endDateLocalDate = null;
        if (this.endDate != null) {
            endDateLocalDate = LocalDate.fromDateFields(this.endDate);
        }
        return endDateLocalDate;
    }

    public boolean isBetweenStartAndEndDate(final LocalDate compareDate) {
        if (isStartDateBeforeOrEqual(compareDate)) {
            return getEndDateLocalDate() == null || isEndDateAfterOrEqual(compareDate);
        }
        return false;
    }

    private boolean isEndDateAfterOrEqual(final LocalDate compareDate) {
        if (this.endDate != null && compareDate != null) {
            return getEndDateLocalDate().isAfter(compareDate) || getEndDateLocalDate().isEqual(compareDate);
        }
        return false;
    }

    private boolean isStartDateBeforeOrEqual(final LocalDate compareDate) {
        if (this.startDate != null && compareDate != null) {
            return getStartDateLocalDate().isBefore(compareDate) || getStartDateLocalDate().equals(compareDate);
        }
        return false;
    }
}
