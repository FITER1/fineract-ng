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
package org.apache.fineract.portfolio.calendar.data;

import lombok.*;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.calendar.domain.CalendarFrequencyType;
import org.apache.fineract.portfolio.calendar.domain.CalendarRemindBy;
import org.apache.fineract.portfolio.calendar.domain.CalendarType;
import org.apache.fineract.portfolio.calendar.domain.CalendarWeekDaysType;
import org.apache.fineract.portfolio.calendar.service.CalendarEnumerations;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.apache.fineract.portfolio.common.domain.NthDayType;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.Collection;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CalendarData {
    private Long id;
    private Long calendarInstanceId;
    private Long entityId;
    private EnumOptionData entityType;
    private String title;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime meetingTime;
    @Builder.Default
    private Integer duration = 0;
    @Builder.Default
    private EnumOptionData type = CalendarEnumerations.calendarType(CalendarType.COLLECTION);
    private boolean repeating;
    private String recurrence;
    @Builder.Default
    private EnumOptionData frequency = CalendarEnumerations.calendarFrequencyType(CalendarFrequencyType.DAILY);
    @Builder.Default
    private Integer interval = 1;
    @Builder.Default
    private EnumOptionData repeatsOnDay = CalendarEnumerations.calendarWeekDaysType(CalendarWeekDaysType.MO);
    @Builder.Default
    private EnumOptionData repeatsOnNthDayOfMonth = CalendarEnumerations.calendarFrequencyNthDayType(NthDayType.ONE);
    @Builder.Default
    private EnumOptionData remindBy = CalendarEnumerations.calendarRemindBy(CalendarRemindBy.EMAIL);
    @Builder.Default
    private Integer firstReminder = 0;
    @Builder.Default
    private Integer secondReminder = 0;
    private Collection<LocalDate> recurringDates;
    private Collection<LocalDate> nextTenRecurringDates;
    private String humanReadable;
    private LocalDate recentEligibleMeetingDate;
    private LocalDate createdDate;
    private LocalDate lastUpdatedDate;
    private Long createdByUserId;
    private String createdByUsername;
    private Long lastUpdatedByUserId;
    private String lastUpdatedByUsername;
    private Integer repeatsOnDayOfMonth;
    // template related
    private List<EnumOptionData> entityTypeOptions;
    private List<EnumOptionData> calendarTypeOptions;
    private List<EnumOptionData> remindByOptions;
    private List<EnumOptionData> frequencyOptions;
    private List<EnumOptionData> repeatsOnDayOptions;
    private List<EnumOptionData> frequencyNthDayTypeOptions;
    //import fields
    private Integer rowIndex;
    private String dateFormat;
    private String locale;
    private String centerId;
    private String typeId;

    public boolean isStartDateBeforeOrEqual(final LocalDate compareDate) {
        if (this.startDate != null && compareDate != null) {
            if (this.startDate.isBefore(compareDate) || this.startDate.equals(compareDate)) { return true; }
        }
        return false;
    }

    public boolean isEndDateAfterOrEqual(final LocalDate compareDate) {
        if (this.endDate != null && compareDate != null) {
            if (this.endDate.isAfter(compareDate) || this.endDate.isEqual(compareDate)) { return true; }
        }
        return false;
    }

    public boolean isBetweenStartAndEndDate(final LocalDate compareDate) {
        if (isStartDateBeforeOrEqual(compareDate)) {
            if (this.endDate == null || isEndDateAfterOrEqual(compareDate)) { return true; }
        }
        return false;
    }

    public boolean isValidRecurringDate(final LocalDate compareDate, final Boolean isSkipMeetingOnFirstDay, final Integer numberOfDays) {
        if (isBetweenStartAndEndDate(compareDate)) { return CalendarUtils.isValidRedurringDate(this.getRecurrence(), this.getStartDate(),
                compareDate, isSkipMeetingOnFirstDay, numberOfDays); }
        return false;
    }
}
