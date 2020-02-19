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
package org.apache.fineract.portfolio.savings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormat;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatRepositoryWrapper;
import org.apache.fineract.infrastructure.accountnumberformat.domain.EntityAccountType;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.portfolio.account.domain.AccountAssociationType;
import org.apache.fineract.portfolio.account.domain.AccountAssociations;
import org.apache.fineract.portfolio.account.domain.AccountAssociationsRepository;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.*;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.apache.fineract.portfolio.client.domain.AccountNumberGenerator;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_ENTITY;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepository;
import org.apache.fineract.portfolio.group.exception.CenterNotActiveException;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.group.exception.GroupNotFoundException;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.DepositsApiConstants;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.data.DepositAccountDataValidator;
import org.apache.fineract.portfolio.savings.domain.*;
import org.apache.fineract.portfolio.savings.exception.SavingsProductNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.math.MathContext;
import java.util.*;

import static org.apache.fineract.portfolio.savings.DepositsApiConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositApplicationProcessWritePlatformServiceJpaRepositoryImpl implements DepositApplicationProcessWritePlatformService {

    private final PlatformSecurityContext context;
    private final SavingsAccountRepositoryWrapper savingAccountRepository;
    private final FixedDepositAccountRepository fixedDepositAccountRepository;
    private final RecurringDepositAccountRepository recurringDepositAccountRepository;
    private final DepositAccountAssembler depositAccountAssembler;
    private final DepositAccountDataValidator depositAccountDataValidator;
    private final AccountNumberGenerator accountNumberGenerator;
    private final ClientRepositoryWrapper clientRepository;
    private final GroupRepository groupRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final NoteRepository noteRepository;
    private final StaffRepositoryWrapper staffRepository;
    private final SavingsAccountApplicationTransitionApiJsonValidator savingsAccountApplicationTransitionApiJsonValidator;
    private final SavingsAccountChargeAssembler savingsAccountChargeAssembler;
    private final AccountAssociationsRepository accountAssociationsRepository;
    private final FromJsonHelper fromJsonHelper;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final AccountNumberFormatRepositoryWrapper accountNumberFormatRepository;
    private final BusinessEventNotifierService businessEventNotifierService;

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        final StringBuilder errorCodeBuilder = new StringBuilder("error.msg.").append(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        if (realCause.getMessage().contains("'sa_account_no_UNIQUE'")) {
            final String accountNo = command.stringValueOfParameterNamed("accountNo");
            errorCodeBuilder.append(".duplicate.accountNo");
            throw new PlatformDataIntegrityException(errorCodeBuilder.toString(), "Savings account with accountNo " + accountNo
                    + " already exists", "accountNo", accountNo);

        } else if (realCause.getMessage().contains("sa_external_id_UNIQUE")) {

            final String externalId = command.stringValueOfParameterNamed("externalId");
            errorCodeBuilder.append(".duplicate.externalId");
            throw new PlatformDataIntegrityException(errorCodeBuilder.toString(), "Savings account with externalId " + externalId
                    + " already exists", "externalId", externalId);
        }

        errorCodeBuilder.append(".unknown.data.integrity.issue");
        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException(errorCodeBuilder.toString(), "Unknown data integrity issue with savings account.");
    }

    @Transactional
    @Override
    public CommandProcessingResult submitFDApplication(final JsonCommand command) {
        try {
            this.depositAccountDataValidator.validateFixedDepositForSubmit(command.json());
            final AppUser submittedBy = this.context.authenticatedUser();

            final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                    .isSavingsInterestPostingAtCurrentPeriodEnd();
            final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

            final FixedDepositAccount account = (FixedDepositAccount) this.depositAccountAssembler.assembleFrom(command, submittedBy,
                    DepositAccountType.FIXED_DEPOSIT);

            final MathContext mc = MathContext.DECIMAL64;
            final boolean isPreMatureClosure = false;

            account.updateMaturityDateAndAmountBeforeAccountActivation(mc, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth);
            this.fixedDepositAccountRepository.save(account);

            if (account.isAccountNumberRequiresAutoGeneration()) {
                AccountNumberFormat accountNumberFormat = this.accountNumberFormatRepository.findByAccountType(EntityAccountType.CLIENT);
                account.updateAccountNo(this.accountNumberGenerator.generate(account, accountNumberFormat));

                this.savingAccountRepository.save(account);
            }

            // Save linked account information
            final Long savingsAccountId = command.longValueOfParameterNamed(DepositsApiConstants.linkedAccountParamName);
            if (savingsAccountId != null) {
                final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(savingsAccountId,
                        DepositAccountType.SAVINGS_DEPOSIT);
                this.depositAccountDataValidator.validatelinkedSavingsAccount(savingsAccount, account);
                boolean isActive = true;
                final AccountAssociations accountAssociations = AccountAssociations.builder()
                    .linkedSavingsAccount(account)
                    .savingsAccount(savingsAccount)
                    .associationType(AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue())
                    .active(isActive)
                    .build();
                this.accountAssociationsRepository.save(accountAssociations);
            }

            final Long savingsId = account.getId();
            this.businessEventNotifierService.notifyBusinessEventWasExecuted( BUSINESS_EVENTS.FIXED_DEPOSIT_ACCOUNT_CREATE,
            		constructEntityMap(BUSINESS_ENTITY.DEPOSIT_ACCOUNT, account));

            this.businessEventNotifierService.notifyBusinessEventWasExecuted( BUSINESS_EVENTS.FIXED_DEPOSIT_ACCOUNT_CREATE,
                    constructEntityMap(BUSINESS_ENTITY.DEPOSIT_ACCOUNT, account));

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(savingsId) //
                    .officeId(account.officeId()) //
                    .clientId(account.clientId()) //
                    .groupId(account.groupId()) //
                    .savingsId(savingsId) //
                    .build();
        } catch (final DataAccessException dve) {
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
    public CommandProcessingResult submitRDApplication(final JsonCommand command) {
        try {
            this.depositAccountDataValidator.validateRecurringDepositForSubmit(command.json());
            final AppUser submittedBy = this.context.authenticatedUser();

            final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                    .isSavingsInterestPostingAtCurrentPeriodEnd();
            final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

            final RecurringDepositAccount account = (RecurringDepositAccount) this.depositAccountAssembler.assembleFrom(command,
                    submittedBy, DepositAccountType.RECURRING_DEPOSIT);

            this.recurringDepositAccountRepository.save(account);

            if (account.isAccountNumberRequiresAutoGeneration()) {
                final AccountNumberFormat accountNumberFormat = this.accountNumberFormatRepository
                        .findByAccountType(EntityAccountType.SAVINGS);
                account.updateAccountNo(this.accountNumberGenerator.generate(account, accountNumberFormat));
            }

            final Long savingsId = account.getId();
            final CalendarInstance calendarInstance = getCalendarInstance(command, account);
            this.calendarInstanceRepository.save(calendarInstance);

            // FIXME: Avoid save separately (Calendar instance requires account
            // details)
            final MathContext mc = MathContext.DECIMAL64;
            final Calendar calendar = calendarInstance.getCalendar();
            final PeriodFrequencyType frequencyType = CalendarFrequencyType.from(CalendarUtils.getFrequency(calendar.getRecurrence()));
            Integer frequency = CalendarUtils.getInterval(calendar.getRecurrence());
            frequency = frequency == -1 ? 1 : frequency;
            account.generateSchedule(frequencyType, frequency, calendar);
            final boolean isPreMatureClosure = false;
            account.updateMaturityDateAndAmount(mc, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth);
            account.validateApplicableInterestRate();
            this.savingAccountRepository.save(account);
            this.businessEventNotifierService.notifyBusinessEventWasExecuted( BUSINESS_EVENTS.RECURRING_DEPOSIT_ACCOUNT_CREATE,
            		constructEntityMap(BUSINESS_ENTITY.DEPOSIT_ACCOUNT, account));

            this.businessEventNotifierService.notifyBusinessEventWasExecuted( BUSINESS_EVENTS.RECURRING_DEPOSIT_ACCOUNT_CREATE,
                    constructEntityMap(BUSINESS_ENTITY.DEPOSIT_ACCOUNT, account));

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(savingsId) //
                    .officeId(account.officeId()) //
                    .clientId(account.clientId()) //
                    .groupId(account.groupId()) //
                    .savingsId(savingsId) //
                    .build();
        } catch (final DataAccessException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
            return new CommandProcessingResult();
        }
    }

    private CalendarInstance getCalendarInstance(final JsonCommand command, RecurringDepositAccount account) {
        CalendarInstance calendarInstance = null;
        final boolean isCalendarInherited = command.booleanPrimitiveValueOfParameterNamed(isCalendarInheritedParamName);

        if (isCalendarInherited) {
            Set<Group> groups = account.getClient().getGroups();
            Long groupId = null;
            if (groups.isEmpty()) {
                final String defaultUserMessage = "Client does not belong to group/center. Cannot follow group/center meeting frequency.";
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.recurring.deposit.account.cannot.create.not.belongs.to.any.groups.to.follow.meeting.frequency",
                        defaultUserMessage, account.clientId());
            } else if (groups.size() > 1) {
                final String defaultUserMessage = "Client belongs to more than one group. Cannot support recurring deposit.";
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.recurring.deposit.account.cannot.create.belongs.to.multiple.groups", defaultUserMessage,
                        account.clientId());
            } else {
                Group group = groups.iterator().next();
                Group parent = group.getParent();
                Integer entityType = CalendarEntityType.GROUPS.getValue();
                if (parent != null) {
                    groupId = parent.getId();
                    entityType = CalendarEntityType.CENTERS.getValue();
                } else {
                    groupId = group.getId();
                }
                CalendarInstance parentCalendarInstance = this.calendarInstanceRepository.findByEntityIdAndEntityTypeIdAndCalendarTypeId(
                        groupId, entityType, CalendarType.COLLECTION.getValue());
                if(parentCalendarInstance == null){
                	final String defaultUserMessage = "Meeting frequency is not attached to the Group/Center to which the client belongs to.";
                    throw new GeneralPlatformDomainRuleException(
                            "error.msg.meeting.frequency.not.attached.to.group.to.which.client.belongs.to",
                            defaultUserMessage, account.clientId());
                }
                calendarInstance = new CalendarInstance(parentCalendarInstance.getCalendar(), account.getId(),
                        CalendarEntityType.SAVINGS.getValue());
            }
        } else {
            LocalDate calendarStartDate = account.depositStartDate();
            final Integer frequencyType = command.integerValueSansLocaleOfParameterNamed(recurringFrequencyTypeParamName);
            final PeriodFrequencyType periodFrequencyType = PeriodFrequencyType.fromInt(frequencyType);
            final Integer frequency = command.integerValueSansLocaleOfParameterNamed(recurringFrequencyParamName);

            final Integer repeatsOnDay = calendarStartDate.getDayOfWeek();
            final String title = "recurring_savings_" + account.getId();

            final Calendar calendar = Calendar.builder()
                .title(title)
                .startDate(calendarStartDate.toDate())
                .typeId(CalendarType.COLLECTION.getValue())
                .recurrence(Calendar.toRecurrence(CalendarFrequencyType.from(periodFrequencyType), frequency, repeatsOnDay, null))
                .build();
            calendarInstance = new CalendarInstance(calendar, account.getId(), CalendarEntityType.SAVINGS.getValue());
        }
        if (calendarInstance == null) {
            final String defaultUserMessage = "No valid recurring details available for recurring depost account creation.";
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.recurring.deposit.account.cannot.create.no.valid.recurring.details.available", defaultUserMessage,
                    account.clientId());
        }
        return calendarInstance;
    }

    @Transactional
    @Override
    public CommandProcessingResult modifyFDApplication(final Long accountId, final JsonCommand command) {
        try {
            this.depositAccountDataValidator.validateFixedDepositForUpdate(command.json());

            final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                    .isSavingsInterestPostingAtCurrentPeriodEnd();
            final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

            final Map<String, Object> changes = new LinkedHashMap<>(20);

            final FixedDepositAccount account = (FixedDepositAccount) this.depositAccountAssembler.assembleFrom(accountId,
                    DepositAccountType.FIXED_DEPOSIT);
            checkClientOrGroupActive(account);
            account.modifyApplication(command, changes);
            account.validateNewApplicationState(DateUtils.getLocalDateOfTenant(), DepositAccountType.FIXED_DEPOSIT.resourceName());

            if (!changes.isEmpty()) {
                updateFDAndRDCommonChanges(changes, command, account);
                final MathContext mc = MathContext.DECIMAL64;
                final boolean isPreMatureClosure = false;
                account.updateMaturityDateAndAmountBeforeAccountActivation(mc, isPreMatureClosure,
                        isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth);
                this.savingAccountRepository.save(account);
            }

            boolean isLinkedAccRequired = command.booleanPrimitiveValueOfParameterNamed(transferInterestToSavingsParamName);

            // Save linked account information
            final Long savingsAccountId = command.longValueOfParameterNamed(DepositsApiConstants.linkedAccountParamName);
            AccountAssociations accountAssociations = this.accountAssociationsRepository.findBySavingsIdAndType(accountId,
                    AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());
            if (savingsAccountId == null) {
                if (accountAssociations != null) {
                    if (this.fromJsonHelper.parameterExists(DepositsApiConstants.linkedAccountParamName, command.parsedJson())) {
                        this.accountAssociationsRepository.delete(accountAssociations);
                        changes.put(DepositsApiConstants.linkedAccountParamName, null);
                        if (isLinkedAccRequired) {
                            this.depositAccountDataValidator.throwLinkedAccountRequiredError();
                        }
                    }
                } else if (isLinkedAccRequired) {
                    this.depositAccountDataValidator.throwLinkedAccountRequiredError();
                }
            } else {
                boolean isModified = false;
                if (accountAssociations == null) {
                    isModified = true;
                } else {
                    final SavingsAccount savingsAccount = accountAssociations.getLinkedSavingsAccount();
                    if (savingsAccount == null || savingsAccount.getId() != savingsAccountId) {
                        isModified = true;
                    }
                }
                if (isModified) {
                    final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(savingsAccountId,
                            DepositAccountType.SAVINGS_DEPOSIT);
                    this.depositAccountDataValidator.validatelinkedSavingsAccount(savingsAccount, account);
                    if (accountAssociations == null) {
                        boolean isActive = true;
                        accountAssociations = AccountAssociations.builder()
                            .linkedSavingsAccount(account)
                            .savingsAccount(savingsAccount)
                            .associationType(AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue())
                            .active(isActive)
                            .build();
                    } else {
                        accountAssociations.setLinkedSavingsAccount(savingsAccount);
                    }
                    changes.put(DepositsApiConstants.linkedAccountParamName, savingsAccountId);
                    this.accountAssociationsRepository.save(accountAssociations);
                }
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(accountId) //
                    .officeId(account.officeId()) //
                    .clientId(account.clientId()) //
                    .groupId(account.groupId()) //
                    .savingsId(accountId) //
                    .changes(changes) //
                    .build();
        } catch (final DataAccessException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.builder().resourceId(-1L).build();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
        	return CommandProcessingResult.builder().resourceId(-1L).build();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult modifyRDApplication(final Long accountId, final JsonCommand command) {
        try {
            this.depositAccountDataValidator.validateRecurringDepositForUpdate(command.json());

            final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                    .isSavingsInterestPostingAtCurrentPeriodEnd();
            final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

            final Map<String, Object> changes = new LinkedHashMap<>(20);

            final RecurringDepositAccount account = (RecurringDepositAccount) this.depositAccountAssembler.assembleFrom(accountId,
                    DepositAccountType.RECURRING_DEPOSIT);
            checkClientOrGroupActive(account);
            account.modifyApplication(command, changes);
            account.validateNewApplicationState(DateUtils.getLocalDateOfTenant(), DepositAccountType.RECURRING_DEPOSIT.resourceName());

            if (!changes.isEmpty()) {
                updateFDAndRDCommonChanges(changes, command, account);
                final MathContext mc = MathContext.DECIMAL64;
                final CalendarInstance calendarInstance = this.calendarInstanceRepository.findByEntityIdAndEntityTypeIdAndCalendarTypeId(
                        accountId, CalendarEntityType.SAVINGS.getValue(), CalendarType.COLLECTION.getValue());
                final Calendar calendar = calendarInstance.getCalendar();
                final PeriodFrequencyType frequencyType = CalendarFrequencyType.from(CalendarUtils.getFrequency(calendar.getRecurrence()));
                Integer frequency = CalendarUtils.getInterval(calendar.getRecurrence());
                frequency = frequency == -1 ? 1 : frequency;
                account.generateSchedule(frequencyType, frequency, calendar);
                final boolean isPreMatureClosure = false;
                account.updateMaturityDateAndAmount(mc, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                        financialYearBeginningMonth);
                account.validateApplicableInterestRate();
                this.savingAccountRepository.save(account);

            }

            // update calendar details
            if (!account.isCalendarInherited()) {
                final LocalDate calendarStartDate = account.depositStartDate();
                final Integer frequencyType = command.integerValueSansLocaleOfParameterNamed(recurringFrequencyTypeParamName);
                final PeriodFrequencyType periodFrequencyType = PeriodFrequencyType.fromInt(frequencyType);
                final Integer frequency = command.integerValueSansLocaleOfParameterNamed(recurringFrequencyParamName);
                final Integer repeatsOnDay = calendarStartDate.getDayOfWeek();

                CalendarInstance calendarInstance = this.calendarInstanceRepository.findByEntityIdAndEntityTypeIdAndCalendarTypeId(
                        accountId, CalendarEntityType.SAVINGS.getValue(), CalendarType.COLLECTION.getValue());
                Calendar calendar = calendarInstance.getCalendar();
                updateRepeatingCalendar(calendar, calendarStartDate, CalendarFrequencyType.from(periodFrequencyType), frequency,
                        repeatsOnDay, null);
                this.calendarInstanceRepository.save(calendarInstance);
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(accountId) //
                    .officeId(account.officeId()) //
                    .clientId(account.clientId()) //
                    .groupId(account.groupId()) //
                    .savingsId(accountId) //
                    .changes(changes) //
                    .build();
        } catch (final DataAccessException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.builder().resourceId(-1L).build();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
        	return CommandProcessingResult.builder().resourceId(-1L).build();
        }
    }

    private Map<String, Object> updateRepeatingCalendar(final Calendar calendar, final LocalDate calendarStartDate, final CalendarFrequencyType frequencyType,
                                                       final Integer interval, final Integer repeatsOnDay, final Integer repeatsOnNthDay) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        if (calendarStartDate != null & calendar.getStartDate() != null) {
            if (!calendarStartDate.equals(calendar.getStartDateLocalDate())) {
                actualChanges.put("startDate", calendarStartDate);
                calendar.setStartDate(calendarStartDate.toDate());
            }
        }

        final String newRecurrence = Calendar.toRecurrence(frequencyType, interval, repeatsOnDay, repeatsOnNthDay);
        if (!StringUtils.isBlank(calendar.getRecurrence()) && !newRecurrence.equalsIgnoreCase(calendar.getRecurrence())) {
            actualChanges.put("recurrence", newRecurrence);
            calendar.setRecurrence(newRecurrence);
        }
        return actualChanges;
    }

    private void updateFDAndRDCommonChanges(final Map<String, Object> changes, final JsonCommand command, final SavingsAccount account) {

        if (changes.containsKey(SavingsApiConstants.clientIdParamName)) {
            final Long clientId = command.longValueOfParameterNamed(SavingsApiConstants.clientIdParamName);
            if (clientId != null) {
                final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
                if (client.isNotActive()) { throw new ClientNotActiveException(clientId); }
                account.update(client);
            } else {
                final Client client = null;
                account.update(client);
            }
        }

        if (changes.containsKey(SavingsApiConstants.groupIdParamName)) {
            final Long groupId = command.longValueOfParameterNamed(SavingsApiConstants.groupIdParamName);
            if (groupId != null) {
                final Group group = this.groupRepository.findById(groupId)
                        .orElseThrow(() -> new GroupNotFoundException(groupId));
                if (group.isNotActive()) {
                    if (group.isCenter()) { throw new CenterNotActiveException(groupId); }
                    throw new GroupNotActiveException(groupId);
                }
                account.update(group);
            } else {
                final Group group = null;
                account.update(group);
            }
        }

        if (changes.containsKey(SavingsApiConstants.productIdParamName)) {
            final Long productId = command.longValueOfParameterNamed(SavingsApiConstants.productIdParamName);
            final SavingsProduct product = this.savingsProductRepository.findById(productId)
                    .orElseThrow(() -> new SavingsProductNotFoundException(productId));

            account.update(product);
        }

        if (changes.containsKey(SavingsApiConstants.fieldOfficerIdParamName)) {
            final Long fieldOfficerId = command.longValueOfParameterNamed(SavingsApiConstants.fieldOfficerIdParamName);
            Staff fieldOfficer = null;
            if (fieldOfficerId != null) {
                fieldOfficer = this.staffRepository.findOneWithNotFoundDetection(fieldOfficerId);
            } else {
                changes.put(SavingsApiConstants.fieldOfficerIdParamName, "");
            }
            account.update(fieldOfficer);
        }

        if (changes.containsKey("charges")) {
            final Set<SavingsAccountCharge> charges = this.savingsAccountChargeAssembler.fromParsedJson(command.parsedJson(), account
                    .getCurrency().getCode());
            final boolean updated = account.update(charges);
            if (!updated) {
                changes.remove("charges");
            }
        }

    }

    @Transactional
    @Override
    public CommandProcessingResult deleteApplication(final Long savingsId, final DepositAccountType depositAccountType) {

        final SavingsAccount account = this.depositAccountAssembler.assembleFrom(savingsId, depositAccountType);
        checkClientOrGroupActive(account);

        if (account.isNotSubmittedAndPendingApproval()) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(depositAccountType
                    .resourceName() + DepositsApiConstants.deleteApplicationAction);

            baseDataValidator.reset().parameter(DepositsApiConstants.activatedOnDateParamName)
                    .failWithCodeNoParameterAddedToErrorCode("not.in.submittedandpendingapproval.state");

            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
        }

        final List<Note> relatedNotes = this.noteRepository.findBySavingsAccountId(savingsId);
        this.noteRepository.deleteInBatch(relatedNotes);

        this.savingAccountRepository.delete(account);

        return CommandProcessingResult.builder() //
                .resourceId(savingsId) //
                .officeId(account.officeId()) //
                .clientId(account.clientId()) //
                .groupId(account.groupId()) //
                .savingsId(savingsId) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult approveApplication(final Long savingsId, final JsonCommand command,
            final DepositAccountType depositAccountType) {

        final AppUser currentUser = this.context.authenticatedUser();

        this.savingsAccountApplicationTransitionApiJsonValidator.validateApproval(command.json());

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(savingsId, depositAccountType);
        checkClientOrGroupActive(savingsAccount);

        final Map<String, Object> changes = savingsAccount.approveApplication(currentUser, command, DateUtils.getLocalDateOfTenant());
        if (!changes.isEmpty()) {
            this.savingAccountRepository.save(savingsAccount);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.savingNote(savingsAccount, noteText);
                changes.put("note", noteText);
                this.noteRepository.save(note);
            }
        }

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(savingsId) //
                .officeId(savingsAccount.officeId()) //
                .clientId(savingsAccount.clientId()) //
                .groupId(savingsAccount.groupId()) //
                .savingsId(savingsId) //
                .changes(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult undoApplicationApproval(final Long savingsId, final JsonCommand command,
            final DepositAccountType depositAccountType) {

        this.context.authenticatedUser();

        this.savingsAccountApplicationTransitionApiJsonValidator.validateForUndo(command.json());

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(savingsId, depositAccountType);
        checkClientOrGroupActive(savingsAccount);

        final Map<String, Object> changes = savingsAccount.undoApplicationApproval();
        if (!changes.isEmpty()) {
            this.savingAccountRepository.save(savingsAccount);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.savingNote(savingsAccount, noteText);
                changes.put("note", noteText);
                this.noteRepository.save(note);
            }
        }

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(savingsId) //
                .officeId(savingsAccount.officeId()) //
                .clientId(savingsAccount.clientId()) //
                .groupId(savingsAccount.groupId()) //
                .savingsId(savingsId) //
                .changes(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult rejectApplication(final Long savingsId, final JsonCommand command,
            final DepositAccountType depositAccountType) {

        final AppUser currentUser = this.context.authenticatedUser();

        this.savingsAccountApplicationTransitionApiJsonValidator.validateRejection(command.json());

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(savingsId, depositAccountType);
        checkClientOrGroupActive(savingsAccount);

        final Map<String, Object> changes = savingsAccount.rejectApplication(currentUser, command, DateUtils.getLocalDateOfTenant());
        if (!changes.isEmpty()) {
            this.savingAccountRepository.save(savingsAccount);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.savingNote(savingsAccount, noteText);
                changes.put("note", noteText);
                this.noteRepository.save(note);
            }
        }

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(savingsId) //
                .officeId(savingsAccount.officeId()) //
                .clientId(savingsAccount.clientId()) //
                .groupId(savingsAccount.groupId()) //
                .savingsId(savingsId) //
                .changes(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult applicantWithdrawsFromApplication(final Long savingsId, final JsonCommand command,
            final DepositAccountType depositAccountType) {
        final AppUser currentUser = this.context.authenticatedUser();

        this.savingsAccountApplicationTransitionApiJsonValidator.validateApplicantWithdrawal(command.json());

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(savingsId, depositAccountType);
        checkClientOrGroupActive(savingsAccount);

        final Map<String, Object> changes = savingsAccount.applicantWithdrawsFromApplication(currentUser, command,
                DateUtils.getLocalDateOfTenant());
        if (!changes.isEmpty()) {
            this.savingAccountRepository.save(savingsAccount);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.savingNote(savingsAccount, noteText);
                changes.put("note", noteText);
                this.noteRepository.save(note);
            }
        }

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(savingsId) //
                .officeId(savingsAccount.officeId()) //
                .clientId(savingsAccount.clientId()) //
                .groupId(savingsAccount.groupId()) //
                .savingsId(savingsId) //
                .changes(changes) //
                .build();
    }

    private void checkClientOrGroupActive(final SavingsAccount account) {
        final Client client = account.getClient();
        if (client != null) {
            if (client.isNotActive()) { throw new ClientNotActiveException(client.getId()); }
        }
        final Group group = account.group();
        if (group != null) {
            if (group.isNotActive()) {
                if (group.isCenter()) { throw new CenterNotActiveException(group.getId()); }
                throw new GroupNotActiveException(group.getId());
            }
        }
    }

    private Map<BusinessEventNotificationConstants.BUSINESS_ENTITY, Object> constructEntityMap(final BUSINESS_ENTITY entityEvent, Object entity) {
        Map<BusinessEventNotificationConstants.BUSINESS_ENTITY, Object> map = new HashMap<>(1);
        map.put(entityEvent, entity);
        return map;
    }
}