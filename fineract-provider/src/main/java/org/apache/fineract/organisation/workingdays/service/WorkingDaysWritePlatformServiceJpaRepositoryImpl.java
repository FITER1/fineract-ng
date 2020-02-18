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
package org.apache.fineract.organisation.workingdays.service;

import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.validate.ValidationException;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.organisation.workingdays.api.WorkingDaysApiConstants;
import org.apache.fineract.organisation.workingdays.data.WorkingDayValidator;
import org.apache.fineract.organisation.workingdays.domain.RepaymentRescheduleType;
import org.apache.fineract.organisation.workingdays.domain.WorkingDays;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysEnumerations;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkingDaysWritePlatformServiceJpaRepositoryImpl implements WorkingDaysWritePlatformService {

    private final WorkingDaysRepositoryWrapper daysRepositoryWrapper;
    private final WorkingDayValidator fromApiJsonDeserializer;

    @Transactional
    @Override
    public CommandProcessingResult updateWorkingDays(JsonCommand command) {
        String recurrence = "";
        RRule rrule = null;
        try {
            this.fromApiJsonDeserializer.validateForUpdate(command.json());
            final WorkingDays workingDays = this.daysRepositoryWrapper.findOne();

            recurrence = command.stringValueOfParameterNamed(WorkingDaysApiConstants.recurrence);
            rrule = new RRule(recurrence);
            rrule.validate();

            Map<String, Object> changes = update(workingDays, command);
            this.daysRepositoryWrapper.saveAndFlush(workingDays);
            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(workingDays.getId()).changes(changes)
                    .build();
        } catch (final ValidationException e) {
            throw new PlatformDataIntegrityException("error.msg.invalid.recurring.rule",
                    "The Recurring Rule value: " + recurrence + " is not valid.", "recurrence", recurrence);
        } catch (final IllegalArgumentException | ParseException e) {
            throw new PlatformDataIntegrityException("error.msg.recurring.rule.parsing.error",
                    "Error in passing the Recurring Rule value: " + recurrence, "recurrence", e.getMessage());
        }
    }

    private Map<String, Object> update(final WorkingDays workingDays, final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String recurrenceParamName = "recurrence";
        if (command.isChangeInStringParameterNamed(recurrenceParamName, workingDays.getRecurrence())) {
            final String newValue = command.stringValueOfParameterNamed(recurrenceParamName);
            actualChanges.put(recurrenceParamName, newValue);
            workingDays.setRecurrence(newValue);
        }

        final String repaymentRescheduleTypeParamName = "repaymentRescheduleType";
        if (command.isChangeInIntegerParameterNamed(repaymentRescheduleTypeParamName, workingDays.getRepaymentReschedulingType())) {
            final Integer newValue =command.integerValueOfParameterNamed(repaymentRescheduleTypeParamName);
            actualChanges.put(repaymentRescheduleTypeParamName,  WorkingDaysEnumerations.workingDaysStatusType(newValue));
            workingDays.setRepaymentReschedulingType(RepaymentRescheduleType.fromInt(newValue).getValue());
        }

        if(command.isChangeInBooleanParameterNamed(WorkingDaysApiConstants.extendTermForDailyRepayments, workingDays.getExtendTermForDailyRepayments())){
            final Boolean newValue = command.booleanPrimitiveValueOfParameterNamed(WorkingDaysApiConstants.extendTermForDailyRepayments);
            actualChanges.put(WorkingDaysApiConstants.extendTermForDailyRepayments, newValue);
            workingDays.setExtendTermForDailyRepayments(newValue);
        }

        if (command.isChangeInBooleanParameterNamed(WorkingDaysApiConstants.extendTermForRepaymentsOnHolidays, workingDays.getExtendTermForRepaymentsOnHolidays())) {
            final Boolean newValue = command.booleanPrimitiveValueOfParameterNamed(WorkingDaysApiConstants.extendTermForRepaymentsOnHolidays);
            actualChanges.put(WorkingDaysApiConstants.extendTermForRepaymentsOnHolidays, newValue);
            workingDays.setExtendTermForRepaymentsOnHolidays(newValue);
        }

        return actualChanges;
    }
}
