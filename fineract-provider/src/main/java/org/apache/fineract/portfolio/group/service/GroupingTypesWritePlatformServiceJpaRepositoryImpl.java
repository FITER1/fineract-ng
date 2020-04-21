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
package org.apache.fineract.portfolio.group.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandProcessingService;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormat;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatRepositoryWrapper;
import org.apache.fineract.infrastructure.accountnumberformat.domain.EntityAccountType;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksWritePlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.RandomPasswordGenerator;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.office.exception.InvalidOfficeException;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.*;
import org.apache.fineract.portfolio.client.domain.AccountNumberGenerator;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.service.LoanStatusMapper;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_ENTITY;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.group.api.GroupingTypesApiConstants;
import org.apache.fineract.portfolio.group.domain.*;
import org.apache.fineract.portfolio.group.exception.*;
import org.apache.fineract.portfolio.group.serialization.GroupingTypesDataValidator;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.PersistenceException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupingTypesWritePlatformServiceJpaRepositoryImpl implements GroupingTypesWritePlatformService {

    private final PlatformSecurityContext context;
    private final GroupRepositoryWrapper groupRepository;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final StaffRepositoryWrapper staffRepository;
    private final NoteRepository noteRepository;
    private final GroupLevelRepository groupLevelRepository;
    private final GroupingTypesDataValidator fromApiJsonDeserializer;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final CommandProcessingService commandProcessingService;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper;
    private final AccountNumberFormatRepositoryWrapper accountNumberFormatRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService;
    private final BusinessEventNotifierService businessEventNotifierService;

    private CommandProcessingResult createGroupingType(final JsonCommand command, final GroupTypes groupingType, final Long centerId) {
        try {
            final String accountNo = command.stringValueOfParameterNamed(GroupingTypesApiConstants.accountNoParamName);
            final String name = command.stringValueOfParameterNamed(GroupingTypesApiConstants.nameParamName);
            final String externalId = command.stringValueOfParameterNamed(GroupingTypesApiConstants.externalIdParamName);

            final AppUser currentUser = this.context.authenticatedUser();
            Long officeId = null;
            Group parentGroup = null;

            if (centerId == null) {
                officeId = command.longValueOfParameterNamed(GroupingTypesApiConstants.officeIdParamName);
            } else {
                parentGroup = this.groupRepository.findOneWithNotFoundDetection(centerId);
                officeId = parentGroup.getOfficeId();
            }

            final Office groupOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);

            final LocalDate activationDate = command.localDateValueOfParameterNamed(GroupingTypesApiConstants.activationDateParamName);
            final GroupLevel groupLevel = this.groupLevelRepository.findById(groupingType.getId()).orElse(null);

            validateOfficeOpeningDateisAfterGroupOrCenterOpeningDate(groupOffice, groupLevel, activationDate);

            Staff staff = null;
            final Long staffId = command.longValueOfParameterNamed(GroupingTypesApiConstants.staffIdParamName);
            if (staffId != null) {
                staff = this.staffRepository.findByOfficeHierarchyWithNotFoundDetection(staffId, groupOffice.getHierarchy());
            }

            final Set<Client> clientMembers = assembleSetOfClients(officeId, command);

            final Set<Group> groupMembers = assembleSetOfChildGroups(officeId, command);

            final boolean active = command.booleanPrimitiveValueOfParameterNamed(GroupingTypesApiConstants.activeParamName);
            LocalDate submittedOnDate = LocalDate.now();
            if (active && submittedOnDate.isAfter(activationDate)) {
                submittedOnDate = activationDate;
            }
            if (command.hasParameter(GroupingTypesApiConstants.submittedOnDateParamName)) {
                submittedOnDate = command.localDateValueOfParameterNamed(GroupingTypesApiConstants.submittedOnDateParamName);
            }

            List<ApiParameterError> dataValidationErrors = new ArrayList<>();

            final Group newGroup = Group.builder()
                .office(groupOffice)
                .staff(staff)
                .parent(parentGroup)
                .groupLevel(groupLevel)
                .name(name)
                .externalId(externalId)
                .status(active ? GroupingTypeStatus.ACTIVE.getValue() : GroupingTypeStatus.PENDING.getValue())
                .activationDate(active ? activationDate.toDate() : null)
                .clientMembers(clientMembers)
                .groupMembers(new ArrayList<>(groupMembers))
                .submittedOnDate(submittedOnDate.toDate())
                .submittedBy(currentUser)
                .accountNumber(StringUtils.isBlank(accountNo) ? new RandomPasswordGenerator(19).generate() : accountNo)
                .accountNumberRequiresAutoGeneration(StringUtils.isBlank(accountNo))
                .build();

            if(parentGroup!=null) {
                parentGroup.getGroupMembers().add(newGroup);
            }

            if(active) {
                newGroup.activate(currentUser, activationDate, dataValidationErrors);
            }

            newGroup.associateClients(newGroup.getClientMembers());

            newGroup.throwExceptionIfErrors(dataValidationErrors);

            boolean rollbackTransaction = false;
            if (newGroup.isActive()) {
                this.groupRepository.save(newGroup);
                // validate Group creation rules for Group
                if (newGroup.getGroupLevel().isGroup()) {
                    validateGroupRulesBeforeActivation(newGroup);
                }

                if (newGroup.getGroupLevel().isCenter()) {
                    final CommandWrapper commandWrapper = new CommandWrapperBuilder().activateCenter(null).build();
                    rollbackTransaction = this.commandProcessingService.validateCommand(commandWrapper, currentUser);
                } else {
                    final CommandWrapper commandWrapper = new CommandWrapperBuilder().activateGroup(null).build();
                    rollbackTransaction = this.commandProcessingService.validateCommand(commandWrapper, currentUser);
                }
            }

            if (!newGroup.getGroupLevel().isCenter() && newGroup.hasActiveClients()) {
                final CommandWrapper commandWrapper = new CommandWrapperBuilder().associateClientsToGroup(newGroup.getId()).build();
                rollbackTransaction = this.commandProcessingService.validateCommand(commandWrapper, currentUser);
            }

            // pre-save to generate id for use in group hierarchy
            this.groupRepository.save(newGroup);

            /*
             * Generate hierarchy for a new center/group and all the child
             * groups if they exist
             */
            newGroup.generateHierarchy();

            /* Generate account number if required */
            generateAccountNumberIfRequired(newGroup);

            this.groupRepository.saveAndFlush(newGroup);
            newGroup.captureStaffHistoryDuringCenterCreation(staff, activationDate);

            if (newGroup.getGroupLevel().isGroup()) {
                if (command.parameterExists(GroupingTypesApiConstants.datatables)) {
                    this.entityDatatableChecksWritePlatformService.saveDatatables(StatusEnum.CREATE.getCode().longValue(),
                            EntityTables.GROUP.getName(), newGroup.getId(), null,
                            command.arrayOfParameterNamed(GroupingTypesApiConstants.datatables));
                }

                this.entityDatatableChecksWritePlatformService.runTheCheck(newGroup.getId(), EntityTables.GROUP.getName(),
                        StatusEnum.CREATE.getCode().longValue(), EntityTables.GROUP.getForeignKeyColumnNameOnDatatable());
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .officeId(groupOffice.getId()) //
                    .groupId(newGroup.getId()) //
                    .resourceId(newGroup.getId()) //
                    .rollbackTransaction(rollbackTransaction)//
                    .build();

        } catch (final DataIntegrityViolationException dve) {
            handleGroupDataIntegrityIssues(command, dve.getMostSpecificCause(), dve, groupingType);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            handleGroupDataIntegrityIssues(command, throwable, dve, groupingType);
         	return new CommandProcessingResult();
        }
    }

    private void generateAccountNumberIfRequired(Group newGroup){
    	if (newGroup.isAccountNumberRequiresAutoGeneration()) {
        	EntityAccountType entityAccountType = null;
        	AccountNumberFormat accountNumberFormat = null;
        	if(newGroup.getGroupLevel().isCenter()){
            	entityAccountType = EntityAccountType.CENTER;
            	accountNumberFormat = this.accountNumberFormatRepository.findByAccountType(entityAccountType);
                newGroup.setAccountNumber(this.accountNumberGenerator.generateCenterAccountNumber(newGroup, accountNumberFormat));
                newGroup.setAccountNumberRequiresAutoGeneration(false);
        	}else {
            	entityAccountType = EntityAccountType.GROUP;
            	accountNumberFormat = this.accountNumberFormatRepository.findByAccountType(entityAccountType);
                newGroup.setAccountNumber(this.accountNumberGenerator.generateCenterAccountNumber(newGroup, accountNumberFormat));
                newGroup.setAccountNumberRequiresAutoGeneration(false);
        	}
            
        }
    }
    @Transactional
    @Override
    public CommandProcessingResult createCenter(final JsonCommand command) {

        this.fromApiJsonDeserializer.validateForCreateCenter(command);

        final Long centerId = null;

        CommandProcessingResult commandProcessingResult = createGroupingType(command, GroupTypes.CENTER, centerId);

        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.CENTERS_CREATE,
                constructEntityMap(BUSINESS_ENTITY.GROUP, commandProcessingResult));

        return commandProcessingResult;
    }

    @Transactional
    @Override
    public CommandProcessingResult createGroup(final Long centerId, final JsonCommand command) {

        if (centerId != null) {
            this.fromApiJsonDeserializer.validateForCreateCenterGroup(command);
        } else {
            this.fromApiJsonDeserializer.validateForCreateGroup(command);
        }

        CommandProcessingResult commandProcessingResult = createGroupingType(command, GroupTypes.GROUP, centerId);

        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BusinessEventNotificationConstants.BUSINESS_EVENTS.GROUPS_CREATE,
                constructEntityMap(BUSINESS_ENTITY.GROUP, commandProcessingResult));

        return commandProcessingResult;
    }

    @Transactional
    @Override
    public CommandProcessingResult activateGroupOrCenter(final Long groupId, final JsonCommand command) {

        try {
            this.fromApiJsonDeserializer.validateForActivation(command, GroupingTypesApiConstants.GROUP_RESOURCE_NAME);

            final AppUser currentUser = this.context.authenticatedUser();

            final Group group = this.groupRepository.findOneWithNotFoundDetection(groupId);

            if (group.getGroupLevel().isGroup()) {
                validateGroupRulesBeforeActivation(group);
            }

            final LocalDate activationDate = command.localDateValueOfParameterNamed("activationDate");

            validateOfficeOpeningDateisAfterGroupOrCenterOpeningDate(group.getOffice(), group.getGroupLevel(), activationDate);

            group.activate(currentUser, activationDate);

            this.groupRepository.saveAndFlush(group);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .officeId(group.getOfficeId()) //
                    .groupId(groupId) //
                    .resourceId(groupId) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleGroupDataIntegrityIssues(command, dve.getMostSpecificCause(), dve, GroupTypes.GROUP);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            handleGroupDataIntegrityIssues(command, throwable, dve, GroupTypes.GROUP);
         	return new CommandProcessingResult();
        }
    }

    private void validateGroupRulesBeforeActivation(final Group group) {
        Integer minClients = configurationDomainService.retrieveMinAllowedClientsInGroup();
        Integer maxClients = configurationDomainService.retrieveMaxAllowedClientsInGroup();
        boolean isGroupClientCountValid = group.isGroupsClientCountWithinMinMaxRange(minClients, maxClients);
        if (!isGroupClientCountValid) { throw new GroupMemberCountNotInPermissibleRangeException(group.getId(), minClients, maxClients); }
        entityDatatableChecksWritePlatformService.runTheCheck(group.getId(), EntityTables.GROUP.getName(),
                StatusEnum.ACTIVATE.getCode().longValue(), EntityTables.GROUP.getForeignKeyColumnNameOnDatatable());
    }

    public void validateGroupRulesBeforeClientAssociation(final Group group) {
        Integer minClients = configurationDomainService.retrieveMinAllowedClientsInGroup();
        Integer maxClients = configurationDomainService.retrieveMaxAllowedClientsInGroup();
        boolean isGroupClientCountValid = group.isGroupsClientCountWithinMaxRange(maxClients);
        if (!isGroupClientCountValid) { throw new GroupMemberCountNotInPermissibleRangeException(group.getId(), minClients, maxClients); }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateCenter(final Long centerId, final JsonCommand command) {

        this.fromApiJsonDeserializer.validateForUpdateCenter(command, centerId);

        return updateGroupingType(centerId, command, GroupTypes.CENTER);
    }

    @Transactional
    @Override
    public CommandProcessingResult updateGroup(final Long groupId, final JsonCommand command) {

        this.fromApiJsonDeserializer.validateForUpdateGroup(command, groupId);

        return updateGroupingType(groupId, command, GroupTypes.GROUP);
    }

    private CommandProcessingResult updateGroupingType(final Long groupId, final JsonCommand command, final GroupTypes groupingType) {

        try {
            this.context.authenticatedUser();
            final Group groupForUpdate = this.groupRepository.findOneWithNotFoundDetection(groupId);
            final Long officeId = groupForUpdate.getOfficeId();
            final Office groupOffice = groupForUpdate.getOffice();
            final String groupHierarchy = groupOffice.getHierarchy();

            this.context.validateAccessRights(groupHierarchy);

            final LocalDate activationDate = command.localDateValueOfParameterNamed(GroupingTypesApiConstants.activationDateParamName);

            validateOfficeOpeningDateisAfterGroupOrCenterOpeningDate(groupOffice, groupForUpdate.getGroupLevel(), activationDate);

            final Map<String, Object> actualChanges = update(groupForUpdate, command);

            if (actualChanges.containsKey(GroupingTypesApiConstants.staffIdParamName)) {
                final Long newValue = command.longValueOfParameterNamed(GroupingTypesApiConstants.staffIdParamName);

                Staff newStaff = null;
                if (newValue != null) {
                    newStaff = this.staffRepository.findByOfficeHierarchyWithNotFoundDetection(newValue, groupHierarchy);
                }
                groupForUpdate.updateStaff(newStaff);
            }

            final GroupLevel groupLevel = this.groupLevelRepository.findById(groupForUpdate.getGroupLevel().getId()).orElse(null);

            /*
             * Ignoring parentId param, if group for update is super parent.
             * TODO Need to check: Ignoring is correct or need throw unsupported
             * param
             */
            if (!groupLevel.isSuperParent()) {

                Long parentId = null;
                final Group presentParentGroup = groupForUpdate.getParent();

                if (presentParentGroup != null) {
                    parentId = presentParentGroup.getId();
                }

                if (command.isChangeInLongParameterNamed(GroupingTypesApiConstants.centerIdParamName, parentId)) {

                    final Long newValue = command.longValueOfParameterNamed(GroupingTypesApiConstants.centerIdParamName);
                    actualChanges.put(GroupingTypesApiConstants.centerIdParamName, newValue);
                    Group newParentGroup = null;
                    if (newValue != null) {
                        newParentGroup = this.groupRepository.findOneWithNotFoundDetection(newValue);

                        if (!newParentGroup.isOfficeIdentifiedBy(officeId)) {
                            final String errorMessage = "Group and parent group must have the same office";
                            throw new InvalidOfficeException("group", "attach.to.parent.group", errorMessage);
                        }
                        /*
                         * If Group is not super parent then validate group
                         * level's parent level is same as group parent's level
                         * this check makes sure new group is added at immediate
                         * next level in hierarchy
                         */

                        if (!groupForUpdate.getGroupLevel().getParentId().equals(newParentGroup.getGroupLevel().getId())) {
                            final String errorMessage = "Parent group's level is  not equal to child level's parent level ";
                            throw new InvalidGroupLevelException("add", "invalid.level", errorMessage);
                        }
                    }

                    groupForUpdate.setParent(newParentGroup);

                    // Parent has changed, re-generate 'Hierarchy' as parent is
                    // changed
                    groupForUpdate.generateHierarchy();

                }
            }

            /*
             * final Set<Client> clientMembers = assembleSetOfClients(officeId,
             * command); List<String> changes =
             * groupForUpdate.updateClientMembersIfDifferent(clientMembers); if
             * (!changes.isEmpty()) {
             * actualChanges.put(GroupingTypesApiConstants
             * .clientMembersParamName, changes); }
             */

            this.groupRepository.saveAndFlush(groupForUpdate);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .officeId(groupForUpdate.getOfficeId()) //
                    .groupId(groupForUpdate.getId()) //
                    .resourceId(groupForUpdate.getId()) //
                    .changes(actualChanges) //
                    .build();

        } catch (final DataIntegrityViolationException dve) {
            handleGroupDataIntegrityIssues(command, dve.getMostSpecificCause(), dve, groupingType);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            handleGroupDataIntegrityIssues(command, throwable, dve, groupingType);
         	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult unassignGroupOrCenterStaff(final Long grouptId, final JsonCommand command) {

        this.context.authenticatedUser();

        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        this.fromApiJsonDeserializer.validateForUnassignStaff(command.json());
        final Group groupForUpdate = this.groupRepository.findOneWithNotFoundDetection(grouptId);
        final Staff presentStaff = groupForUpdate.getStaff();
        Long presentStaffId = null;
        if (presentStaff == null) { throw new GroupHasNoStaffException(grouptId); }
        presentStaffId = presentStaff.getId();
        final String staffIdParamName = "staffId";
        if (!command.isChangeInLongParameterNamed(staffIdParamName, presentStaffId)) {
            groupForUpdate.unassignStaff();
        }
        this.groupRepository.saveAndFlush(groupForUpdate);

        actualChanges.put(staffIdParamName, null);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .officeId(groupForUpdate.getId()) //
                .groupId(groupForUpdate.getOfficeId()) //
                .resourceId(groupForUpdate.getId()) //
                .changes(actualChanges) //
                .build();

    }

    @Override
    public CommandProcessingResult assignGroupOrCenterStaff(final Long groupId, final JsonCommand command) {

        this.context.authenticatedUser();

        final Map<String, Object> actualChanges = new LinkedHashMap<>(5);

        this.fromApiJsonDeserializer.validateForAssignStaff(command.json());

        final Group groupForUpdate = this.groupRepository.findOneWithNotFoundDetection(groupId);

        Staff staff = null;
        final Long staffId = command.longValueOfParameterNamed(GroupingTypesApiConstants.staffIdParamName);
        final boolean inheritStaffForClientAccounts = command
                .booleanPrimitiveValueOfParameterNamed(GroupingTypesApiConstants.inheritStaffForClientAccounts);
        staff = this.staffRepository.findByOfficeHierarchyWithNotFoundDetection(staffId, groupForUpdate.getOffice().getHierarchy());
        groupForUpdate.updateStaff(staff);

        if (inheritStaffForClientAccounts) {
            LocalDate loanOfficerReassignmentDate = LocalDate.now();
            /*
             * update loan officer for client and update loan officer for
             * clients loans and savings
             */
            Set<Client> clients = groupForUpdate.getClientMembers();
            if (clients != null) {
                for (Client client : clients) {
                    client.setStaff(staff);
                    if (this.loanRepositoryWrapper.doNonClosedLoanAccountsExistForClient(client.getId())) {
                        for (final Loan loan : this.loanRepositoryWrapper.findLoanByClientId(client.getId())) {
                            if (loan.isDisbursed() && !loan.isClosed()) {
                                loan.reassignLoanOfficer(staff, loanOfficerReassignmentDate);
                            }
                        }
                    }
                    if (this.savingsAccountRepositoryWrapper.doNonClosedSavingAccountsExistForClient(client.getId())) {
                        for (final SavingsAccount savingsAccount : this.savingsAccountRepositoryWrapper
                                .findSavingAccountByClientId(client.getId())) {
                            if (!savingsAccount.isClosed()) {
                                savingsAccount.reassignSavingsOfficer(staff, loanOfficerReassignmentDate);
                            }
                        }
                    }
                }
            }
        }
        this.groupRepository.saveAndFlush(groupForUpdate);

        actualChanges.put(GroupingTypesApiConstants.staffIdParamName, staffId);

        return CommandProcessingResult.builder() //
                .officeId(groupForUpdate.getOfficeId()) //
                .resourceId(groupForUpdate.getId()) //
                .groupId(groupId) //
                .changes(actualChanges) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteGroup(final Long groupId) {
        try {

            final Group groupForDelete = this.groupRepository.findOneWithNotFoundDetection(groupId);

            if (!GroupingTypeStatus.fromInt(groupForDelete.getStatus()).isPending()) {
                throw new GroupMustBePendingToBeDeletedException(groupId);
            }

            final List<Note> relatedNotes = this.noteRepository.findByGroupId(groupId);
            this.noteRepository.deleteInBatch(relatedNotes);

            this.groupRepository.delete(groupForDelete);
            this.groupRepository.flush();
            return CommandProcessingResult.builder() //
                    .officeId(groupForDelete.getId()) //
                    .groupId(groupForDelete.getOfficeId()) //
                    .resourceId(groupForDelete.getId()) //
                    .build();
        } catch (DataIntegrityViolationException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            log.error(throwable.getMessage());
            throw new PlatformDataIntegrityException("error.msg.group.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.");
        }
    }

    @Override
    public CommandProcessingResult closeGroup(final Long groupId, final JsonCommand command) {
        this.fromApiJsonDeserializer.validateForGroupClose(command);
        final Group group = this.groupRepository.findOneWithNotFoundDetection(groupId);
        final LocalDate closureDate = command.localDateValueOfParameterNamed(GroupingTypesApiConstants.closureDateParamName);
        final Long closureReasonId = command.longValueOfParameterNamed(GroupingTypesApiConstants.closureReasonIdParamName);

        final AppUser currentUser = this.context.authenticatedUser();

        final CodeValue closureReason = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                GroupingTypesApiConstants.GROUP_CLOSURE_REASON, closureReasonId);

        if (group.hasActiveClients()) {
            final String errorMessage = group.getGroupLevel().getLevelName()
                    + " cannot be closed because of active clients associated with it.";
            throw new InvalidGroupStateTransitionException(group.getGroupLevel().getLevelName(), "close", "active.clients.exist",
                    errorMessage);
        }

        validateLoansAndSavingsForGroupOrCenterClose(group, closureDate);

        entityDatatableChecksWritePlatformService.runTheCheck(groupId, EntityTables.GROUP.getName(),
                StatusEnum.CLOSE.getCode().longValue(),EntityTables.GROUP.getForeignKeyColumnNameOnDatatable());

        group.close(currentUser, closureReason, closureDate);

        this.groupRepository.saveAndFlush(group);

        return CommandProcessingResult.builder() //
                .groupId(groupId) //
                .resourceId(groupId) //
                .build();
    }

    private Map<String, Object> update(final Group group, final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        if (command.isChangeInIntegerParameterNamed(GroupingTypesApiConstants.statusParamName, group.getStatus())) {
            final Integer newValue = command.integerValueOfParameterNamed(GroupingTypesApiConstants.statusParamName);
            actualChanges.put(GroupingTypesApiConstants.statusParamName, GroupingTypeEnumerations.status(newValue));
            group.setStatus(GroupingTypeStatus.fromInt(newValue).getValue());
        }

        if (command.isChangeInStringParameterNamed(GroupingTypesApiConstants.externalIdParamName, group.getExternalId())) {
            final String newValue = command.stringValueOfParameterNamed(GroupingTypesApiConstants.externalIdParamName);
            actualChanges.put(GroupingTypesApiConstants.externalIdParamName, newValue);
            group.setExternalId(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInLongParameterNamed(GroupingTypesApiConstants.officeIdParamName, group.getOffice()!=null ? group.getOffice().getId() : null)) {
            final Long newValue = command.longValueOfParameterNamed(GroupingTypesApiConstants.officeIdParamName);
            actualChanges.put(GroupingTypesApiConstants.officeIdParamName, newValue);
        }

        if (command.isChangeInLongParameterNamed(GroupingTypesApiConstants.staffIdParamName, group.getStaff()!=null ? group.getStaff().getId() : null)) {
            final Long newValue = command.longValueOfParameterNamed(GroupingTypesApiConstants.staffIdParamName);
            actualChanges.put(GroupingTypesApiConstants.staffIdParamName, newValue);
        }

        if (command.isChangeInStringParameterNamed(GroupingTypesApiConstants.nameParamName, group.getName())) {
            final String newValue = command.stringValueOfParameterNamed(GroupingTypesApiConstants.nameParamName);
            actualChanges.put(GroupingTypesApiConstants.nameParamName, newValue);
            group.setName(StringUtils.defaultIfEmpty(newValue, null));
        }

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        if (command.isChangeInLocalDateParameterNamed(GroupingTypesApiConstants.activationDateParamName, group.getActivationLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(GroupingTypesApiConstants.activationDateParamName);
            actualChanges.put(GroupingTypesApiConstants.activationDateParamName, valueAsInput);
            actualChanges.put(GroupingTypesApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(GroupingTypesApiConstants.localeParamName, localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(GroupingTypesApiConstants.activationDateParamName);
            if (newValue != null) {
                group.setActivationDate(newValue.toDate());
            }
        }

        if (command.isChangeInStringParameterNamed(GroupingTypesApiConstants.accountNoParamName, group.getAccountNumber())) {
            final String newValue = command.stringValueOfParameterNamed(GroupingTypesApiConstants.accountNoParamName);
            actualChanges.put(GroupingTypesApiConstants.accountNoParamName, newValue);
            group.setAccountNumber(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInLocalDateParameterNamed(GroupingTypesApiConstants.submittedOnDateParamName, group.getSubmittedOnDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(GroupingTypesApiConstants.submittedOnDateParamName);
            actualChanges.put(GroupingTypesApiConstants.submittedOnDateParamName, valueAsInput);
            actualChanges.put(GroupingTypesApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(GroupingTypesApiConstants.localeParamName, localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(GroupingTypesApiConstants.submittedOnDateParamName);
            if (newValue != null) {
                group.setSubmittedOnDate(newValue.toDate());
            }
        }

        return actualChanges;
    }

    private void validateLoansAndSavingsForGroupOrCenterClose(final Group groupOrCenter, final LocalDate closureDate) {
        final Collection<Loan> groupLoans = this.loanRepositoryWrapper.findByGroupId(groupOrCenter.getId());
        for (final Loan loan : groupLoans) {
            final LoanStatusMapper loanStatus = new LoanStatusMapper(loan.status().getValue());
            if (loanStatus.isOpen()) {
                final String errorMessage = groupOrCenter.getGroupLevel().getLevelName() + " cannot be closed because of non-closed loans.";
                throw new InvalidGroupStateTransitionException(groupOrCenter.getGroupLevel().getLevelName(), "close", "loan.not.closed",
                        errorMessage);
            } else if (loanStatus.isClosed() && loan.getClosedOnDate().after(closureDate.toDate())) {
                final String errorMessage = groupOrCenter.getGroupLevel().getLevelName()
                        + "closureDate cannot be before the loan closedOnDate.";
                throw new InvalidGroupStateTransitionException(groupOrCenter.getGroupLevel().getLevelName(), "close",
                        "date.cannot.before.loan.closed.date", errorMessage, closureDate, loan.getClosedOnDate());
            } else if (loanStatus.isPendingApproval()) {
                final String errorMessage = groupOrCenter.getGroupLevel().getLevelName() + " cannot be closed because of non-closed loans.";
                throw new InvalidGroupStateTransitionException(groupOrCenter.getGroupLevel().getLevelName(), "close", "loan.not.closed",
                        errorMessage);
            } else if (loanStatus.isAwaitingDisbursal()) {
                final String errorMessage = "Group cannot be closed because of non-closed loans.";
                throw new InvalidGroupStateTransitionException(groupOrCenter.getGroupLevel().getLevelName(), "close", "loan.not.closed",
                        errorMessage);
            }
        }

        final List<SavingsAccount> groupSavingAccounts = this.savingsAccountRepositoryWrapper.findByGroupId(groupOrCenter.getId());

        for (final SavingsAccount saving : groupSavingAccounts) {
            if (saving.isActive() || saving.isSubmittedAndPendingApproval() || saving.isApproved()) {
                final String errorMessage = groupOrCenter.getGroupLevel().getLevelName()
                        + " cannot be closed with active savings accounts associated.";
                throw new InvalidGroupStateTransitionException(groupOrCenter.getGroupLevel().getLevelName(), "close",
                        "savings.account.not.closed", errorMessage);
            } else if (saving.isClosed() && saving.getClosedOnDate().isAfter(closureDate)) {
                final String errorMessage = groupOrCenter.getGroupLevel().getLevelName()
                        + " closureDate cannot be before the loan closedOnDate.";
                throw new InvalidGroupStateTransitionException(groupOrCenter.getGroupLevel().getLevelName(), "close",
                        "date.cannot.before.loan.closed.date", errorMessage, closureDate, saving.getClosedOnDate());
            }
        }
    }

    @Override
    public CommandProcessingResult closeCenter(final Long centerId, final JsonCommand command) {
        this.fromApiJsonDeserializer.validateForCenterClose(command);
        final Group center = this.groupRepository.findOneWithNotFoundDetection(centerId);
        final LocalDate closureDate = command.localDateValueOfParameterNamed(GroupingTypesApiConstants.closureDateParamName);
        final Long closureReasonId = command.longValueOfParameterNamed(GroupingTypesApiConstants.closureReasonIdParamName);

        final CodeValue closureReason = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                GroupingTypesApiConstants.CENTER_CLOSURE_REASON, closureReasonId);

        final AppUser currentUser = this.context.authenticatedUser();

        boolean hasActiveGroups = center.getGroupMembers().stream()
            .filter(g -> GroupingTypeStatus.fromInt(g.getStatus()).isClosed())
            .map(g -> GroupingTypeStatus.fromInt(g.getStatus()).isClosed())
            .findFirst()
            .orElse(false);

        if (hasActiveGroups) {
            final String errorMessage = center.getGroupLevel().getLevelName() + " cannot be closed because of active groups associated with it.";
            throw new InvalidGroupStateTransitionException(center.getGroupLevel().getLevelName(), "close", "active.groups.exist", errorMessage);
        }

        validateLoansAndSavingsForGroupOrCenterClose(center, closureDate);

        entityDatatableChecksWritePlatformService.runTheCheck(centerId, EntityTables.GROUP.getName(),
                StatusEnum.ACTIVATE.getCode().longValue(), EntityTables.GROUP.getForeignKeyColumnNameOnDatatable());


        center.close(currentUser, closureReason, closureDate);

        this.groupRepository.saveAndFlush(center);

        return CommandProcessingResult.builder() //
                .resourceId(centerId) //
                .build();
    }

    private Set<Client> assembleSetOfClients(final Long groupOfficeId, final JsonCommand command) {

        final Set<Client> clientMembers = new HashSet<>();
        final String[] clientMembersArray = command.arrayValueOfParameterNamed(GroupingTypesApiConstants.clientMembersParamName);

        if (!ObjectUtils.isEmpty(clientMembersArray)) {
            for (final String clientId : clientMembersArray) {
                final Long id = Long.valueOf(clientId);
                final Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(id);
                if (!client.getOffice().getId().equals(groupOfficeId)) {
                    final String errorMessage = "Client with identifier " + clientId + " must have the same office as group.";
                    throw new InvalidOfficeException("client", "attach.to.group", errorMessage, clientId, groupOfficeId);
                }
                clientMembers.add(client);
            }
        }

        return clientMembers;
    }

    private Set<Group> assembleSetOfChildGroups(final Long officeId, final JsonCommand command) {

        final Set<Group> childGroups = new HashSet<>();
        final String[] childGroupsArray = command.arrayValueOfParameterNamed(GroupingTypesApiConstants.groupMembersParamName);

        if (!ObjectUtils.isEmpty(childGroupsArray)) {
            for (final String groupId : childGroupsArray) {
                final Long id = Long.valueOf(groupId);
                final Group group = this.groupRepository.findOneWithNotFoundDetection(id);

                if (!group.isOfficeIdentifiedBy(officeId)) {
                    final String errorMessage = "Group and child groups must have the same office.";
                    throw new InvalidOfficeException("group", "attach.to.parent.group", errorMessage);
                }

                childGroups.add(group);
            }
        }

        return childGroups;
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleGroupDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve,
            final GroupTypes groupLevel) {

        String levelName = "Invalid";
        switch (groupLevel) {
            case CENTER:
                levelName = "Center";
            break;
            case GROUP:
                levelName = "Group";
            break;
            case INVALID:
            break;
        }

        String errorMessageForUser = null;
        String errorMessageForMachine = null;

        if (realCause.getMessage().contains("'external_id'")) {

            final String externalId = command.stringValueOfParameterNamed(GroupingTypesApiConstants.externalIdParamName);
            errorMessageForUser = levelName + " with externalId `" + externalId + "` already exists.";
            errorMessageForMachine = "error.msg." + levelName.toLowerCase() + ".duplicate.externalId";
            throw new PlatformDataIntegrityException(errorMessageForMachine, errorMessageForUser,
                    GroupingTypesApiConstants.externalIdParamName, externalId);
        } else if (realCause.getMessage().contains("'name'")) {

            final String name = command.stringValueOfParameterNamed(GroupingTypesApiConstants.nameParamName);
            errorMessageForUser = levelName + " with name `" + name + "` already exists.";
            errorMessageForMachine = "error.msg." + levelName.toLowerCase() + ".duplicate.name";
            throw new PlatformDataIntegrityException(errorMessageForMachine, errorMessageForUser, GroupingTypesApiConstants.nameParamName,
                    name);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.group.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    @Override
    public CommandProcessingResult associateClientsToGroup(final Long groupId, final JsonCommand command) {

        this.fromApiJsonDeserializer.validateForAssociateClients(command.json());

        final Group groupForUpdate = this.groupRepository.findOneWithNotFoundDetection(groupId);
        final Set<Client> clientMembers = assembleSetOfClients(groupForUpdate.getOfficeId(), command);
        final Map<String, Object> actualChanges = new HashMap<>();

        final List<String> changes = groupForUpdate.associateClients(clientMembers);

        if (groupForUpdate.getGroupLevel().isGroup()) {
            validateGroupRulesBeforeClientAssociation(groupForUpdate);
        }
        if (!changes.isEmpty()) {
            actualChanges.put(GroupingTypesApiConstants.clientMembersParamName, changes);
        }

        this.groupRepository.saveAndFlush(groupForUpdate);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .officeId(groupForUpdate.getOfficeId()) //
                .groupId(groupForUpdate.getId()) //
                .resourceId(groupForUpdate.getId()) //
                .changes(actualChanges) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult disassociateClientsFromGroup(final Long groupId, final JsonCommand command) {
        this.fromApiJsonDeserializer.validateForDisassociateClients(command.json());

        final Group groupForUpdate = this.groupRepository.findOneWithNotFoundDetection(groupId);
        final Set<Client> clientMembers = assembleSetOfClients(groupForUpdate.getOfficeId(), command);

        // check if any client has got group loans
        checkForActiveJLGLoans(groupForUpdate.getId(), clientMembers);
        validateForJLGSavings(groupForUpdate.getId(), clientMembers);
        final Map<String, Object> actualChanges = new HashMap<>();

        final List<String> changes = groupForUpdate.disassociateClients(clientMembers);
        if (!changes.isEmpty()) {
            actualChanges.put(GroupingTypesApiConstants.clientMembersParamName, changes);
        }

        this.groupRepository.saveAndFlush(groupForUpdate);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .officeId(groupForUpdate.getOfficeId()) //
                .groupId(groupForUpdate.getId()) //
                .resourceId(groupForUpdate.getId()) //
                .changes(actualChanges) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult associateGroupsToCenter(final Long centerId, final JsonCommand command) {

        this.fromApiJsonDeserializer.validateForAssociateGroups(command.json());
        final Group centerForUpdate = this.groupRepository.findOneWithNotFoundDetection(centerId);
        final Set<Group> groupMembers = assembleSetOfChildGroups(centerForUpdate.getOfficeId(), command);
        checkGroupMembersMeetingSyncWithCenterMeeting(centerId, groupMembers);

        final Map<String, Object> actualChanges = new HashMap<>();

        final List<String> changes = centerForUpdate.associateGroups(groupMembers);
        if (!changes.isEmpty()) {
            actualChanges.put(GroupingTypesApiConstants.groupMembersParamName, changes);
        }

        this.groupRepository.saveAndFlush(centerForUpdate);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .officeId(centerForUpdate.getOfficeId()) //
                .groupId(centerForUpdate.getId()) //
                .resourceId(centerForUpdate.getId()) //
                .changes(actualChanges) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult disassociateGroupsToCenter(final Long centerId, final JsonCommand command) {
        this.fromApiJsonDeserializer.validateForDisassociateGroups(command.json());

        final Group centerForUpdate = this.groupRepository.findOneWithNotFoundDetection(centerId);
        final Set<Group> groupMembers = assembleSetOfChildGroups(centerForUpdate.getOfficeId(), command);

        final Map<String, Object> actualChanges = new HashMap<>();

        final List<String> changes = centerForUpdate.disassociateGroups(groupMembers);
        if (!changes.isEmpty()) {
            actualChanges.put(GroupingTypesApiConstants.clientMembersParamName, changes);
        }

        this.groupRepository.saveAndFlush(centerForUpdate);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .officeId(centerForUpdate.getOfficeId()) //
                .groupId(centerForUpdate.getId()) //
                .resourceId(centerForUpdate.getId()) //
                .changes(actualChanges) //
                .build();

    }

    @Transactional
    private void checkForActiveJLGLoans(final Long groupId, final Set<Client> clientMembers) {
        for (final Client client : clientMembers) {
            final Collection<Loan> loans = this.loanRepositoryWrapper.findActiveLoansByLoanIdAndGroupId(client.getId(), groupId);
            if (!CollectionUtils.isEmpty(loans)) {
                final String defaultUserMessage = "Client with identifier " + client.getId()
                        + " cannot be disassociated it has group loans.";
                throw new GroupAccountExistsException("disassociate", "client.has.group.loan", defaultUserMessage, client.getId(), groupId);
            }
        }
    }

    @Transactional
    private void validateForJLGSavings(final Long groupId, final Set<Client> clientMembers) {
        for (final Client client : clientMembers) {
            final Collection<SavingsAccount> savings = this.savingsAccountRepositoryWrapper.findByClientIdAndGroupId(client.getId(), groupId);
            if (!CollectionUtils.isEmpty(savings)) {
                final String defaultUserMessage = "Client with identifier " + client.getId()
                        + " cannot be disassociated it has group savings.";
                throw new GroupAccountExistsException("disassociate", "client.has.group.saving", defaultUserMessage, client.getId(),
                        groupId);
            }
        }
    }

    public void validateOfficeOpeningDateisAfterGroupOrCenterOpeningDate(final Office groupOffice, final GroupLevel groupLevel,
            final LocalDate activationDate) {
        if (activationDate != null && groupOffice.getOpeningLocalDate().isAfter(activationDate)) {
            final String levelName = groupLevel.getLevelName();
            final String errorMessage = levelName
                    + " activation date should be greater than or equal to the parent Office's creation date " + activationDate.toString();
            throw new InvalidGroupStateTransitionException(levelName.toLowerCase(), "activate.date",
                    "cannot.be.before.office.activation.date", errorMessage, activationDate, groupOffice.getOpeningLocalDate());
        }
    }

    private void checkGroupMembersMeetingSyncWithCenterMeeting(final Long centerId, final Set<Group> groupMembers) {

        /**
         * Get parent(center) calendar
         */
        Calendar ceneterCalendar = null;
        final CalendarInstance parentCalendarInstance = this.calendarInstanceRepository.findByEntityIdAndEntityTypeIdAndCalendarTypeId(
                centerId, CalendarEntityType.CENTERS.getValue(), CalendarType.COLLECTION.getValue());
        if (parentCalendarInstance != null) {
            ceneterCalendar = parentCalendarInstance.getCalendar();
        }

        for (final Group group : groupMembers) {
            /**
             * Get child(group) calendar
             */
            Calendar groupCalendar = null;
            final CalendarInstance groupCalendarInstance = this.calendarInstanceRepository.findByEntityIdAndEntityTypeIdAndCalendarTypeId(
                    group.getId(), CalendarEntityType.GROUPS.getValue(), CalendarType.COLLECTION.getValue());
            if (groupCalendarInstance != null) {
                groupCalendar = groupCalendarInstance.getCalendar();
            }

            /**
             * Group shouldn't have a meeting when no meeting attached for
             * center
             */
            if (ceneterCalendar == null && groupCalendar != null) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.center.associating.group.not.allowed.with.meeting.attached.to.group", "Group with id " + group.getId()
                                + " is already associated with meeting", group.getId());
            }
            /**
             * Group meeting recurrence should match with center meeting
             * recurrence
             */
            else if (ceneterCalendar != null && groupCalendar != null) {

                if (!ceneterCalendar.getRecurrence().equalsIgnoreCase(groupCalendar.getRecurrence())) { throw new GeneralPlatformDomainRuleException(
                        "error.msg.center.associating.group.not.allowed.with.different.meeting", "Group with id " + group.getId()
                                + " meeting recurrence doesnot matched with center meeting recurrence", group.getId()); }
            }
        }
    }

    private Map<BusinessEventNotificationConstants.BUSINESS_ENTITY, Object> constructEntityMap(final BUSINESS_ENTITY entityEvent, Object entity) {
        Map<BUSINESS_ENTITY, Object> map = new HashMap<>(1);
        map.put(entityEvent, entity);
        return map;
    }
}
