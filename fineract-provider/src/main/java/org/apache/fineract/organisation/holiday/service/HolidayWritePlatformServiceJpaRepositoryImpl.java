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
package org.apache.fineract.organisation.holiday.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.holiday.api.HolidayApiConstants;
import org.apache.fineract.organisation.holiday.data.HolidayDataValidator;
import org.apache.fineract.organisation.holiday.domain.Holiday;
import org.apache.fineract.organisation.holiday.domain.HolidayRepositoryWrapper;
import org.apache.fineract.organisation.holiday.domain.HolidayStatusType;
import org.apache.fineract.organisation.holiday.domain.RescheduleType;
import org.apache.fineract.organisation.holiday.exception.HolidayDateException;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.workingdays.domain.WorkingDays;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.apache.fineract.organisation.workingdays.service.WorkingDaysUtil;
import org.joda.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.*;

import static org.apache.fineract.organisation.holiday.api.HolidayApiConstants.*;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.dateFormatParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.localeParamName;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayWritePlatformServiceJpaRepositoryImpl implements HolidayWritePlatformService {

    private final HolidayDataValidator fromApiJsonDeserializer;
    private final HolidayRepositoryWrapper holidayRepository;
    private final WorkingDaysRepositoryWrapper daysRepositoryWrapper;
    private final PlatformSecurityContext context;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final FromJsonHelper fromApiJsonHelper;

    @Transactional
    @Override
    public CommandProcessingResult createHoliday(final JsonCommand command) {

        try {
            this.context.authenticatedUser();
            this.fromApiJsonDeserializer.validateForCreate(command.json());

            validateInputDates(command);

            final Set<Office> offices = getSelectedOffices(command);

            final Holiday holiday = createNew(offices, command);

            this.holidayRepository.save(holiday);

            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(holiday.getId()).build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateHoliday(final JsonCommand command) {

        try {
            this.context.authenticatedUser();
            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            final Holiday holiday = this.holidayRepository.findOneWithNotFoundDetection(command.entityId());
            Map<String, Object> changes = update(holiday, command);

            validateInputDates(holiday.getFromDateLocalDate(), holiday.getToDateLocalDate(), holiday.getRepaymentsRescheduledToLocalDate());

            if (changes.containsKey(officesParamName)) {
                final Set<Office> offices = getSelectedOffices(command);
                final boolean updated = update(holiday, offices);
                if (!updated) {
                    changes.remove(officesParamName);
                }
            }

            this.holidayRepository.saveAndFlush(holiday);

            return CommandProcessingResult.builder().resourceId(holiday.getId()).changes(changes).build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult activateHoliday(final Long holidayId) {
        this.context.authenticatedUser();
        final Holiday holiday = this.holidayRepository.findOneWithNotFoundDetection(holidayId);

        activate(holiday);
        this.holidayRepository.saveAndFlush(holiday);
        return CommandProcessingResult.builder().resourceId(holiday.getId()).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteHoliday(final Long holidayId) {
        this.context.authenticatedUser();
        final Holiday holiday = this.holidayRepository.findOneWithNotFoundDetection(holidayId);
        delete(holiday);
        this.holidayRepository.saveAndFlush(holiday);
        return CommandProcessingResult.builder().resourceId(holidayId).build();
    }

    private Map<String, Object> update(final Holiday holiday, final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("holiday" + ".update");

        final HolidayStatusType currentStatus = HolidayStatusType.fromInt(holiday.getStatus());

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        if (command.isChangeInStringParameterNamed(nameParamName, holiday.getName())) {
            final String newValue = command.stringValueOfParameterNamed(nameParamName);
            actualChanges.put(nameParamName, newValue);
            holiday.setName(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(descriptionParamName, holiday.getDescription())) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            holiday.setDescription(StringUtils.defaultIfEmpty(newValue, null));
        }


        if (command.isChangeInIntegerParameterNamed(HolidayApiConstants.reschedulingType, holiday.getReschedulingType())) {
            final Integer newValue =command.integerValueOfParameterNamed(HolidayApiConstants.reschedulingType);
            actualChanges.put(HolidayApiConstants.reschedulingType, newValue);
            holiday.setReschedulingType(RescheduleType.fromInt(newValue).getValue());
            if(newValue.equals(RescheduleType.RESCHEDULETONEXTREPAYMENTDATE.getValue())){
                holiday.setReschedulingType(null);
            }
        }

        if (currentStatus.isPendingActivation()) {
            if (command.isChangeInLocalDateParameterNamed(fromDateParamName, holiday.getFromDateLocalDate())) {
                final String valueAsInput = command.stringValueOfParameterNamed(fromDateParamName);
                actualChanges.put(fromDateParamName, valueAsInput);
                actualChanges.put(dateFormatParamName, dateFormatAsInput);
                actualChanges.put(localeParamName, localeAsInput);
                final LocalDate newValue = command.localDateValueOfParameterNamed(fromDateParamName);
                holiday.setFromDate(newValue.toDate());
            }

            if (command.isChangeInLocalDateParameterNamed(toDateParamName, holiday.getToDateLocalDate())) {
                final String valueAsInput = command.stringValueOfParameterNamed(toDateParamName);
                actualChanges.put(toDateParamName, valueAsInput);
                actualChanges.put(dateFormatParamName, dateFormatAsInput);
                actualChanges.put(localeParamName, localeAsInput);

                final LocalDate newValue = command.localDateValueOfParameterNamed(toDateParamName);
                holiday.setToDate(newValue.toDate());
            }

            if (command.isChangeInLocalDateParameterNamed(repaymentsRescheduledToParamName, holiday.getRepaymentsRescheduledToLocalDate())) {
                final String valueAsInput = command.stringValueOfParameterNamed(repaymentsRescheduledToParamName);
                actualChanges.put(repaymentsRescheduledToParamName, valueAsInput);
                actualChanges.put(dateFormatParamName, dateFormatAsInput);
                actualChanges.put(localeParamName, localeAsInput);

                final LocalDate newValue = command.localDateValueOfParameterNamed(repaymentsRescheduledToParamName);
                holiday.setRepaymentsRescheduledTo(newValue.toDate());
            }

            if (command.hasParameter(officesParamName)) {
                final JsonArray jsonArray = command.arrayOfParameterNamed(officesParamName);
                if (jsonArray != null) {
                    actualChanges.put(officesParamName, command.jsonFragment(officesParamName));
                }
            }
        } else {
            if (command.isChangeInLocalDateParameterNamed(fromDateParamName, holiday.getFromDateLocalDate())) {
                baseDataValidator.reset().parameter(fromDateParamName).failWithCode("cannot.edit.holiday.in.active.state");
            }

            if (command.isChangeInLocalDateParameterNamed(toDateParamName, holiday.getToDateLocalDate())) {
                baseDataValidator.reset().parameter(toDateParamName).failWithCode("cannot.edit.holiday.in.active.state");
            }

            if (command.isChangeInLocalDateParameterNamed(repaymentsRescheduledToParamName, holiday.getRepaymentsRescheduledToLocalDate())) {
                baseDataValidator.reset().parameter(repaymentsRescheduledToParamName).failWithCode("cannot.edit.holiday.in.active.state");
            }

            if (command.hasParameter(officesParamName)) {
                baseDataValidator.reset().parameter(repaymentsRescheduledToParamName).failWithCode("cannot.edit.holiday.in.active.state");
            }

            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        return actualChanges;
    }

    private Holiday createNew(final Set<Office> offices, final JsonCommand command) {
        final String name = command.stringValueOfParameterNamed(HolidayApiConstants.nameParamName);
        final LocalDate fromDate = command.localDateValueOfParameterNamed(HolidayApiConstants.fromDateParamName);
        final LocalDate toDate = command.localDateValueOfParameterNamed(HolidayApiConstants.toDateParamName);
        Integer reschedulingType = null;
        if(command.parameterExists(HolidayApiConstants.reschedulingType)){
            reschedulingType = command.integerValueOfParameterNamed(HolidayApiConstants.reschedulingType);
        }
        LocalDate repaymentsRescheduledTo = null;
        if(reschedulingType == null || reschedulingType.equals(RescheduleType.RESCHEDULETOSPECIFICDATE.getValue())){
            repaymentsRescheduledTo = command
                .localDateValueOfParameterNamed(HolidayApiConstants.repaymentsRescheduledToParamName);
        }
        final Integer status = HolidayStatusType.PENDING_FOR_ACTIVATION.getValue();
        final boolean processed = false;// default it to false. Only batch job
        // should update this field.
        final String description = command.stringValueOfParameterNamed(HolidayApiConstants.descriptionParamName);

        return Holiday.builder()
            .name(name)
            .fromDate(fromDate!=null ? fromDate.toDate() : null)
            .toDate(toDate!=null ? toDate.toDate() : null)
            .repaymentsRescheduledTo(repaymentsRescheduledTo!=null ? repaymentsRescheduledTo.toDate() : null)
            .status(status)
            .processed(processed)
            .description(description)
            .offices(offices)
            .reschedulingType(reschedulingType)
            .build();
    }

    private boolean update(final Holiday holiday, final Set<Office> newOffices) {
        if (newOffices == null) { return false; }

        boolean updated = false;
        if (holiday.getOffices() != null) {
            final Set<Office> currentSetOfOffices = new HashSet<>(holiday.getOffices());
            final Set<Office> newSetOfOffices = new HashSet<>(newOffices);

            if (!(currentSetOfOffices.equals(newSetOfOffices))) {
                updated = true;
                holiday.setOffices(newOffices);
            }
        } else {
            updated = true;
            holiday.setOffices(newOffices);
        }
        return updated;
    }

    private void delete(final Holiday holiday) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("holiday" + ".delete");

        final HolidayStatusType currentStatus = HolidayStatusType.fromInt(holiday.getStatus());
        if (currentStatus.isDeleted()) {
            baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("already.in.deleted.state");
            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
        }
        holiday.setStatus(HolidayStatusType.DELETED.getValue());
    }

    private void activate(final Holiday holiday) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("holiday" + ".activate");

        final HolidayStatusType currentStatus = HolidayStatusType.fromInt(holiday.getStatus());
        if (!currentStatus.isPendingActivation()) {
            baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("not.in.pending.for.activation.state");
            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
        }

        holiday.setStatus(HolidayStatusType.ACTIVE.getValue());
    }

    private Set<Office> getSelectedOffices(final JsonCommand command) {
        Set<Office> offices = null;
        final JsonObject topLevelJsonElement = this.fromApiJsonHelper.parse(command.json()).getAsJsonObject();
        if (topLevelJsonElement.has(HolidayApiConstants.officesParamName)
                && topLevelJsonElement.get(HolidayApiConstants.officesParamName).isJsonArray()) {

            final JsonArray array = topLevelJsonElement.get(HolidayApiConstants.officesParamName).getAsJsonArray();
            offices = new HashSet<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                final JsonObject officeElement = array.get(i).getAsJsonObject();
                final Long officeId = this.fromApiJsonHelper.extractLongNamed(HolidayApiConstants.officeIdParamName, officeElement);
                final Office office = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
                offices.add(office);
            }
        }
        return offices;
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("holiday_name")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.holiday.duplicate.name", "Holiday with name `" + name + "` already exists",
                    "name", name);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.office.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private void validateInputDates(final JsonCommand command) {
        final LocalDate fromDate = command.localDateValueOfParameterNamed(HolidayApiConstants.fromDateParamName);
        final LocalDate toDate = command.localDateValueOfParameterNamed(HolidayApiConstants.toDateParamName);
        Integer reshedulingType = null;
        if(command.parameterExists(HolidayApiConstants.reschedulingType)){
            reshedulingType = command.integerValueOfParameterNamed(HolidayApiConstants.reschedulingType);
        }
        LocalDate repaymentsRescheduledTo = null;
        if(reshedulingType != null && reshedulingType.equals(2)){
            repaymentsRescheduledTo = command
                    .localDateValueOfParameterNamed(HolidayApiConstants.repaymentsRescheduledToParamName);
        }
        if(repaymentsRescheduledTo != null){
            this.validateInputDates(fromDate, toDate, repaymentsRescheduledTo);
        }
    }

    private void validateInputDates(final LocalDate fromDate, final LocalDate toDate, final LocalDate repaymentsRescheduledTo) {

        String defaultUserMessage = "";

        if (toDate.isBefore(fromDate)) {
            defaultUserMessage = "To Date date cannot be before the From Date.";
            throw new HolidayDateException("to.date.cannot.be.before.from.date", defaultUserMessage, fromDate.toString(),
                    toDate.toString());
        }
        if(repaymentsRescheduledTo != null){
            if ((repaymentsRescheduledTo.isEqual(fromDate) || repaymentsRescheduledTo.isEqual(toDate)
                    || (repaymentsRescheduledTo.isAfter(fromDate) && repaymentsRescheduledTo.isBefore(toDate)))) {

                defaultUserMessage = "Repayments rescheduled date should be before from date or after to date.";
                throw new HolidayDateException("repayments.rescheduled.date.should.be.before.from.date.or.after.to.date", defaultUserMessage,
                        repaymentsRescheduledTo.toString());
            }

            final WorkingDays workingDays = this.daysRepositoryWrapper.findOne();
            final Boolean isRepaymentOnWorkingDay = WorkingDaysUtil.isWorkingDay(workingDays, repaymentsRescheduledTo);

            if (!isRepaymentOnWorkingDay) {
                defaultUserMessage = "Repayments rescheduled date should not fall on non working days";
                throw new HolidayDateException("repayments.rescheduled.date.should.not.fall.on.non.working.day", defaultUserMessage,
                        repaymentsRescheduledTo.toString());
            }

            // validate repaymentsRescheduledTo date
            // 1. should be within a 7 days date range.
            // 2. Alternative date should not be an exist holiday.//TBD
            // 3. Holiday should not be on an repaymentsRescheduledTo date of
            // another holiday.//TBD

            // restricting repaymentsRescheduledTo date to be within 7 days range
            // before or after from date and to date.
            if (repaymentsRescheduledTo.isBefore(fromDate.minusDays(7)) || repaymentsRescheduledTo.isAfter(toDate.plusDays(7))) {
                defaultUserMessage = "Repayments Rescheduled to date must be within 7 days before or after from and to dates";
                throw new HolidayDateException("repayments.rescheduled.to.must.be.within.range", defaultUserMessage, fromDate.toString(),
                        toDate.toString(), repaymentsRescheduledTo.toString());
            }
        }


    }

}
