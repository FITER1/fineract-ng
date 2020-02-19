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
package org.apache.fineract.portfolio.calendar.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.calendar.CalendarConstants.CALENDAR_SUPPORTED_PARAMETERS;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.*;
import org.apache.fineract.portfolio.calendar.exception.CalendarDateException;
import org.apache.fineract.portfolio.calendar.exception.CalendarNotFoundException;
import org.apache.fineract.portfolio.calendar.exception.CalendarParameterUpdateNotSupportedException;
import org.apache.fineract.portfolio.calendar.serialization.CalendarCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static org.apache.fineract.portfolio.calendar.CalendarConstants.CALENDAR_RESOURCE_NAME;

@Service
@RequiredArgsConstructor
public class CalendarWritePlatformServiceJpaRepositoryImpl implements CalendarWritePlatformService {

    private final CalendarRepository calendarRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;
    private final CalendarCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final LoanWritePlatformService loanWritePlatformService;
    private final ConfigurationDomainService configurationDomainService;
    private final GroupRepositoryWrapper groupRepository;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final ClientRepositoryWrapper clientRepository;

    @Override
    public CommandProcessingResult createCalendar(final JsonCommand command) {

        this.fromApiJsonDeserializer.validateForCreate(command.json());
        Long entityId = null;
        CalendarEntityType entityType = CalendarEntityType.INVALID;
        LocalDate entityActivationDate = null;
        Group centerOrGroup = null;
        if (command.getGroupId() != null) {
            centerOrGroup = this.groupRepository.findOneWithNotFoundDetection(command.getGroupId());
            entityActivationDate = centerOrGroup.getActivationLocalDate();
            entityType = centerOrGroup.isCenter() ? CalendarEntityType.CENTERS : CalendarEntityType.GROUPS;
            entityId = command.getGroupId();
        } else if (command.getLoanId() != null) {
            final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(command.getLoanId(), true);
            entityActivationDate = (loan.getApprovedOnDate() == null) ? loan.getSubmittedOnDate() : loan.getApprovedOnDate();
            entityType = CalendarEntityType.LOANS;
            entityId = command.getLoanId();
        } else if (command.getClientId() != null) {
            final Client client = this.clientRepository.findOneWithNotFoundDetection(command.getClientId());
            entityActivationDate = client.getActivationLocalDate();
            entityType = CalendarEntityType.CLIENTS;
            entityId = command.getClientId();
        }

        final Integer entityTypeId = entityType.getValue();
        final Calendar newCalendar = fromJson(command);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("calendar");
        if (entityActivationDate == null || newCalendar.getStartDateLocalDate().isBefore(entityActivationDate)) {
            final DateTimeFormatter formatter = DateTimeFormat.forPattern(command.dateFormat()).withLocale(command.extractLocale());
            String dateAsString = "";
            if (entityActivationDate != null) dateAsString = formatter.print(entityActivationDate);

            final String errorMessage = "cannot.be.before." + entityType.name().toLowerCase() + ".activation.date";
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue()).value(dateAsString)
                    .failWithCodeNoParameterAddedToErrorCode(errorMessage);
        }

        if (centerOrGroup != null) {
            Long centerOrGroupId = centerOrGroup.getId();
            Integer centerOrGroupEntityTypeId = entityType.getValue();

            final Group parent = centerOrGroup.getParent();
            if (parent != null) {
                centerOrGroupId = parent.getId();
                centerOrGroupEntityTypeId = CalendarEntityType.CENTERS.getValue();
            }

            final CalendarInstance collectionCalendarInstance = this.calendarInstanceRepository
                    .findByEntityIdAndEntityTypeIdAndCalendarTypeId(centerOrGroupId, centerOrGroupEntityTypeId,
                            CalendarType.COLLECTION.getValue());
            if (collectionCalendarInstance != null) {
                final String errorMessage = "multiple.collection.calendar.not.supported";
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode(errorMessage);
            }
        }

        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }

        this.calendarRepository.save(newCalendar);

        final CalendarInstance newCalendarInstance = new CalendarInstance(newCalendar, entityId, entityTypeId);
        this.calendarInstanceRepository.save(newCalendarInstance);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(newCalendar.getId()) //
                .clientId(command.getClientId()) //
                .groupId(command.getGroupId()) //
                .loanId(command.getLoanId()) //
                .build();

    }


	public void validateIsEditMeetingAllowed(Long groupId) {

		final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
		final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(
				dataValidationErrors).resource("calendar");
		Group centerOrGroup = null;

		if (groupId != null) {
			centerOrGroup = this.groupRepository
					.findOneWithNotFoundDetection(groupId);
			final Group parent = centerOrGroup.getParent();
			/* Check if it is a Group and belongs to a center */
			if (centerOrGroup.isGroup() && parent != null) {
				
				Integer centerEntityTypeId = CalendarEntityType.CENTERS
						.getValue();
				/* Check if calendar is created at center */
				final CalendarInstance collectionCalendarInstance = this.calendarInstanceRepository
						.findByEntityIdAndEntityTypeIdAndCalendarTypeId(
								parent.getId(), centerEntityTypeId,
								CalendarType.COLLECTION.getValue());
				/* If calendar is created by parent group, then it cannot be edited by the child group */
				if (collectionCalendarInstance != null) {
					final String errorMessage = "meeting.created.at.center.cannot.be.edited.at.group.level";
					baseDataValidator.reset()
							.failWithCodeNoParameterAddedToErrorCode(
									errorMessage);
				}
			}

		}
		if (!dataValidationErrors.isEmpty()) {
			throw new PlatformApiDataValidationException(
					"validation.msg.validation.errors.exist",
					"Validation errors exist.", dataValidationErrors);
		}

	}
    
    @Override
    public CommandProcessingResult updateCalendar(final JsonCommand command) {

    	/** Validate to check if Edit is Allowed **/
    	this.validateIsEditMeetingAllowed(command.getGroupId());
        /*
         * Validate all the data for updating the calendar
         */
        this.fromApiJsonDeserializer.validateForUpdate(command.json());
        
        Boolean areActiveEntitiesSynced = false;
        final Long calendarId = command.entityId();

        final Collection<Integer> loanStatuses = new ArrayList<>(Arrays.asList(LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(),
                LoanStatus.APPROVED.getValue(), LoanStatus.ACTIVE.getValue()));

        final Integer numberOfActiveLoansSyncedWithThisCalendar = this.calendarInstanceRepository.countOfLoansSyncedWithCalendar(
                calendarId, loanStatuses);

        /*
         * areActiveEntitiesSynced is set to true, if there are any active loans
         * synced to this calendar.
         */
        
        if(numberOfActiveLoansSyncedWithThisCalendar > 0){
            areActiveEntitiesSynced = true;
        }

        
        final Calendar calendarForUpdate = this.calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CalendarNotFoundException(calendarId));

        final Date oldStartDate = calendarForUpdate.getStartDate();
        // create calendar history before updating calendar
        final CalendarHistory calendarHistory = CalendarHistory.builder()
            .calendar(calendarForUpdate)
            .startDate(oldStartDate)
            .title(calendarForUpdate.getTitle())
            .description(calendarForUpdate.getDescription())
            .location(calendarForUpdate.getLocation())
            .endDate(calendarForUpdate.getStartDate())
            .duration(calendarForUpdate.getDuration())
            .typeId(calendarForUpdate.getTypeId())
            .repeating(calendarForUpdate.isRepeating())
            .recurrence(calendarForUpdate.getRecurrence())
            .remindById(calendarForUpdate.getRemindById())
            .firstReminder(calendarForUpdate.getFirstReminder())
            .secondReminder(calendarForUpdate.getSecondReminder())
            .build();

        Map<String, Object> changes = null;
        
        final Boolean reschedulebasedOnMeetingDates = command
                .booleanObjectValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.RESCHEDULE_BASED_ON_MEETING_DATES.getValue());
        
        /*
         * System allows to change the meeting date by two means,
         * 
         * Option 1: reschedulebasedOnMeetingDates = false or reschedulebasedOnMeetingDates is not passed 
         * By directly editing the recurring day with effective from
         * date and system decides the next meeting date based on some sensible
         * logic (i.e., number of minimum days between two repayments)
         * 
         * 
         * Option 2: reschedulebasedOnMeetingDates = true 
         * By providing alternative meeting date for one of future
         * meeting date and derive the day of recurrence from the new meeting
         * date. Ex: User proposes new meeting date say "14/Nov/2014" for
         * present meeting date "12/Nov/2014", based on this input other values
         * re derived and loans are rescheduled
         * 
         */
        
        LocalDate newMeetingDate = null;
        LocalDate presentMeetingDate = null;
        
        if (reschedulebasedOnMeetingDates != null && reschedulebasedOnMeetingDates) {
            
            newMeetingDate = command.localDateValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.NEW_MEETING_DATE.getValue());
            presentMeetingDate = command.localDateValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.PRESENT_MEETING_DATE.getValue());

            /*
             * New meeting date proposed will become the new start date for the
             * updated calendar
             */

            changes = updateStartDateAndDerivedFeilds(calendarForUpdate, newMeetingDate);

        } else {
            changes = update(calendarForUpdate, command, areActiveEntitiesSynced);
        }
        
        if (!changes.isEmpty()) {
            // update calendar history table only if there is a change in
            // calendar start date.
            if (reschedulebasedOnMeetingDates == null){
            presentMeetingDate = command.localDateValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue());
            }
            if (null != newMeetingDate) {
                final Date endDate = presentMeetingDate.minusDays(1).toDate();
                calendarHistory.setEndDate(endDate);
            }
            this.calendarHistoryRepository.save(calendarHistory);
            Set<CalendarHistory> history = calendarForUpdate.getCalendarHistory();
            history.add(calendarHistory);
            calendarForUpdate.setCalendarHistory(history);
            this.calendarRepository.saveAndFlush(calendarForUpdate);

            if (this.configurationDomainService.isRescheduleFutureRepaymentsEnabled() && calendarForUpdate.isRepeating()) {
                // fetch all loan calendar instances associated with modifying
                // calendar.
                final Collection<CalendarInstance> loanCalendarInstances = this.calendarInstanceRepository.findByCalendarIdAndEntityTypeId(
                        calendarId, CalendarEntityType.LOANS.getValue());

                if (!CollectionUtils.isEmpty(loanCalendarInstances)) {
                    // update all loans associated with modifying calendar
                    this.loanWritePlatformService.applyMeetingDateChanges(calendarForUpdate, loanCalendarInstances,
                            reschedulebasedOnMeetingDates, presentMeetingDate, newMeetingDate);

                }
            }
        }

        return CommandProcessingResult.builder()
                .commandId(command.commandId())
                .resourceId(calendarForUpdate.getId())
                .changes(changes)
                .build();
    }

    @Override
    public CommandProcessingResult deleteCalendar(final Long calendarId) {
        final Calendar calendarForDelete = this.calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CalendarNotFoundException(calendarId));

        this.calendarRepository.delete(calendarForDelete);
        return CommandProcessingResult.builder()
                .commandId(null)
                .resourceId(calendarId)
                .build();
    }

    @Override
    public CommandProcessingResult createCalendarInstance(final Long calendarId, final Long entityId, final Integer entityTypeId) {
        final Calendar calendarForUpdate = this.calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CalendarNotFoundException(calendarId));

        final CalendarInstance newCalendarInstance = new CalendarInstance(calendarForUpdate, entityId, entityTypeId);
        this.calendarInstanceRepository.save(newCalendarInstance);

        return CommandProcessingResult.builder()
                .commandId(null)
                .resourceId(calendarForUpdate.getId())
                .build();
    }

    @Override
    public CommandProcessingResult updateCalendarInstance(final Long calendarId, final Long entityId, final Integer entityTypeId) {
        final Calendar calendarForUpdate = this.calendarRepository.findById(calendarId)
                .orElseThrow(() -> new CalendarNotFoundException(calendarId));

        final CalendarInstance calendarInstanceForUpdate = this.calendarInstanceRepository.findByCalendarIdAndEntityIdAndEntityTypeId(
                calendarId, entityId, entityTypeId);
        this.calendarInstanceRepository.saveAndFlush(calendarInstanceForUpdate);
        return CommandProcessingResult.builder()
                .commandId(null)
                .resourceId(calendarForUpdate.getId())
                .build();
    }

    private Map<String, Object> updateStartDateAndDerivedFeilds(final Calendar calendar, final LocalDate newMeetingStartDate) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        final LocalDate currentDate = DateUtils.getLocalDateOfTenant();

        if (newMeetingStartDate.isBefore(currentDate)) {
            final String defaultUserMessage = "New meeting effective from date cannot be in past";
            throw new CalendarDateException("new.start.date.cannot.be.in.past", defaultUserMessage, newMeetingStartDate, calendar.getStartDateLocalDate());
        } else if (calendar.isStartDateAfter(newMeetingStartDate) && calendar.isStartDateBeforeOrEqual(currentDate)) {
            // new meeting date should be on or after start date or current
            // date
            final String defaultUserMessage = "New meeting effective from date cannot be a date before existing meeting start date";
            throw new CalendarDateException("new.start.date.before.existing.date", defaultUserMessage, newMeetingStartDate, calendar.getStartDateLocalDate());
        } else {

            actualChanges.put(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue(), newMeetingStartDate.toString());
            calendar.setStartDate(newMeetingStartDate.toDate());

            /*
             * If meeting start date is changed then there is possibilities of
             * recurring day may change, so derive the recurring day and update
             * it if it is changed. For weekly type is weekday and for monthly
             * type it is day of the month
             */

            CalendarFrequencyType calendarFrequencyType = CalendarUtils.getFrequency(calendar.getRecurrence());
            Integer interval = CalendarUtils.getInterval(calendar.getRecurrence());
            Integer repeatsOnDay = null;

            /*
             * Repeats on day, need to derive based on the start date
             */

            if (calendarFrequencyType.isWeekly()) {
                repeatsOnDay = newMeetingStartDate.getDayOfWeek();
            } else if (calendarFrequencyType.isMonthly()) {
                repeatsOnDay = newMeetingStartDate.getDayOfMonth();
            }

            // TODO cover other recurrence also

            calendar.setRecurrence(Calendar.toRecurrence(calendarFrequencyType, interval, repeatsOnDay, null));
        }

        return actualChanges;

    }

    private static Calendar fromJson(final JsonCommand command) {

        // final Long entityId = command.getSupportedEntityId();
        // final Integer entityTypeId =
        // CalendarEntityType.valueOf(command.getSupportedEntityType().toUpperCase()).getValue();
        Date meetingtime=null;
        final String title = command.stringValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue());
        final String description = command.stringValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue());
        final String location = command.stringValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue());
        final LocalDate startDate = command.localDateValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue());
        final LocalDate endDate = command.localDateValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue());
        final Integer duration = command.integerValueSansLocaleOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.DURATION.getValue());
        final Integer typeId = command.integerValueSansLocaleOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.TYPE_ID.getValue());
        final boolean repeating = command.booleanPrimitiveValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue());
        final Integer remindById = command.integerValueSansLocaleOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.REMIND_BY_ID.getValue());
        final Integer firstReminder = command.integerValueSansLocaleOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.FIRST_REMINDER
            .getValue());
        final Integer secondReminder = command.integerValueSansLocaleOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.SECOND_REMINDER
            .getValue());
        final LocalDateTime time= command.localTimeValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue());
        if(time!=null){
            meetingtime=time.toDate();
        }
        final String recurrence = Calendar.toRecurrence(command, null);

        return new Calendar(title, description, location, startDate, endDate, duration, typeId, repeating, recurrence, remindById,
            firstReminder, secondReminder,meetingtime);
    }

    private Map<String, Object> update(final Calendar calendar, final JsonCommand command, final Boolean areActiveEntitiesSynced) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        if (command.isChangeInStringParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue(), calendar.getTitle())) {
            final String newValue = command.stringValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue());
            actualChanges.put(CALENDAR_SUPPORTED_PARAMETERS.TITLE.getValue(), newValue);
            calendar.setTitle(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue(), calendar.getDescription())) {
            final String newValue = command.stringValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue());
            actualChanges.put(CALENDAR_SUPPORTED_PARAMETERS.DESCRIPTION.getValue(), newValue);
            calendar.setDescription(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue(), calendar.getLocation())) {
            final String newValue = command.stringValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue());
            actualChanges.put(CALENDAR_SUPPORTED_PARAMETERS.LOCATION.getValue(), newValue);
            calendar.setLocation(StringUtils.defaultIfEmpty(newValue, null));
        }

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();
        final String startDateParamName = CALENDAR_SUPPORTED_PARAMETERS.START_DATE.getValue();
        if (command.isChangeInLocalDateParameterNamed(startDateParamName, calendar.getStartDateLocalDate())) {

            final String valueAsInput = command.stringValueOfParameterNamed(startDateParamName);
            final LocalDate newValue = command.localDateValueOfParameterNamed(startDateParamName);
            final LocalDate currentDate = DateUtils.getLocalDateOfTenant();

            if (newValue.isBefore(currentDate)) {
                final String defaultUserMessage = "New meeting effective from date cannot be in past";
                throw new CalendarDateException("new.start.date.cannot.be.in.past", defaultUserMessage, newValue, calendar.getStartDateLocalDate());
            } else if (calendar.isStartDateAfter(newValue) && calendar.isStartDateBeforeOrEqual(currentDate)) {
                // new meeting date should be on or after start date or current
                // date
                final String defaultUserMessage = "New meeting effective from date cannot be a date before existing meeting start date";
                throw new CalendarDateException("new.start.date.before.existing.date", defaultUserMessage, newValue,
                    calendar.getStartDateLocalDate());
            } else {
                actualChanges.put(startDateParamName, valueAsInput);
                actualChanges.put("dateFormat", dateFormatAsInput);
                actualChanges.put("locale", localeAsInput);
                calendar.setStartDate(newValue.toDate());
            }
        }

        final String endDateParamName = CALENDAR_SUPPORTED_PARAMETERS.END_DATE.getValue();
        if (command.isChangeInLocalDateParameterNamed(endDateParamName, calendar.getEndDateLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(endDateParamName);
            actualChanges.put(endDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(endDateParamName);
            calendar.setEndDate(newValue.toDate());
        }

        final String durationParamName = CALENDAR_SUPPORTED_PARAMETERS.DURATION.getValue();
        if (command.isChangeInIntegerSansLocaleParameterNamed(durationParamName, calendar.getDuration())) {
            final Integer newValue = command.integerValueSansLocaleOfParameterNamed(durationParamName);
            actualChanges.put(durationParamName, newValue);
            calendar.setDuration(newValue);
        }

        // Do not allow to change calendar type
        // TODO: AA Instead of throwing an exception, do not allow meeting
        // calendar type to update.
        final String typeParamName = CALENDAR_SUPPORTED_PARAMETERS.TYPE_ID.getValue();
        if (command.isChangeInIntegerSansLocaleParameterNamed(typeParamName, calendar.getTypeId())) {
            final Integer newValue = command.integerValueSansLocaleOfParameterNamed(typeParamName);
            final String defaultUserMessage = "Meeting calendar type update is not supported";
            final String oldMeeingType = CalendarType.fromInt(calendar.getTypeId()).name();
            final String newMeetingType = CalendarType.fromInt(newValue).name();

            throw new CalendarParameterUpdateNotSupportedException("meeting.type", defaultUserMessage, newMeetingType, oldMeeingType);
            /*
             * final Integer newValue =
             * command.integerValueSansLocaleOfParameterNamed(typeParamName);
             * actualChanges.put(typeParamName, newValue); this.typeId =
             * newValue;
             */
        }

        final String repeatingParamName = CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue();
        if (command.isChangeInBooleanParameterNamed(repeatingParamName, calendar.isRepeating())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(repeatingParamName);
            actualChanges.put(repeatingParamName, newValue);
            calendar.setRepeating(newValue);
        }

        // if repeating is false then update recurrence to NULL
        if (!calendar.isRepeating()) {
            calendar.setRecurrence(null);
        }

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(CALENDAR_RESOURCE_NAME);

        final CalendarType calendarType = CalendarType.fromInt(calendar.getTypeId());
        if (calendarType.isCollection() && !calendar.isRepeating()) {
            baseDataValidator.reset().parameter(CALENDAR_SUPPORTED_PARAMETERS.REPEATING.getValue())
                .failWithCodeNoParameterAddedToErrorCode("must.repeat.for.collection.calendar");
            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
        }

        final String newRecurrence = Calendar.toRecurrence(command, calendar);
        if (!StringUtils.isBlank(calendar.getRecurrence()) && !newRecurrence.equalsIgnoreCase(calendar.getRecurrence())) {
            /*
             * If active entities like JLG loan or RD accounts are synced to the
             * calendar then do not allow to change meeting frequency
             */

            if (areActiveEntitiesSynced && !CalendarUtils.isFrequencySame(calendar.getRecurrence(), newRecurrence)) {
                final String defaultUserMessage = "Update of meeting frequency is not supported";
                throw new CalendarParameterUpdateNotSupportedException("meeting.frequency", defaultUserMessage);
            }

            /*
             * If active entities like JLG loan or RD accounts are synced to the
             * calendar then do not allow to change meeting interval
             */

            if (areActiveEntitiesSynced && !CalendarUtils.isIntervalSame(calendar.getRecurrence(), newRecurrence)) {
                final String defaultUserMessage = "Update of meeting interval is not supported";
                throw new CalendarParameterUpdateNotSupportedException("meeting.interval", defaultUserMessage);
            }

            actualChanges.put("recurrence", newRecurrence);
            calendar.setRecurrence(StringUtils.defaultIfEmpty(newRecurrence, null));
        }

        final String remindByParamName = CALENDAR_SUPPORTED_PARAMETERS.REMIND_BY_ID.getValue();
        if (command.isChangeInIntegerSansLocaleParameterNamed(remindByParamName, calendar.getRemindById())) {
            final Integer newValue = command.integerValueSansLocaleOfParameterNamed(remindByParamName);
            actualChanges.put(remindByParamName, newValue);
            calendar.setRemindById(newValue);
        }

        final String firstRemindarParamName = CALENDAR_SUPPORTED_PARAMETERS.FIRST_REMINDER.getValue();
        if (command.isChangeInIntegerSansLocaleParameterNamed(firstRemindarParamName, calendar.getFirstReminder())) {
            final Integer newValue = command.integerValueSansLocaleOfParameterNamed(firstRemindarParamName);
            actualChanges.put(firstRemindarParamName, newValue);
            calendar.setFirstReminder(newValue);
        }

        final String secondRemindarParamName = CALENDAR_SUPPORTED_PARAMETERS.SECOND_REMINDER.getValue();
        if (command.isChangeInIntegerSansLocaleParameterNamed(secondRemindarParamName, calendar.getSecondReminder())) {
            final Integer newValue = command.integerValueSansLocaleOfParameterNamed(secondRemindarParamName);
            actualChanges.put(secondRemindarParamName, newValue);
            calendar.setSecondReminder(newValue);
        }

        final String timeFormat = command.stringValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.Time_Format.getValue());
        final String time = CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue();
        if (command.isChangeInTimeParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue(), calendar.getMeetingtime(), timeFormat)) {
            final String newValue = command.stringValueOfParameterNamed(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue());
            actualChanges.put(CALENDAR_SUPPORTED_PARAMETERS.MEETING_TIME.getValue(), newValue);
            LocalDateTime timeInLocalDateTimeFormat=command.localTimeValueOfParameterNamed(time);
            if(timeInLocalDateTimeFormat!=null){
                calendar.setMeetingtime(timeInLocalDateTimeFormat.toDate());
            }

        }

        return actualChanges;
    }
}
