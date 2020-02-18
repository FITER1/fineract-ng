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
package org.apache.fineract.organisation.staff.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.organisation.staff.exception.StaffNotFoundException;
import org.apache.fineract.organisation.staff.serialization.StaffCommandFromApiJsonDeserializer;
import org.joda.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffWritePlatformServiceJpaRepositoryImpl implements StaffWritePlatformService {

    private final StaffCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final StaffRepository staffRepository;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;

    @Transactional
    @Override
    public CommandProcessingResult createStaff(final JsonCommand command) {

        try {
            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final Long officeId = command.longValueOfParameterNamed("officeId");

            final Office staffOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
            final Staff staff = fromJson(staffOffice, command);

            this.staffRepository.save(staff);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(staff.getId()).officeId(officeId) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleStaffDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleStaffDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateStaff(final Long staffId, final JsonCommand command) {

        try {
            this.fromApiJsonDeserializer.validateForUpdate(command.json(), staffId);

            final Staff staffForUpdate = this.staffRepository.findById(staffId)
                    .orElseThrow(() -> new StaffNotFoundException(staffId));
            final Map<String, Object> changesOnly = update(staffForUpdate, command);

            if (changesOnly.containsKey("officeId")) {
                final Long officeId = (Long) changesOnly.get("officeId");
                final Office newOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
                staffForUpdate.setOffice(newOffice);
            }

            if (!changesOnly.isEmpty()) {
                this.staffRepository.saveAndFlush(staffForUpdate);
            }

            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(staffId)
                    .officeId(staffForUpdate.getOffice().getId()).changes(changesOnly).build();
        } catch (final DataIntegrityViolationException dve) {
            handleStaffDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleStaffDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    private Staff fromJson(final Office staffOffice, final JsonCommand command) {

        final String firstnameParamName = "firstname";
        final String firstname = command.stringValueOfParameterNamed(firstnameParamName);

        final String lastnameParamName = "lastname";
        final String lastname = command.stringValueOfParameterNamed(lastnameParamName);

        final String externalIdParamName = "externalId";
        final String externalId = command.stringValueOfParameterNamedAllowingNull(externalIdParamName);

        final String mobileNoParamName = "mobileNo";
        final String mobileNo = command.stringValueOfParameterNamedAllowingNull(mobileNoParamName);

        final String isLoanOfficerParamName = "isLoanOfficer";
        final boolean isLoanOfficer = command.booleanPrimitiveValueOfParameterNamed(isLoanOfficerParamName);

        final String isActiveParamName = "isActive";
        final Boolean isActive = command.booleanObjectValueOfParameterNamed(isActiveParamName);

        LocalDate joiningDate = null;

        final String joiningDateParamName = "joiningDate";
        if (command.hasParameter(joiningDateParamName)) {
            joiningDate = command.localDateValueOfParameterNamed(joiningDateParamName);
        }

        return Staff.builder()
            .office(staffOffice)
            .firstname(firstname)
            .lastname(lastname)
            .externalId(externalId)
            .mobileNo(mobileNo)
            .loanOfficer(isLoanOfficer)
            .active(isActive)
            .joiningDate(joiningDate==null ? null : joiningDate.toDate())
            .displayName(deriveDisplayName(firstname, lastname))
            .build();
    }

    private Map<String, Object> update(final Staff staff, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final String officeIdParamName = "officeId";
        if (command.isChangeInLongParameterNamed(officeIdParamName, staff.getOffice().getId())) {
            final Long newValue = command.longValueOfParameterNamed(officeIdParamName);
            actualChanges.put(officeIdParamName, newValue);
        }

        boolean firstnameChanged = false;
        final String firstnameParamName = "firstname";
        if (command.isChangeInStringParameterNamed(firstnameParamName, staff.getFirstname())) {
            final String newValue = command.stringValueOfParameterNamed(firstnameParamName);
            actualChanges.put(firstnameParamName, newValue);
            staff.setFirstname(newValue);
            firstnameChanged = true;
        }

        boolean lastnameChanged = false;
        final String lastnameParamName = "lastname";
        if (command.isChangeInStringParameterNamed(lastnameParamName, staff.getLastname())) {
            final String newValue = command.stringValueOfParameterNamed(lastnameParamName);
            actualChanges.put(lastnameParamName, newValue);
            staff.setLastname(newValue);
            lastnameChanged = true;
        }

        if (firstnameChanged || lastnameChanged) {
            staff.setDisplayName(deriveDisplayName(staff.getFirstname(), staff.getLastname()));
        }

        final String externalIdParamName = "externalId";
        if (command.isChangeInStringParameterNamed(externalIdParamName, staff.getExternalId())) {
            final String newValue = command.stringValueOfParameterNamed(externalIdParamName);
            actualChanges.put(externalIdParamName, newValue);
            staff.setExternalId(newValue);
        }

        final String mobileNoParamName = "mobileNo";
        if (command.isChangeInStringParameterNamed(mobileNoParamName, staff.getMobileNo())) {
            final String newValue = command.stringValueOfParameterNamed(mobileNoParamName);
            actualChanges.put(mobileNoParamName, newValue);
            staff.setMobileNo(StringUtils.defaultIfEmpty(newValue, null));
        }

        final String isLoanOfficerParamName = "isLoanOfficer";
        if (command.isChangeInBooleanParameterNamed(isLoanOfficerParamName, staff.isLoanOfficer())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(isLoanOfficerParamName);
            actualChanges.put(isLoanOfficerParamName, newValue);
            staff.setLoanOfficer(newValue);
        }

        final String isActiveParamName = "isActive";
        if (command.isChangeInBooleanParameterNamed(isActiveParamName, staff.isActive())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(isActiveParamName);
            actualChanges.put(isActiveParamName, newValue);
            staff.setActive(newValue);
        }

        final String joiningDateParamName = "joiningDate";
        if (command.isChangeInDateParameterNamed(joiningDateParamName, staff.getJoiningDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(joiningDateParamName);
            actualChanges.put(joiningDateParamName, valueAsInput);
            final LocalDate newValue = command.localDateValueOfParameterNamed(joiningDateParamName);
            staff.setJoiningDate(newValue.toDate());
        }

        return actualChanges;
    }

    private static String deriveDisplayName(String firstname, String lastname) {
        if (!StringUtils.isBlank(firstname)) {
            return lastname + ", " + firstname;
        } else {
            return lastname;
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleStaffDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("external_id")) {

            final String externalId = command.stringValueOfParameterNamed("externalId");
            throw new PlatformDataIntegrityException("error.msg.staff.duplicate.externalId", "Staff with externalId `" + externalId
                    + "` already exists", "externalId", externalId);
        } else if (realCause.getMessage().contains("display_name")) {
            final String lastname = command.stringValueOfParameterNamed("lastname");
            String displayName = lastname;
            if (!StringUtils.isBlank(displayName)) {
                final String firstname = command.stringValueOfParameterNamed("firstname");
                displayName = lastname + ", " + firstname;
            }
            throw new PlatformDataIntegrityException("error.msg.staff.duplicate.displayName", "A staff with the given display name '"
                    + displayName + "' already exists", "displayName", displayName);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.staff.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}