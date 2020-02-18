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
package org.apache.fineract.organisation.teller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.accounting.common.AccountingConstants.FINANCIAL_ACTIVITY;
import org.apache.fineract.accounting.financialactivityaccount.domain.FinancialActivityAccount;
import org.apache.fineract.accounting.financialactivityaccount.domain.FinancialActivityAccountRepositoryWrapper;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryType;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepository;
import org.apache.fineract.organisation.staff.exception.StaffNotFoundException;
import org.apache.fineract.organisation.teller.data.CashierTransactionDataValidator;
import org.apache.fineract.organisation.teller.domain.*;
import org.apache.fineract.organisation.teller.exception.CashierExistForTellerException;
import org.apache.fineract.organisation.teller.exception.CashierNotFoundException;
import org.apache.fineract.organisation.teller.serialization.TellerCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.client.domain.ClientTransaction;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TellerWritePlatformServiceJpaImpl implements TellerWritePlatformService {

    private final PlatformSecurityContext context;
    private final TellerCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final TellerRepositoryWrapper tellerRepositoryWrapper;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final StaffRepository staffRepository;
    private final CashierRepository cashierRepository;
    private final CashierTransactionRepository cashierTxnRepository;
    private final JournalEntryRepository glJournalEntryRepository;
    private final FinancialActivityAccountRepositoryWrapper financialActivityAccountRepositoryWrapper;
    private final CashierTransactionDataValidator cashierTransactionDataValidator;

    @Override
    @Transactional
    public CommandProcessingResult createTeller(JsonCommand command) {
        try {
            this.context.authenticatedUser();

            final Long officeId = command.longValueOfParameterNamed("officeId");

            this.fromApiJsonDeserializer.validateForCreateAndUpdateTeller(command.json());

            // final Office parent =
            // validateUserPriviledgeOnOfficeAndRetrieve(currentUser, officeId);
            final Office tellerOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
            final Teller teller = fromJson(tellerOffice, command);

            // pre save to generate id for use in office hierarchy
            this.tellerRepositoryWrapper.save(teller);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(teller.getId()) //
                    .officeId(teller.getOffice().getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleTellerDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleTellerDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Override
    @Transactional
    public CommandProcessingResult modifyTeller(Long tellerId, JsonCommand command) {
        try {

            final Long officeId = command.longValueOfParameterNamed("officeId");
            final Office tellerOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreateAndUpdateTeller(command.json());

            final Teller teller = validateUserPriviledgeOnTellerAndRetrieve(currentUser, tellerId);

            final Map<String, Object> changes = update(teller, tellerOffice, command);

            if (!changes.isEmpty()) {
                this.tellerRepositoryWrapper.saveAndFlush(teller);
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(teller.getId()) //
                    .officeId(teller.getOffice().getId()) //
                    .changes(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleTellerDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleTellerDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Override
    @Transactional
    public CommandProcessingResult deleteTeller(Long tellerId) {
        // TODO Auto-generated method stub

        Teller teller = tellerRepositoryWrapper.findOneWithNotFoundDetection(tellerId);
        Set<Cashier> isTellerIdPresentInCashier = teller.getCashiers();

        for (final Cashier tellerIdInCashier : isTellerIdPresentInCashier) {
            if (tellerIdInCashier.getTeller().getId().toString()
                    .equalsIgnoreCase(tellerId.toString())) { throw new CashierExistForTellerException(tellerId); }

        }
        tellerRepositoryWrapper.delete(teller);
        return CommandProcessingResult.builder() //
                .resourceId(teller.getId()) //
                .build();

    }

    private Teller fromJson(final Office tellerOffice, final JsonCommand command) {
        final String name = command.stringValueOfParameterNamed("name");
        final String description = command.stringValueOfParameterNamed("description");
        final LocalDate startDate = command.localDateValueOfParameterNamed("startDate");
        final LocalDate endDate = command.localDateValueOfParameterNamed("endDate");
        final Integer tellerStatusInt = command.integerValueOfParameterNamed("status");
        final TellerStatus status = TellerStatus.fromInt(tellerStatusInt);

        return Teller.builder()
            .office(tellerOffice)
            .name(name)
            .description(description)
            .startDate(startDate==null ? null : startDate.toDate())
            .endDate(endDate==null ? null : endDate.toDate())
            .status(status==null ? null : status.getValue())
            .build();
    }

    private Map<String, Object> update(final Teller teller, final Office tellerOffice, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        final String officeIdParamName = "officeId";
        if (command.isChangeInLongParameterNamed(officeIdParamName, teller.getOffice().getId())) {
            final long newValue = command.longValueOfParameterNamed(officeIdParamName);
            actualChanges.put(officeIdParamName, newValue);
            teller.setOffice(tellerOffice);
        }

        final String nameParamName = "name";
        if (command.isChangeInStringParameterNamed(nameParamName, teller.getName())) {
            final String newValue = command.stringValueOfParameterNamed(nameParamName);
            actualChanges.put(nameParamName, newValue);
            teller.setName(newValue);
        }

        final String descriptionParamName = "description";
        if (command.isChangeInStringParameterNamed(descriptionParamName, teller.getDescription())) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            teller.setDescription(newValue);
        }

        final String startDateParamName = "startDate";
        if (command.isChangeInLocalDateParameterNamed(startDateParamName, toLocalDate(teller.getStartDate()))) {
            final String valueAsInput = command.stringValueOfParameterNamed(startDateParamName);
            actualChanges.put(startDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(startDateParamName);
            teller.setStartDate(newValue.toDate());
        }

        final String endDateParamName = "endDate";
        if (command.isChangeInLocalDateParameterNamed(endDateParamName, toLocalDate(teller.getEndDate()))) {
            final String valueAsInput = command.stringValueOfParameterNamed(endDateParamName);
            actualChanges.put(endDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(endDateParamName);
            teller.setEndDate(newValue.toDate());
        }

        final String statusParamName = "status";
        if (command.isChangeInIntegerParameterNamed(statusParamName, teller.getStatus())) {
            final Integer valueAsInput = command.integerValueOfParameterNamed(statusParamName);
            actualChanges.put(statusParamName, valueAsInput);
            final Integer newValue = command.integerValueOfParameterNamed(statusParamName);
            final TellerStatus status = TellerStatus.fromInt(newValue);
            if (status != TellerStatus.INVALID) {
                teller.setStatus(status.getValue());
            }
        }

        return actualChanges;
    }

    /*
     * used to restrict modifying operations to office that are either the users
     * office or lower (child) in the office hierarchy
     */
    private Teller validateUserPriviledgeOnTellerAndRetrieve(final AppUser currentUser, final Long tellerId) {

        final Long userOfficeId = currentUser.getOffice().getId();
        final Office userOffice = this.officeRepositoryWrapper.findOfficeHierarchy(userOfficeId);
        final Teller tellerToReturn = this.tellerRepositoryWrapper.findOneWithNotFoundDetection(tellerId);
        final Long tellerOfficeId = tellerToReturn.getOffice().getId();
        if (userOffice.doesNotHaveAnOfficeInHierarchyWithId(tellerOfficeId)) { throw new NoAuthorizationException(
            "User does not have sufficient priviledges to act on the provided office."); }
        return tellerToReturn;
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleTellerDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("m_tellers_name_unq")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.teller.duplicate.name", "Teller with name `" + name + "` already exists",
                    "name", name);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.teller.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    @Override
    public CommandProcessingResult allocateCashierToTeller(final Long tellerId, JsonCommand command) {
        try {
            this.context.authenticatedUser();
            Long hourStartTime;
            Long minStartTime;
            Long hourEndTime;
            Long minEndTime;
            String startTime = " ";
            String endTime = " ";
            final Teller teller = this.tellerRepositoryWrapper.findOneWithNotFoundDetection(tellerId);
            final Office tellerOffice = teller.getOffice();

            final Long staffId = command.longValueOfParameterNamed("staffId");

            this.fromApiJsonDeserializer.validateForAllocateCashier(command.json());

            final Staff staff = this.staffRepository.findById(staffId)
                    .orElseThrow(() -> new StaffNotFoundException(staffId));
            final Boolean isFullDay = command.booleanObjectValueOfParameterNamed("isFullDay");
            if (!isFullDay) {
                hourStartTime = command.longValueOfParameterNamed("hourStartTime");
                minStartTime = command.longValueOfParameterNamed("minStartTime");

                if (minStartTime == 0)
                    startTime = hourStartTime.toString() + ":" + minStartTime.toString() + "0";
                else
                    startTime = hourStartTime.toString() + ":" + minStartTime.toString();

                hourEndTime = command.longValueOfParameterNamed("hourEndTime");
                minEndTime = command.longValueOfParameterNamed("minEndTime");
                if (minEndTime == 0)
                    endTime = hourEndTime.toString() + ":" + minEndTime.toString() + "0";
                else
                    endTime = hourEndTime.toString() + ":" + minEndTime.toString();

            }
            final Cashier cashier = fromJson(tellerOffice, teller, staff, startTime, endTime, command);
            this.cashierTransactionDataValidator.validateCashierAllowedDateAndTime(cashier, teller);
            
            this.cashierRepository.save(cashier);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(teller.getId()) //
                    .subResourceId(cashier.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleTellerDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleTellerDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Override
    @Transactional
    public CommandProcessingResult updateCashierAllocation(Long tellerId, Long cashierId, JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForAllocateCashier(command.json());

            final Long staffId = command.longValueOfParameterNamed("staffId");
            final Staff staff = this.staffRepository.findById(staffId)
                    .orElseThrow(() -> new StaffNotFoundException(staffId));

            final Cashier cashier = validateUserPriviledgeOnCashierAndRetrieve(currentUser, tellerId, cashierId);

            cashier.setStaff(staff);

            // TODO - check if staff office and teller office match

            final Map<String, Object> changes = update(cashier, command);

            if (!changes.isEmpty()) {
                this.cashierRepository.saveAndFlush(cashier);
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(cashier.getTeller().getId()) //
                    .subResourceId(cashier.getId()) //
                    .changes(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleTellerDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleTellerDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    private Cashier validateUserPriviledgeOnCashierAndRetrieve(final AppUser currentUser, final Long tellerId, final Long cashierId) {

        validateUserPriviledgeOnTellerAndRetrieve(currentUser, tellerId);

        return this.cashierRepository.findById(cashierId).orElse(null);
    }

    @Override
    @Transactional
    public CommandProcessingResult deleteCashierAllocation(Long tellerId, Long cashierId, JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();
            final Cashier cashier = validateUserPriviledgeOnCashierAndRetrieve(currentUser, tellerId, cashierId);
            this.cashierRepository.delete(cashier);

        } catch (final DataIntegrityViolationException dve) {
            handleTellerDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleTellerDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }

        return CommandProcessingResult.builder() //
                .resourceId(cashierId) //
                .build();
    }

    /*
     * @Override public CommandProcessingResult inwardCashToCashier (final Long
     * cashierId, final CashierTransaction cashierTxn) { CashierTxnType txnType
     * = CashierTxnType.INWARD_CASH_TXN; // pre save to generate id for use in
     * office hierarchy this.cashierTxnRepository.save(cashierTxn); }
     */

    @Override
    public CommandProcessingResult allocateCashToCashier(final Long cashierId, JsonCommand command) {
    	return doTransactionForCashier(cashierId, CashierTxnType.ALLOCATE, command); // For
                                                                                     // fund
                                                                                     // allocation
                                                                                     // to
                                                                                     // cashier
    }

    @Override
    public CommandProcessingResult settleCashFromCashier(final Long cashierId, JsonCommand command) {
    	
    	this.cashierTransactionDataValidator.validateSettleCashAndCashOutTransactions(cashierId, command);
    	
    	return doTransactionForCashier(cashierId, CashierTxnType.SETTLE, command); // For
                                                                                   // fund
                                                                                   // settlement
                                                                                   // from
                                                                                   // cashier
    }

    private Cashier fromJson(final Office cashierOffice, final Teller teller, final Staff staff, final String startTime,
                             final String endTime, final JsonCommand command) {
        // final Long tellerId = teller.getId();
        // final Long staffId = command.longValueOfParameterNamed("staffId");
        final String description = command.stringValueOfParameterNamed("description");
        final LocalDate startDate = command.localDateValueOfParameterNamed("startDate");
        final LocalDate endDate = command.localDateValueOfParameterNamed("endDate");
        final Boolean isFullDay = command.booleanObjectValueOfParameterNamed("isFullDay");
        /*
         * final String startTime =
         * command.stringValueOfParameterNamed("startTime"); final String
         * endTime = command.stringValueOfParameterNamed("endTime");
         */

        return Cashier.builder()
            .office(cashierOffice)
            .teller(teller)
            .staff(staff)
            .description(description)
            .startDate(startDate.toDate())
            .endDate(endDate.toDate())
            .fullDay(isFullDay)
            .startTime(startTime)
            .endTime(endTime)
            .build();
    }

    private Map<String, Object> update(final Cashier cashier, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        final String descriptionParamName = "description";
        if (command.isChangeInStringParameterNamed(descriptionParamName, cashier.getDescription())) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            cashier.setDescription(newValue);
        }

        final String startDateParamName = "startDate";
        if (command.isChangeInLocalDateParameterNamed(startDateParamName, toLocalDate(cashier.getStartDate()))) {
            final String valueAsInput = command.stringValueOfParameterNamed(startDateParamName);
            actualChanges.put(startDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(startDateParamName);
            cashier.setStartDate(newValue.toDate());
        }

        final String endDateParamName = "endDate";
        if (command.isChangeInLocalDateParameterNamed(endDateParamName, toLocalDate(cashier.getEndDate()))) {
            final String valueAsInput = command.stringValueOfParameterNamed(endDateParamName);
            actualChanges.put(endDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(endDateParamName);
            cashier.setEndDate(newValue.toDate());
        }

        final Boolean isFullDay = command.booleanObjectValueOfParameterNamed("isFullDay");

        final String isFullDayParamName = "isFullDay";
        if (command.isChangeInBooleanParameterNamed(isFullDayParamName, cashier.getFullDay())) {
            final Boolean newValue = command.booleanObjectValueOfParameterNamed(isFullDayParamName);
            actualChanges.put(isFullDayParamName, newValue);
            cashier.setFullDay(newValue);
        }

        if (!isFullDay) {
            String newStartHour = "";
            String newStartMin = "";
            String newEndHour = "";
            String newEndMin = "";
            final String hourStartTimeParamName = "hourStartTime";
            final String minStartTimeParamName = "minStartTime";
            final String hourEndTimeParamName = "hourEndTime";
            final String minEndTimeParamName = "minEndTime";
            if (command.isChangeInLongParameterNamed(hourStartTimeParamName, toTimePart(cashier.getStartTime(), 0))
                || command.isChangeInLongParameterNamed(minStartTimeParamName, toTimePart(cashier.getStartTime(), 1))) {
                newStartHour = command.stringValueOfParameterNamed(hourStartTimeParamName);
                if(newEndHour.equalsIgnoreCase("0")){
                    newEndHour= newEndHour + "0";
                }
                actualChanges.put(hourStartTimeParamName, newStartHour);
                newStartMin = command.stringValueOfParameterNamed(minStartTimeParamName);
                if(newStartMin.equalsIgnoreCase("0")){
                    newStartMin= newStartMin + "0";
                }
                actualChanges.put(minStartTimeParamName, newStartMin);
                cashier.setStartTime(newStartHour + ":" + newStartMin);
            }

            if (command.isChangeInLongParameterNamed(hourEndTimeParamName, toTimePart(cashier.getEndTime(), 0))
                || command.isChangeInLongParameterNamed(minEndTimeParamName, toTimePart(cashier.getEndTime(), 1))) {
                newEndHour = command.stringValueOfParameterNamed(hourEndTimeParamName);
                if(newEndHour.equalsIgnoreCase("0")){
                    newEndHour= newEndHour + "0";
                }
                actualChanges.put(hourEndTimeParamName, newEndHour);
                newEndMin = command.stringValueOfParameterNamed(minEndTimeParamName);
                if(newEndMin.equalsIgnoreCase("0")){
                    newEndMin= newEndMin + "0";
                }
                actualChanges.put(minEndTimeParamName, newEndMin);
                cashier.setEndTime(newEndHour + ":" + newEndMin);
            }

        }

        return actualChanges;
    }

    private Long toTimePart(String time, int index) {
        if (time != null && !time.equalsIgnoreCase("")) {
            String[] extractHourFromStartTime = time.split(":");
            return Long.valueOf(extractHourFromStartTime[index]);
        }
        return null;
    }

    private LocalDate toLocalDate(Date date) {
        LocalDate localDate = null;
        if (date != null) {
            localDate = LocalDate.fromDateFields(date);
        }
        return localDate;
    }

    private CommandProcessingResult doTransactionForCashier(final Long cashierId, final CashierTxnType txnType, JsonCommand command) {
        try {
            final AppUser currentUser = this.context.authenticatedUser();

            final Cashier cashier = this.cashierRepository.findById(cashierId)
                    .orElseThrow(() -> new CashierNotFoundException(cashierId));

            this.fromApiJsonDeserializer.validateForCashTxnForCashier(command.json());

            final String entityType = command.stringValueOfParameterNamed("entityType");
            final Long entityId = command.longValueOfParameterNamed("entityId");
            if (entityType != null) {
                if (entityType.equals("loan account")) {
                    // TODO : Check if loan account exists
                    // LoanAccount loan = null;
                    // if (loan == null) { throw new
                    // LoanAccountFoundException(entityId); }
                } else if (entityType.equals("savings account")) {
                    // TODO : Check if loan account exists
                    // SavingsAccount savingsaccount = null;
                    // if (savingsaccount == null) { throw new
                    // SavingsAccountNotFoundException(entityId); }

                }
                if (entityType.equals("client")) {
                    // TODO: Check if client exists
                    // Client client = null;
                    // if (client == null) { throw new
                    // ClientNotFoundException(entityId); }
                } else {
                    // TODO : Invalid type handling
                }
            }

            final CashierTransaction cashierTxn = fromJson(cashier, command);
            cashierTxn.setTxnType(txnType.getId());

            this.cashierTxnRepository.save(cashierTxn);

            // Pass the journal entries
            FinancialActivityAccount mainVaultFinancialActivityAccount = this.financialActivityAccountRepositoryWrapper
                    .findByFinancialActivityTypeWithNotFoundDetection(FINANCIAL_ACTIVITY.CASH_AT_MAINVAULT.getValue());
            FinancialActivityAccount tellerCashFinancialActivityAccount = this.financialActivityAccountRepositoryWrapper
                    .findByFinancialActivityTypeWithNotFoundDetection(FINANCIAL_ACTIVITY.CASH_AT_TELLER.getValue());
            GLAccount creditAccount = null;
            GLAccount debitAccount = null;
            if (txnType.equals(CashierTxnType.ALLOCATE)) {
                debitAccount = tellerCashFinancialActivityAccount.getGlAccount();
                creditAccount = mainVaultFinancialActivityAccount.getGlAccount();
            } else if (txnType.equals(CashierTxnType.SETTLE)) {
                debitAccount = mainVaultFinancialActivityAccount.getGlAccount();
                creditAccount = tellerCashFinancialActivityAccount.getGlAccount();
            }

            final Office cashierOffice = cashier.getTeller().getOffice();

            final Long time = System.currentTimeMillis();
            final String uniqueVal = String.valueOf(time) + currentUser.getId() + cashierOffice.getId();
            final String transactionId = Long.toHexString(Long.parseLong(uniqueVal));
            ClientTransaction clientTransaction = null;
            final Long shareTransactionId = null;

            final JournalEntry debitJournalEntry = JournalEntry.builder()
                .office(cashierOffice)
                .glAccount(debitAccount)
                .currencyCode(cashierTxn.getCurrencyCode())
                .transactionId(transactionId)
                .manualEntry(false)
                .transactionDate(cashierTxn.getTxnDate())
                .type(JournalEntryType.DEBIT.getValue())
                .amount(cashierTxn.getTxnAmount())
                .description(cashierTxn.getTxnNote())
                .clientTransaction(clientTransaction)
                .shareTransactionId(shareTransactionId)
                .build();

            final JournalEntry creditJournalEntry = JournalEntry.builder()
                .office(cashierOffice)
                .glAccount(debitAccount)
                .currencyCode(cashierTxn.getCurrencyCode())
                .transactionId(transactionId)
                .manualEntry(false)
                .transactionDate(cashierTxn.getTxnDate())
                .type(JournalEntryType.CREDIT.getValue())
                .amount(cashierTxn.getTxnAmount())
                .description(cashierTxn.getTxnNote())
                .clientTransaction(clientTransaction)
                .shareTransactionId(shareTransactionId)
                .build();

            this.glJournalEntryRepository.saveAndFlush(debitJournalEntry);
            this.glJournalEntryRepository.saveAndFlush(creditJournalEntry);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(cashier.getId()) //
                    .subResourceId(cashierTxn.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleTellerDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleTellerDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    private CashierTransaction fromJson(final Cashier cashier, final JsonCommand command) {
        final Integer txnType = command.integerValueOfParameterNamed("txnType");
        final BigDecimal txnAmount = command.bigDecimalValueOfParameterNamed("txnAmount");
        final LocalDate txnDate = command.localDateValueOfParameterNamed("txnDate");
        final String entityType = command.stringValueOfParameterNamed("entityType");
        final String txnNote = command.stringValueOfParameterNamed("txnNote");
        final Long entityId = command.longValueOfParameterNamed("entityId");
        final String currencyCode = command.stringValueOfParameterNamed("currencyCode");

        // TODO: get client/loan/savings details
        return CashierTransaction.builder()
            .cashier(cashier)
            .txnType(txnType)
            .txnAmount(txnAmount)
            .txnDate(txnDate==null ? null : txnDate.toDate())
            .entityType(entityType)
            .entityId(entityId)
            .txnNote(txnNote)
            .currencyCode(currencyCode)
            .build();
    }

    private Map<String, Object> update(final CashierTransaction cashierTransaction, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        final String txnTypeParamName = "txnType";
        if (command.isChangeInIntegerParameterNamed(txnTypeParamName, cashierTransaction.getTxnType())) {
            final Integer newValue = command.integerValueOfParameterNamed(txnTypeParamName);
            actualChanges.put(txnTypeParamName, newValue);
            cashierTransaction.setTxnType(newValue);
        }

        final String txnDateParamName = "txnDate";
        if (command.isChangeInLocalDateParameterNamed(txnDateParamName, toLocalDate(cashierTransaction.getTxnDate()))) {
            final String valueAsInput = command.stringValueOfParameterNamed(txnDateParamName);
            actualChanges.put(txnDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(txnDateParamName);
            cashierTransaction.setTxnDate(newValue.toDate());
        }

        final String txnAmountParamName = "txnAmount";
        if (command.isChangeInBigDecimalParameterNamed(txnAmountParamName, cashierTransaction.getTxnAmount())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(txnAmountParamName);
            actualChanges.put(txnAmountParamName, newValue);
            cashierTransaction.setTxnAmount(newValue);
        }

        final String txnNoteParamName = "txnNote";
        if (command.isChangeInStringParameterNamed(txnNoteParamName, cashierTransaction.getTxnNote())) {
            final String newValue = command.stringValueOfParameterNamed(txnNoteParamName);
            actualChanges.put(txnNoteParamName, newValue);
            cashierTransaction.setTxnNote(newValue);
        }

        final String currencyCodeParamName = "currencyCode";
        if (command.isChangeInStringParameterNamed(currencyCodeParamName, cashierTransaction.getCurrencyCode())) {
            final String newValue = command.stringValueOfParameterNamed(currencyCodeParamName);
            actualChanges.put(currencyCodeParamName, newValue);
            cashierTransaction.setCurrencyCode(newValue);
        }

        return actualChanges;
    }
}
