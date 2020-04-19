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
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.portfolio.calendar.CalendarConstants.CALENDAR_SUPPORTED_PARAMETERS;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.apache.fineract.portfolio.common.domain.NthDayType;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.util.*;

import static org.apache.fineract.portfolio.calendar.CalendarConstants.CALENDAR_RESOURCE_NAME;

@SuperBuilder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_calendar")
public class Calendar extends AbstractAuditableCustom<AppUser, Long> {

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
    
    @Column(name="meeting_time")
    // @Temporal(TemporalType.TIME)
    private Date meetingtime;
    
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "calendar_id")
    @Builder.Default
    private Set<CalendarHistory> calendarHistory = new HashSet<>();

    public Calendar(final String title, final String description, final String location, final LocalDate startDate,
            final LocalDate endDate, final Integer duration, final Integer typeId, final boolean repeating, final String recurrence,
            final Integer remindById, final Integer firstReminder, final Integer secondReminder,final Date meetingtime) {

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(CALENDAR_RESOURCE_NAME);

        final CalendarType calendarType = CalendarType.fromInt(typeId);
        if (calendarType.isCollection() && !repeating) {
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue())
                    .failWithCodeNoParameterAddedToErrorCode("must.repeat.for.collection.calendar");
            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
        }

        this.title = StringUtils.defaultIfEmpty(title, null);
        this.description = StringUtils.defaultIfEmpty(description, null);
        this.location = StringUtils.defaultIfEmpty(location, null);

        if (null != startDate) {
            this.startDate = startDate.toDateTimeAtStartOfDay().toDate();
        } else {
            this.startDate = null;
        }

        if (null != endDate) {
            this.endDate = endDate.toDateTimeAtStartOfDay().toDate();
        } else {
            this.endDate = null;
        }

        this.duration = duration;
        this.typeId = typeId;
        this.repeating = repeating;
        this.recurrence = StringUtils.defaultIfEmpty(recurrence, null);
        this.remindById = remindById;
        this.firstReminder = firstReminder;
        this.secondReminder = secondReminder;
        this.meetingtime=meetingtime;
    }

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

    public boolean isStartDateBeforeOrEqual(final LocalDate compareDate) {
        if (this.startDate != null && compareDate != null) {
            if (getStartDateLocalDate().isBefore(compareDate) || getStartDateLocalDate().equals(compareDate)) { return true; }
        }
        return false;
    }

    public boolean isStartDateAfter(final LocalDate compareDate) {
        if (this.startDate != null && compareDate != null && getStartDateLocalDate().isAfter(compareDate)) { return true; }
        return false;
    }

    public boolean isEndDateAfterOrEqual(final LocalDate compareDate) {
        if (this.endDate != null && compareDate != null) {
            if (getEndDateLocalDate().isAfter(compareDate) || getEndDateLocalDate().isEqual(compareDate)) { return true; }
        }
        return false;
    }

    public boolean isBetweenStartAndEndDate(final LocalDate compareDate) {
        if (isStartDateBeforeOrEqual(compareDate)) {
            if (getEndDateLocalDate() == null || isEndDateAfterOrEqual(compareDate)) { return true; }
        }
        return false;
    }

    public static String toRecurrence(final JsonCommand command, final Calendar calendar) {
        final boolean repeating;
        if (command.parameterExists(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue())) {
            repeating = command.booleanPrimitiveValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue());
        } else if (calendar != null) {
            repeating = calendar.isRepeating();
        } else {
            repeating = false;
        }

        if (repeating) {
            final Integer frequency = command.integerValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.FREQUENCY.getValue());
            final CalendarFrequencyType frequencyType = CalendarFrequencyType.fromInt(frequency);
            final Integer interval = command.integerValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.INTERVAL.getValue());
            Integer repeatsOnDay = null;
            if (frequencyType.isWeekly()) {
                repeatsOnDay = command.integerValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_DAY.getValue());
            }
            Integer repeatsOnNthDayOfMonth = null;
            if (frequencyType.isMonthly()) {
                repeatsOnNthDayOfMonth = command.integerValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_NTH_DAY_OF_MONTH
                        .getValue());
                final NthDayType nthDay = NthDayType.fromInt(repeatsOnNthDayOfMonth);
                repeatsOnDay = command.integerValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_LAST_WEEKDAY_OF_MONTH
                        .getValue());
                if (nthDay.isOnDay()) {
                    repeatsOnNthDayOfMonth = command.integerValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.REPEATS_ON_DAY_OF_MONTH
                            .getValue());
                    repeatsOnDay = null;
                }
            }

            return toRecurrence(frequencyType, interval, repeatsOnDay, repeatsOnNthDayOfMonth);
        }
        return "";
    }

    public static String toRecurrence(final CalendarFrequencyType frequencyType, final Integer interval,
                                      final Integer repeatsOnDay, final Integer repeatsOnNthDayOfMonth) {
        final StringBuilder recurrenceBuilder = new StringBuilder(200);

        recurrenceBuilder.append("FREQ=");
        recurrenceBuilder.append(frequencyType.toString().toUpperCase());
        if (interval > 1) {
            recurrenceBuilder.append(";INTERVAL=");
            recurrenceBuilder.append(interval);
        }
        if (frequencyType.isWeekly()) {
            if (repeatsOnDay != null) {
                final CalendarWeekDaysType weekDays = CalendarWeekDaysType.fromInt(repeatsOnDay);
                if (!weekDays.isInvalid()) {
                    recurrenceBuilder.append(";BYDAY=");
                    recurrenceBuilder.append(weekDays.toString().toUpperCase());
                }
            }
        }
        if (frequencyType.isMonthly()) {
            if (repeatsOnNthDayOfMonth != null && (repeatsOnDay == null || repeatsOnDay == CalendarWeekDaysType.INVALID.getValue())) {
                if (repeatsOnNthDayOfMonth >= -1 && repeatsOnNthDayOfMonth <= 28) {
                    recurrenceBuilder.append(";BYMONTHDAY=");
                    recurrenceBuilder.append(repeatsOnNthDayOfMonth);
                }
            } else if (repeatsOnNthDayOfMonth != null && repeatsOnDay != null && repeatsOnDay != CalendarWeekDaysType.INVALID.getValue()) {
                final NthDayType nthDay = NthDayType.fromInt(repeatsOnNthDayOfMonth);
                if (!nthDay.isInvalid()) {
                    recurrenceBuilder.append(";BYSETPOS=");
                    recurrenceBuilder.append(nthDay.getValue());
                }
                final CalendarWeekDaysType weekday = CalendarWeekDaysType.fromInt(repeatsOnDay);
                if (!weekday.isInvalid()) {
                    recurrenceBuilder.append(";BYDAY=");
                    recurrenceBuilder.append(weekday.toString().toUpperCase());
                }
            }
        }
        return recurrenceBuilder.toString();
    }

    public boolean isValidRecurringDate(final LocalDate compareDate, Boolean isSkipRepaymentOnFirstMonth, Integer numberOfDays) {

        if (isBetweenStartAndEndDate(compareDate)) { return CalendarUtils.isValidRedurringDate(getRecurrence(), getStartDateLocalDate(),
                compareDate, isSkipRepaymentOnFirstMonth, numberOfDays); }

        // validate with history details.
        for (CalendarHistory history : getCalendarHistory()) {
            if (history.isBetweenStartAndEndDate(compareDate)) {
                return CalendarUtils.isValidRedurringDate(history.getRecurrence(), history.getStartDateLocalDate(), compareDate, isSkipRepaymentOnFirstMonth, numberOfDays);
            }
        }

        return false;
    }
}
