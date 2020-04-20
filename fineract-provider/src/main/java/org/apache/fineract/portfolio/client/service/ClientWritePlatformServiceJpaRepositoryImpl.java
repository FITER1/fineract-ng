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
package org.apache.fineract.portfolio.client.service;

import com.google.gson.JsonElement;
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
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksWritePlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.RandomPasswordGenerator;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.portfolio.address.service.AddressWritePlatformService;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.data.ClientDataValidator;
import org.apache.fineract.portfolio.client.domain.*;
import org.apache.fineract.portfolio.client.exception.*;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_ENTITY;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepository;
import org.apache.fineract.portfolio.group.exception.GroupMemberCountNotInPermissibleRangeException;
import org.apache.fineract.portfolio.group.exception.GroupNotFoundException;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.savings.data.SavingsAccountDataDTO;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepository;
import org.apache.fineract.portfolio.savings.exception.SavingsProductNotFoundException;
import org.apache.fineract.portfolio.savings.service.SavingsApplicationProcessWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientWritePlatformServiceJpaRepositoryImpl implements ClientWritePlatformService {

    private final PlatformSecurityContext context;
    private final ClientRepositoryWrapper clientRepository;
    private final ClientNonPersonRepositoryWrapper clientNonPersonRepository;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final NoteRepository noteRepository;
    private final GroupRepository groupRepository;
    private final ClientDataValidator fromApiJsonDeserializer;
    private final AccountNumberGenerator accountNumberGenerator;
    private final StaffRepositoryWrapper staffRepository;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final SavingsAccountRepositoryWrapper savingsRepositoryWrapper;
    private final SavingsProductRepository savingsProductRepository;
    private final SavingsApplicationProcessWritePlatformService savingsApplicationProcessWritePlatformService;
    private final CommandProcessingService commandProcessingService;
    private final ConfigurationDomainService configurationDomainService;
    private final AccountNumberFormatRepositoryWrapper accountNumberFormatRepository;
    private final FromJsonHelper fromApiJsonHelper;
    private final ConfigurationReadPlatformService configurationReadPlatformService;
    private final AddressWritePlatformService addressWritePlatformService;
    private final ClientFamilyMembersWritePlatformService clientFamilyMembersWritePlatformService;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService;

    @Transactional
    @Override
    public CommandProcessingResult deleteClient(final Long clientId) {
        try {
            final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);

            if (client.isNotPending()) { throw new ClientMustBePendingToBeDeletedException(clientId); }
            final List<Note> relatedNotes = this.noteRepository.findByClientId(clientId);
            this.noteRepository.deleteInBatch(relatedNotes);

            final ClientNonPerson clientNonPerson = this.clientNonPersonRepository.findOneByClientId(clientId);
            if (clientNonPerson != null) this.clientNonPersonRepository.delete(clientNonPerson);

            this.clientRepository.delete(client);
            this.clientRepository.flush();
            return CommandProcessingResult.builder() //
                    .officeId(client.getOffice().getId()) //
                    .clientId(clientId) //
                    .resourceId(clientId) //
                    .build();
        } catch (DataIntegrityViolationException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            log.error(throwable.getMessage());
            throw new PlatformDataIntegrityException("error.msg.client.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.");
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("external_id")) {

            final String externalId = command.stringValueOfParameterNamed("externalId");
            throw new PlatformDataIntegrityException("error.msg.client.duplicate.externalId", "Client with externalId `" + externalId
                    + "` already exists", "externalId", externalId);
        } else if (realCause.getMessage().contains("account_no_UNIQUE")) {
            final String accountNo = command.stringValueOfParameterNamed("accountNo");
            throw new PlatformDataIntegrityException("error.msg.client.duplicate.accountNo", "Client with accountNo `" + accountNo
                    + "` already exists", "accountNo", accountNo);
        } else if (realCause.getMessage().contains("mobile_no")) {
            final String mobileNo = command.stringValueOfParameterNamed("mobileNo");
            throw new PlatformDataIntegrityException("error.msg.client.duplicate.mobileNo", "Client with mobileNo `" + mobileNo
                    + "` already exists", "mobileNo", mobileNo);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.client.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    @Transactional
    @Override
    public CommandProcessingResult createClient(final JsonCommand command) {

        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

			final GlobalConfigurationPropertyData configuration = this.configurationReadPlatformService
					.retrieveGlobalConfiguration("Enable-Address");

			final Boolean isAddressEnabled = configuration.isEnabled();

			final Boolean isStaff = command.booleanObjectValueOfParameterNamed(ClientApiConstants.isStaffParamName);

            final Long officeId = command.longValueOfParameterNamed(ClientApiConstants.officeIdParamName);

            final Office clientOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);

            final Long groupId = command.longValueOfParameterNamed(ClientApiConstants.groupIdParamName);

            Group clientParentGroup = null;
            if (groupId != null) {
                clientParentGroup = this.groupRepository.findById(groupId)
                        .orElseThrow(() -> new GroupNotFoundException(groupId));
            }

            Staff staff = null;
            final Long staffId = command.longValueOfParameterNamed(ClientApiConstants.staffIdParamName);
            if (staffId != null) {
                staff = this.staffRepository.findByOfficeHierarchyWithNotFoundDetection(staffId, clientOffice.getHierarchy());
            }

            CodeValue gender = null;
            final Long genderId = command.longValueOfParameterNamed(ClientApiConstants.genderIdParamName);
            if (genderId != null) {
                gender = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.GENDER, genderId);
            }

            CodeValue clientType = null;
            final Long clientTypeId = command.longValueOfParameterNamed(ClientApiConstants.clientTypeIdParamName);
            if (clientTypeId != null) {
                clientType = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.CLIENT_TYPE,
                        clientTypeId);
            }

            CodeValue clientClassification = null;
            final Long clientClassificationId = command.longValueOfParameterNamed(ClientApiConstants.clientClassificationIdParamName);
            if (clientClassificationId != null) {
                clientClassification = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                        ClientApiConstants.CLIENT_CLASSIFICATION, clientClassificationId);
            }

           
            final Long savingsProductId = command.longValueOfParameterNamed(ClientApiConstants.savingsProductIdParamName);
            if (savingsProductId != null) {
                this.savingsProductRepository.findById(savingsProductId)
                        .orElseThrow(() -> new SavingsProductNotFoundException(savingsProductId));
            }
            
            final Integer legalFormParamValue = command.integerValueOfParameterNamed(ClientApiConstants.legalFormIdParamName);
            boolean isEntity = false;
            Integer legalFormValue = null;
            if(legalFormParamValue != null)
            {
            	LegalForm legalForm = LegalForm.fromInt(legalFormParamValue);
            	if(legalForm != null)
                {
                	legalFormValue = legalForm.getValue();
                	isEntity = legalForm.isEntity();
                }
            }
            
            final Client newClient = createNew(currentUser, clientOffice, clientParentGroup, staff, savingsProductId, gender,
                    clientType, clientClassification, legalFormValue, command);
            this.clientRepository.save(newClient);
            boolean rollbackTransaction = false;
            if (newClient.isActive()) {
                validateParentGroupRulesBeforeClientActivation(newClient);
                runEntityDatatableCheck(newClient.getId());
                final CommandWrapper commandWrapper = new CommandWrapperBuilder().activateClient(null).build();
                rollbackTransaction = this.commandProcessingService.validateCommand(commandWrapper, currentUser);
            }
			
            this.clientRepository.save(newClient);
            if (newClient.isActive()) {
                this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.CLIENTS_ACTIVATE,
                        constructEntityMap(BUSINESS_ENTITY.CLIENT, newClient));
            }
            if (newClient.isAccountNumberRequiresAutoGeneration()) {
                AccountNumberFormat accountNumberFormat = this.accountNumberFormatRepository.findByAccountType(EntityAccountType.CLIENT);
                newClient.setAccountNumber(accountNumberGenerator.generate(newClient, accountNumberFormat));
                newClient.setAccountNumberRequiresAutoGeneration(true);
                this.clientRepository.save(newClient);
            }
                        
            final Locale locale = command.extractLocale();
            final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
            CommandProcessingResult result = openSavingsAccount(newClient, fmt);
            if (result.getSavingsId() != null) {
                this.clientRepository.save(newClient);
            }

            if(isEntity) {
                extractAndCreateClientNonPerson(newClient, command);
            }

            if (isAddressEnabled) {
                this.addressWritePlatformService.addNewClientAddress(newClient, command);
            }

            if(command.arrayOfParameterNamed("familyMembers")!=null)
            {
            	this.clientFamilyMembersWritePlatformService.addClientFamilyMember(newClient, command);
            }

            if(command.parameterExists(ClientApiConstants.datatables)){
                this.entityDatatableChecksWritePlatformService.saveDatatables(StatusEnum.CREATE.getCode().longValue(),
                        EntityTables.CLIENT.getName(), newClient.getId(), null,
                        command.arrayOfParameterNamed(ClientApiConstants.datatables));
            }

            this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.CLIENTS_CREATE,
                    constructEntityMap(BUSINESS_ENTITY.CLIENT, newClient));

            this.entityDatatableChecksWritePlatformService.runTheCheck(newClient.getId(), EntityTables.CLIENT.getName(),
                    StatusEnum.CREATE.getCode().longValue(), EntityTables.CLIENT.getForeignKeyColumnNameOnDatatable());
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .officeId(clientOffice.getId()) //
                    .clientId(newClient.getId()) //
                    .groupId(groupId) //
                    .resourceId(newClient.getId()) //
                    .savingsId(result.getSavingsId())//
                    // .rollbackTransaction(rollbackTransaction)//
                    .rollbackTransaction(result.isRollbackTransaction())//
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch(final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            handleDataIntegrityIssues(command, throwable, dve);
         	return new CommandProcessingResult();
        }
    }

    /**
     * This method extracts ClientNonPerson details from Client command and creates a new ClientNonPerson record
     * @param client
     * @param command
     */
    public void extractAndCreateClientNonPerson(Client client, JsonCommand command) {
    	final JsonElement clientNonPersonElement = this.fromApiJsonHelper.parse(command.jsonFragment(ClientApiConstants.clientNonPersonDetailsParamName));

		if(clientNonPersonElement != null && !isEmpty(clientNonPersonElement)) {
			final String incorpNumber = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.incorpNumberParamName, clientNonPersonElement);
	        final String remarks = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.remarksParamName, clientNonPersonElement);
	        final LocalDate incorpValidityTill = this.fromApiJsonHelper.extractLocalDateNamed(ClientApiConstants.incorpValidityTillParamName, clientNonPersonElement);

	    	//JsonCommand clientNonPersonCommand = JsonCommand.fromExistingCommand(command, command.arrayOfParameterNamed(ClientApiConstants.clientNonPersonDetailsParamName).getAsJsonObject());
	    	CodeValue clientNonPersonConstitution = null;
	        final Long clientNonPersonConstitutionId = this.fromApiJsonHelper.extractLongNamed(ClientApiConstants.constitutionIdParamName, clientNonPersonElement);
	        if (clientNonPersonConstitutionId != null) {
	        	clientNonPersonConstitution = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.CLIENT_NON_PERSON_CONSTITUTION,
	        			clientNonPersonConstitutionId);
	        }

	        CodeValue clientNonPersonMainBusinessLine = null;
	        final Long clientNonPersonMainBusinessLineId = this.fromApiJsonHelper.extractLongNamed(ClientApiConstants.mainBusinessLineIdParamName, clientNonPersonElement);
	        if (clientNonPersonMainBusinessLineId != null) {
	        	clientNonPersonMainBusinessLine = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.CLIENT_NON_PERSON_MAIN_BUSINESS_LINE,
	        			clientNonPersonMainBusinessLineId);
	        }

	    	final ClientNonPerson newClientNonPerson = createNewNonPerson(client, clientNonPersonConstitution, clientNonPersonMainBusinessLine, incorpNumber, incorpValidityTill, remarks);

	    	this.clientNonPersonRepository.save(newClientNonPerson);
		}
    }

    public boolean isEmpty(final JsonElement element) {
    	return element.toString().trim().length()<4;
    }

    @Transactional
    @Override
    public CommandProcessingResult updateClient(final Long clientId, final JsonCommand command) {

        try {
            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            final Client clientForUpdate = this.clientRepository.findOneWithNotFoundDetection(clientId);
            final String clientHierarchy = clientForUpdate.getOffice().getHierarchy();

            this.context.validateAccessRights(clientHierarchy);

            final Map<String, Object> changes = update(clientForUpdate, command);

            if (changes.containsKey(ClientApiConstants.staffIdParamName)) {

                final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.staffIdParamName);
                Staff newStaff = null;
                if (newValue != null) {
                    newStaff = this.staffRepository.findByOfficeHierarchyWithNotFoundDetection(newValue, clientForUpdate.getOffice()
                            .getHierarchy());
                }
                clientForUpdate.setStaff(newStaff);
            }

            if (changes.containsKey(ClientApiConstants.genderIdParamName)) {

                final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.genderIdParamName);
                CodeValue gender = null;
                if (newValue != null) {
                    gender = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.GENDER, newValue);
                }
                clientForUpdate.setGender(gender);
            }

            if (changes.containsKey(ClientApiConstants.savingsProductIdParamName)) {
                if (clientForUpdate.isActive()) { throw new ClientActiveForUpdateException(clientId,
                        ClientApiConstants.savingsProductIdParamName); }
                final Long savingsProductId = command.longValueOfParameterNamed(ClientApiConstants.savingsProductIdParamName);
                if (savingsProductId != null) {
                    this.savingsProductRepository.findById(savingsProductId)
                            .orElseThrow(() -> new SavingsProductNotFoundException(savingsProductId));
                }
                clientForUpdate.setSavingsProductId(savingsProductId);
            }

            if (changes.containsKey(ClientApiConstants.genderIdParamName)) {
                final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.genderIdParamName);
                CodeValue newCodeVal = null;
                if (newValue != null) {
                    newCodeVal = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.GENDER, newValue);
                }
                clientForUpdate.setGender(newCodeVal);
            }

            if (changes.containsKey(ClientApiConstants.clientTypeIdParamName)) {
                final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.clientTypeIdParamName);
                CodeValue newCodeVal = null;
                if (newValue != null) {
                    newCodeVal = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.CLIENT_TYPE,
                            newValue);
                }
                clientForUpdate.setClientType(newCodeVal);
            }

            if (changes.containsKey(ClientApiConstants.clientClassificationIdParamName)) {
                final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.clientClassificationIdParamName);
                CodeValue newCodeVal = null;
                if (newValue != null) {
                    newCodeVal = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                            ClientApiConstants.CLIENT_CLASSIFICATION, newValue);
                }
                clientForUpdate.setClientClassification(newCodeVal);
            }

            if (!changes.isEmpty()) {
                this.clientRepository.saveAndFlush(clientForUpdate);
            }

            if (changes.containsKey(ClientApiConstants.legalFormIdParamName)) {
            	Integer legalFormValue = clientForUpdate.getLegalForm();
            	boolean isChangedToEntity = false;
            	if(legalFormValue != null)
            	{
            		LegalForm legalForm = LegalForm.fromInt(legalFormValue);
            		if(legalForm != null)
            			isChangedToEntity = legalForm.isEntity();
            	}

                if(isChangedToEntity)
                {
                	extractAndCreateClientNonPerson(clientForUpdate, command);
                }
                else
                {
                	final ClientNonPerson clientNonPerson = this.clientNonPersonRepository.findOneByClientId(clientForUpdate.getId());
                	if(clientNonPerson != null)
                		this.clientNonPersonRepository.delete(clientNonPerson);
                }
            }

            final ClientNonPerson clientNonPersonForUpdate = this.clientNonPersonRepository.findOneByClientId(clientId);
            if(clientNonPersonForUpdate != null)
            {
            	final JsonElement clientNonPersonElement = command.jsonElement(ClientApiConstants.clientNonPersonDetailsParamName);
            	final Map<String, Object> clientNonPersonChanges = updateNonPerson(clientNonPersonForUpdate, JsonCommand.fromExistingCommand(command, clientNonPersonElement));

                if (clientNonPersonChanges.containsKey(ClientApiConstants.constitutionIdParamName)) {

                    final Long newValue = this.fromApiJsonHelper.extractLongNamed(ClientApiConstants.constitutionIdParamName, clientNonPersonElement);
                    CodeValue constitution = null;
                    if (newValue != null) {
                        constitution = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.CLIENT_NON_PERSON_CONSTITUTION, newValue);
                    }
                    clientNonPersonForUpdate.setConstitution(constitution);
                }

                if (clientNonPersonChanges.containsKey(ClientApiConstants.mainBusinessLineIdParamName)) {

                    final Long newValue = this.fromApiJsonHelper.extractLongNamed(ClientApiConstants.mainBusinessLineIdParamName, clientNonPersonElement);
                    CodeValue mainBusinessLine = null;
                    if (newValue != null) {
                        mainBusinessLine = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.CLIENT_NON_PERSON_MAIN_BUSINESS_LINE, newValue);
                    }
                    clientNonPersonForUpdate.setMainBusinessLine(mainBusinessLine);
                }

                if (!clientNonPersonChanges.isEmpty()) {
                    this.clientNonPersonRepository.saveAndFlush(clientNonPersonForUpdate);
                }

                changes.putAll(clientNonPersonChanges);
            } else {
                final Integer legalFormParamValue = command.integerValueOfParameterNamed(ClientApiConstants.legalFormIdParamName);
                boolean isEntity = false;
                if (legalFormParamValue != null) {
                    final LegalForm legalForm = LegalForm.fromInt(legalFormParamValue);
                    if (legalForm != null) {
                        isEntity = legalForm.isEntity();
                    }
                }
                if (isEntity) {
                    extractAndCreateClientNonPerson(clientForUpdate, command);
                }
            }
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .officeId(clientForUpdate.getOffice().getId()) //
                    .clientId(clientId) //
                    .resourceId(clientId) //
                    .changes(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch(final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            handleDataIntegrityIssues(command, throwable, dve);
         	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult activateClient(final Long clientId, final JsonCommand command) {
        try {
            this.fromApiJsonDeserializer.validateActivation(command);

            final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId, true);
            validateParentGroupRulesBeforeClientActivation(client);
            final Locale locale = command.extractLocale();
            final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
            final LocalDate activationDate = command.localDateValueOfParameterNamed("activationDate");

            runEntityDatatableCheck(clientId);

            final AppUser currentUser = this.context.authenticatedUser();
            client.activate(currentUser, fmt, activationDate);
            CommandProcessingResult result = openSavingsAccount(client, fmt);
            this.clientRepository.saveAndFlush(client);
            this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.CLIENTS_ACTIVATE,
                    constructEntityMap(BUSINESS_ENTITY.CLIENT, client));
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .officeId(client.getOffice().getId()) //
                    .clientId(clientId) //
                    .resourceId(clientId) //
                    .savingsId(result.getSavingsId())//
                    .rollbackTransaction(result.isRollbackTransaction())//
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }
    }

    private Client createNew(final AppUser currentUser,
                             final Office clientOffice,
                             final Group clientParentGroup,
                             final Staff staff,
                             final Long savingsProductId,
                             final CodeValue gender,
                             final CodeValue clientType,
                             final CodeValue clientClassification,
                             final Integer legalForm,
                             final JsonCommand command) {

        final String accountNo = command.stringValueOfParameterNamed(ClientApiConstants.accountNoParamName);
        final String externalId = command.stringValueOfParameterNamed(ClientApiConstants.externalIdParamName);
        final String mobileNo = command.stringValueOfParameterNamed(ClientApiConstants.mobileNoParamName);
        final String emailAddress = command.stringValueOfParameterNamed(ClientApiConstants.emailAddressParamName);

        final String firstname = command.stringValueOfParameterNamed(ClientApiConstants.firstnameParamName);
        final String middlename = command.stringValueOfParameterNamed(ClientApiConstants.middlenameParamName);
        final String lastname = command.stringValueOfParameterNamed(ClientApiConstants.lastnameParamName);
        final String fullname = command.stringValueOfParameterNamed(ClientApiConstants.fullnameParamName);

        final boolean isStaff = command.booleanPrimitiveValueOfParameterNamed(ClientApiConstants.isStaffParamName);

        final LocalDate dataOfBirth = command.localDateValueOfParameterNamed(ClientApiConstants.dateOfBirthParamName);

        ClientStatus status = ClientStatus.PENDING;
        boolean active = false;
        if (command.hasParameter("active")) {
            active = command.booleanPrimitiveValueOfParameterNamed(ClientApiConstants.activeParamName);
        }

        LocalDate activationDate = null;
        LocalDate officeJoiningDate = null;
        if (active) {
            status = ClientStatus.ACTIVE;
            activationDate = command.localDateValueOfParameterNamed(ClientApiConstants.activationDateParamName);
            officeJoiningDate = activationDate;
        }

        LocalDate submittedOnDate = LocalDate.now();
        if (active && submittedOnDate.isAfter(activationDate)) {
            submittedOnDate = activationDate;
        }
        if (command.hasParameter(ClientApiConstants.submittedOnDateParamName)) {
            submittedOnDate = command.localDateValueOfParameterNamed(ClientApiConstants.submittedOnDateParamName);
        }

        Client client = Client.builder()
            .submittedBy(currentUser)
            .status(status.getValue())
            .office(clientOffice)
            .firstname(StringUtils.trim(firstname))
            .middlename(StringUtils.trim(middlename))
            .lastname(StringUtils.trim(lastname))
            .fullname(StringUtils.trim(fullname))
            .activationDate(activationDate!=null ? activationDate.toDateTimeAtStartOfDay().toDate() : null)
            .officeJoiningDate(officeJoiningDate!=null ? officeJoiningDate.toDateTimeAtStartOfDay().toDate() : null)
            .externalId(StringUtils.trim(externalId))
            .mobileNo(StringUtils.trim(mobileNo))
            .emailAddress(StringUtils.trim(emailAddress))
            .staff(staff)
            .submittedOnDate(submittedOnDate.toDate())
            .savingsProductId(savingsProductId)
            .dateOfBirth(dataOfBirth!=null ? dataOfBirth.toDateTimeAtStartOfDay().toDate() : null)
            .gender(gender)
            .clientType(clientType)
            .clientClassification(clientClassification)
            .legalForm(legalForm)
            .staffFlag(isStaff)
            .build();

        if (StringUtils.isBlank(accountNo)) {
            client.setAccountNumber(new RandomPasswordGenerator(19).generate());
            client.setAccountNumberRequiresAutoGeneration(true);
        } else {
            client.setAccountNumber(accountNo);
        }

        if (clientParentGroup != null) {
            client.setGroups(Collections.singleton(clientParentGroup));
        }

        client.deriveDisplayName();
        client.validate();

        return client;
    }

    private ClientNonPerson createNewNonPerson(final Client client, final CodeValue constitution, final CodeValue mainBusinessLine, String incorpNumber, LocalDate incorpValidityTill, String remarks) {
        validateNonPerson(client, incorpValidityTill);

        return ClientNonPerson.builder()
            .client(client)
            .constitution(constitution)
            .mainBusinessLine(mainBusinessLine)
            .incorpNumber(incorpNumber)
            .incorpValidityTill(incorpValidityTill==null ? null : incorpValidityTill.toDateTimeAtStartOfDay().toDate())
            .remarks(remarks)
            .build();
    }

    private void validateNonPerson(final Client client, LocalDate incorpValidityTill) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        if (incorpValidityTill!=null && client.dateOfBirthLocalDate() != null && client.dateOfBirthLocalDate().isAfter(incorpValidityTill)) {
            final String defaultUserMessage = "incorpvaliditytill date cannot be after the incorporation date";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.clients.incorpValidityTill.after.incorp.date", defaultUserMessage, ClientApiConstants.incorpValidityTillParamName, incorpValidityTill);
            dataValidationErrors.add(error);
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    private Map<String, Object> updateNonPerson(final ClientNonPerson nonPerson, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        if (command.isChangeInStringParameterNamed(ClientApiConstants.incorpNumberParamName, nonPerson.getIncorpNumber())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.incorpNumberParamName);
            actualChanges.put(ClientApiConstants.incorpNumberParamName, newValue);
            nonPerson.setIncorpNumber(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.remarksParamName, nonPerson.getRemarks())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.remarksParamName);
            actualChanges.put(ClientApiConstants.remarksParamName, newValue);
            nonPerson.setRemarks(StringUtils.defaultIfEmpty(newValue, null));
        }

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        if (command.isChangeInLocalDateParameterNamed(ClientApiConstants.incorpValidityTillParamName, LocalDate.fromDateFields(nonPerson.getIncorpValidityTill()))) {
            final String valueAsInput = command.stringValueOfParameterNamed(ClientApiConstants.incorpValidityTillParamName);
            actualChanges.put(ClientApiConstants.incorpValidityTillParamName, valueAsInput);
            actualChanges.put(ClientApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(ClientApiConstants.localeParamName, localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(ClientApiConstants.incorpValidityTillParamName);
            nonPerson.setIncorpValidityTill(newValue.toDate());
        }

        if (command.isChangeInLongParameterNamed(ClientApiConstants.constitutionIdParamName, nonPerson.getConstitution().getId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.constitutionIdParamName);
            actualChanges.put(ClientApiConstants.constitutionIdParamName, newValue);
        }

        if (command.isChangeInLongParameterNamed(ClientApiConstants.mainBusinessLineIdParamName, nonPerson.getMainBusinessLine().getId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.mainBusinessLineIdParamName);
            actualChanges.put(ClientApiConstants.mainBusinessLineIdParamName, newValue);
        }

        //validate();

        return actualChanges;
    }

    private Map<String, Object> update(final Client client, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        if (command.isChangeInIntegerParameterNamed(ClientApiConstants.statusParamName, client.getStatus())) {
            final Integer newValue = command.integerValueOfParameterNamed(ClientApiConstants.statusParamName);
            actualChanges.put(ClientApiConstants.statusParamName, ClientEnumerations.status(newValue));
            client.setStatus(ClientStatus.fromInt(newValue).getValue());
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.accountNoParamName, client.getAccountNumber())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.accountNoParamName);
            actualChanges.put(ClientApiConstants.accountNoParamName, newValue);
            client.setAccountNumber(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.externalIdParamName, client.getExternalId())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.externalIdParamName);
            actualChanges.put(ClientApiConstants.externalIdParamName, newValue);
            client.setExternalId(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.mobileNoParamName, client.getMobileNo())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.mobileNoParamName);
            actualChanges.put(ClientApiConstants.mobileNoParamName, newValue);
            client.setMobileNo(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.emailAddressParamName, client.getEmailAddress())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.emailAddressParamName);
            actualChanges.put(ClientApiConstants.emailAddressParamName, newValue);
            client.setEmailAddress(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.firstnameParamName, client.getFirstname())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.firstnameParamName);
            actualChanges.put(ClientApiConstants.firstnameParamName, newValue);
            client.setFirstname(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.middlenameParamName, client.getMiddlename())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.middlenameParamName);
            actualChanges.put(ClientApiConstants.middlenameParamName, newValue);
            client.setMiddlename(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.lastnameParamName, client.getLastname())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.lastnameParamName);
            actualChanges.put(ClientApiConstants.lastnameParamName, newValue);
            client.setLastname(StringUtils.defaultIfEmpty(newValue, null));
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.fullnameParamName, client.getFullname())) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.fullnameParamName);
            actualChanges.put(ClientApiConstants.fullnameParamName, newValue);
            client.setFullname(newValue);
        }

        if (client.getStaff()!=null && command.isChangeInLongParameterNamed(ClientApiConstants.staffIdParamName, client.getStaff().getId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.staffIdParamName);
            actualChanges.put(ClientApiConstants.staffIdParamName, newValue);
        }

        if (client.getGender()!=null && command.isChangeInLongParameterNamed(ClientApiConstants.genderIdParamName, client.getGender().getId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.genderIdParamName);
            actualChanges.put(ClientApiConstants.genderIdParamName, newValue);
        }

        if (command.isChangeInLongParameterNamed(ClientApiConstants.savingsProductIdParamName, client.getSavingsProductId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.savingsProductIdParamName);
            actualChanges.put(ClientApiConstants.savingsProductIdParamName, newValue);
        }

        if (client.getClientType()!=null && command.isChangeInLongParameterNamed(ClientApiConstants.clientTypeIdParamName, client.getClientType().getId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.clientTypeIdParamName);
            actualChanges.put(ClientApiConstants.clientTypeIdParamName, newValue);
        }

        if (client.getClientClassification()!=null && command.isChangeInLongParameterNamed(ClientApiConstants.clientClassificationIdParamName, client.getClientClassification().getId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.clientClassificationIdParamName);
            actualChanges.put(ClientApiConstants.clientClassificationIdParamName, newValue);
        }

        if (command.isChangeInIntegerParameterNamed(ClientApiConstants.legalFormIdParamName, client.getLegalForm())) {
            final Integer newValue = command.integerValueOfParameterNamed(ClientApiConstants.legalFormIdParamName);
            if(newValue != null)
            {
                LegalForm legalForm = LegalForm.fromInt(newValue);
                if(legalForm != null)
                {
                    actualChanges.put(ClientApiConstants.legalFormIdParamName, ClientEnumerations.legalForm(newValue));
                    client.setLegalForm(legalForm.getValue());
                    if(legalForm.isPerson()){
                        client.setFullname(null);
                    }else if(legalForm.isEntity()){
                        client.setFirstname(null);
                        client.setLastname(null);
                        client.setDisplayName(null);
                    }
                }
                else
                {
                    actualChanges.put(ClientApiConstants.legalFormIdParamName, null);
                    client.setLegalForm(null);
                }
            }
            else
            {
                actualChanges.put(ClientApiConstants.legalFormIdParamName, null);
                client.setLegalForm(null);
            }
        }

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        if (command.isChangeInLocalDateParameterNamed(ClientApiConstants.activationDateParamName, client.getActivationLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(ClientApiConstants.activationDateParamName);
            actualChanges.put(ClientApiConstants.activationDateParamName, valueAsInput);
            actualChanges.put(ClientApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(ClientApiConstants.localeParamName, localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(ClientApiConstants.activationDateParamName);
            client.setActivationDate(newValue.toDate());
            client.setOfficeJoiningDate(client.getActivationDate());
        }

        if (command.isChangeInLocalDateParameterNamed(ClientApiConstants.dateOfBirthParamName, LocalDate.fromDateFields(client.getDateOfBirth()))) {
            final String valueAsInput = command.stringValueOfParameterNamed(ClientApiConstants.dateOfBirthParamName);
            actualChanges.put(ClientApiConstants.dateOfBirthParamName, valueAsInput);
            actualChanges.put(ClientApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(ClientApiConstants.localeParamName, localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(ClientApiConstants.dateOfBirthParamName);
            client.setDateOfBirth(newValue.toDate());
        }

        if (command.isChangeInLocalDateParameterNamed(ClientApiConstants.submittedOnDateParamName, client.getSubmittedOnDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(ClientApiConstants.submittedOnDateParamName);
            actualChanges.put(ClientApiConstants.submittedOnDateParamName, valueAsInput);
            actualChanges.put(ClientApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(ClientApiConstants.localeParamName, localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(ClientApiConstants.submittedOnDateParamName);
            client.setSubmittedOnDate(newValue.toDate());
        }

        client.validateUpdate();

        client.deriveDisplayName();

        return actualChanges;
    }

    private CommandProcessingResult openSavingsAccount(final Client client, final DateTimeFormatter fmt) {
        CommandProcessingResult commandProcessingResult = new CommandProcessingResult();
        if (client.isActive() && client.getSavingsProductId() != null) {
            SavingsAccountDataDTO savingsAccountDataDTO = new SavingsAccountDataDTO(client, null, client.getSavingsProductId(),
                    client.getActivationLocalDate(), client.getActivatedBy(), fmt);
            commandProcessingResult = this.savingsApplicationProcessWritePlatformService.createActiveApplication(savingsAccountDataDTO);
            if (commandProcessingResult.getSavingsId() != null) {
                this.savingsRepositoryWrapper.findOneWithNotFoundDetection(commandProcessingResult.getSavingsId());
                client.setSavingsAccountId(commandProcessingResult.getSavingsId());
                client.setSavingsProductId(null);
            }
        }
        return commandProcessingResult;
    }

    @Transactional
    @Override
    public CommandProcessingResult unassignClientStaff(final Long clientId, final JsonCommand command) {

        this.context.authenticatedUser();

        final Map<String, Object> actualChanges = new LinkedHashMap<>(5);

        this.fromApiJsonDeserializer.validateForUnassignStaff(command.json());

        final Client clientForUpdate = this.clientRepository.findOneWithNotFoundDetection(clientId);

        final Staff presentStaff = clientForUpdate.getStaff();
        Long presentStaffId = null;
        if (presentStaff == null) { throw new ClientHasNoStaffException(clientId); }
        presentStaffId = presentStaff.getId();
        final String staffIdParamName = ClientApiConstants.staffIdParamName;
        if (!command.isChangeInLongParameterNamed(staffIdParamName, presentStaffId)) {
            clientForUpdate.setStaff(null);
        }
        this.clientRepository.saveAndFlush(clientForUpdate);

        actualChanges.put(staffIdParamName, presentStaffId);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .officeId(clientForUpdate.getOffice().getId()) //
                .resourceId(clientForUpdate.getId()) //
                .clientId(clientId) //
                .changes(actualChanges) //
                .build();
    }

    @Override
    public CommandProcessingResult assignClientStaff(final Long clientId, final JsonCommand command) {

        this.context.authenticatedUser();

        final Map<String, Object> actualChanges = new LinkedHashMap<>(5);

        this.fromApiJsonDeserializer.validateForAssignStaff(command.json());

        final Client clientForUpdate = this.clientRepository.findOneWithNotFoundDetection(clientId);
        Staff staff = null;
        final Long staffId = command.longValueOfParameterNamed(ClientApiConstants.staffIdParamName);
        if (staffId != null) {
            staff = this.staffRepository.findByOfficeHierarchyWithNotFoundDetection(staffId, clientForUpdate.getOffice().getHierarchy());
            /**
             * TODO Vishwas: We maintain history of chage of loan officer w.r.t
             * loan in a history table, should we do the same for a client?
             * Especially useful when the change happens due to a transfer etc
             **/
            clientForUpdate.setStaff(staff);
        }

        this.clientRepository.saveAndFlush(clientForUpdate);

        actualChanges.put(ClientApiConstants.staffIdParamName, staffId);
        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .officeId(clientForUpdate.getOffice().getId()) //
                .resourceId(clientForUpdate.getId()) //
                .clientId(clientId) //
                .changes(actualChanges) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult closeClient(final Long clientId, final JsonCommand command) {
        try {

            final AppUser currentUser = this.context.authenticatedUser();
            this.fromApiJsonDeserializer.validateClose(command);

            final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
            final LocalDate closureDate = command.localDateValueOfParameterNamed(ClientApiConstants.closureDateParamName);
            final Long closureReasonId = command.longValueOfParameterNamed(ClientApiConstants.closureReasonIdParamName);

            final CodeValue closureReason = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                    ClientApiConstants.CLIENT_CLOSURE_REASON, closureReasonId);

            if (ClientStatus.fromInt(client.getStatus()).isClosed()) {
                final String errorMessage = "Client is already closed.";
                throw new InvalidClientStateTransitionException("close", "is.already.closed", errorMessage);
            } else if (ClientStatus.fromInt(client.getStatus()).isUnderTransfer()) {
                final String errorMessage = "Cannot Close a Client under Transfer";
                throw new InvalidClientStateTransitionException("close", "is.under.transfer", errorMessage);
            }

            if (client.isNotPending() && client.getActivationLocalDate() != null && client.getActivationLocalDate().isAfter(closureDate)) {
                final String errorMessage = "The client closureDate cannot be before the client ActivationDate.";
                throw new InvalidClientStateTransitionException("close", "date.cannot.before.client.actvation.date", errorMessage,
                        closureDate, client.getActivationLocalDate());
            }
            entityDatatableChecksWritePlatformService.runTheCheck(clientId,EntityTables.CLIENT.getName(),
                    StatusEnum.CLOSE.getCode().longValue(),EntityTables.CLIENT.getForeignKeyColumnNameOnDatatable());

            final List<Loan> clientLoans = this.loanRepositoryWrapper.findLoanByClientId(clientId);
            for (final Loan loan : clientLoans) {
                final LoanStatusMapper loanStatus = new LoanStatusMapper(loan.status().getValue());
                if (loanStatus.isOpen() || loanStatus.isPendingApproval() || loanStatus.isAwaitingDisbursal()) {
                    final String errorMessage = "Client cannot be closed because of non-closed loans.";
                    throw new InvalidClientStateTransitionException("close", "loan.non-closed", errorMessage);
                } else if (loanStatus.isClosed() && loan.getClosedOnDate().after(closureDate.toDate())) {
                    final String errorMessage = "The client closureDate cannot be before the loan closedOnDate.";
                    throw new InvalidClientStateTransitionException("close", "date.cannot.before.loan.closed.date", errorMessage,
                            closureDate, loan.getClosedOnDate());
                } else if (loanStatus.isOverpaid()) {
                    final String errorMessage = "Client cannot be closed because of overpaid loans.";
                    throw new InvalidClientStateTransitionException("close", "loan.overpaid", errorMessage);
                }
            }
            final List<SavingsAccount> clientSavingAccounts = this.savingsRepositoryWrapper.findSavingAccountByClientId(clientId);

            for (final SavingsAccount saving : clientSavingAccounts) {
                if (saving.isActive() || saving.isSubmittedAndPendingApproval() || saving.isApproved()) {
                    final String errorMessage = "Client cannot be closed because of non-closed savings account.";
                    throw new InvalidClientStateTransitionException("close", "non-closed.savings.account", errorMessage);
                }
            }

            client.close(currentUser, closureReason, closureDate.toDate());
            this.clientRepository.saveAndFlush(client);
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .clientId(clientId) //
                    .resourceId(clientId) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }
    }

    @Override
    public CommandProcessingResult updateDefaultSavingsAccount(final Long clientId, final JsonCommand command) {

        this.context.authenticatedUser();

        final Map<String, Object> actualChanges = new LinkedHashMap<>(5);

        this.fromApiJsonDeserializer.validateForSavingsAccount(command.json());

        final Client clientForUpdate = this.clientRepository.findOneWithNotFoundDetection(clientId);

        SavingsAccount savingsAccount = null;
        final Long savingsId = command.longValueOfParameterNamed(ClientApiConstants.savingsAccountIdParamName);
        if (savingsId != null) {
            savingsAccount = this.savingsRepositoryWrapper.findOneWithNotFoundDetection(savingsId);
            if (!savingsAccount.getClient().getId().equals(clientId)) {
                String defaultUserMessage = "saving account must belongs to client";
                throw new InvalidClientSavingProductException("saving.account", "must.belongs.to.client", defaultUserMessage, savingsId,
                        clientForUpdate.getId());
            }
            clientForUpdate.setSavingsAccountId(savingsId);
        }

        this.clientRepository.saveAndFlush(clientForUpdate);

        actualChanges.put(ClientApiConstants.savingsAccountIdParamName, savingsId);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .officeId(clientForUpdate.getOffice().getId()) //
                .resourceId(clientForUpdate.getId()) //
                .clientId(clientId) //
                .changes(actualChanges) //
                .build();
    }

    /*
     * To become a part of a group, group may have set of criteria to be m et
     * before client can become member of it.
     */
    private void validateParentGroupRulesBeforeClientActivation(Client client) {
        Integer minNumberOfClients = configurationDomainService.retrieveMinAllowedClientsInGroup();
        Integer maxNumberOfClients = configurationDomainService.retrieveMaxAllowedClientsInGroup();
        if (client.getGroups() != null && maxNumberOfClients != null) {
            for (Group group : client.getGroups()) {
                /**
                 * Since this Client has not yet been associated with the group,
                 * reduce maxNumberOfClients by 1
                 **/
                final boolean validationsuccess = group.isGroupsClientCountWithinMaxRange(maxNumberOfClients - 1);
                if (!validationsuccess) { throw new GroupMemberCountNotInPermissibleRangeException(group.getId(), minNumberOfClients,
                        maxNumberOfClients); }
            }
        }
    }

    private void runEntityDatatableCheck(final Long clientId) {
        entityDatatableChecksWritePlatformService.runTheCheck(clientId, EntityTables.CLIENT.getName(), StatusEnum.ACTIVATE.getCode()
                .longValue(), EntityTables.CLIENT.getForeignKeyColumnNameOnDatatable());
    }

    @Override
    public CommandProcessingResult rejectClient(final Long entityId, final JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        this.fromApiJsonDeserializer.validateRejection(command);

        final Client client = this.clientRepository.findOneWithNotFoundDetection(entityId);
        final LocalDate rejectionDate = command.localDateValueOfParameterNamed(ClientApiConstants.rejectionDateParamName);
        final Long rejectionReasonId = command.longValueOfParameterNamed(ClientApiConstants.rejectionReasonIdParamName);

        final CodeValue rejectionReason = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                ClientApiConstants.CLIENT_REJECT_REASON, rejectionReasonId);

        if (client.isNotPending()) {
            final String errorMessage = "Only clients pending activation may be withdrawn.";
            throw new InvalidClientStateTransitionException("rejection", "on.account.not.in.pending.activation.status", errorMessage,
                    rejectionDate, client.getSubmittedOnDate());
        } else if (client.getSubmittedOnDate().isAfter(rejectionDate)) {
            final String errorMessage = "The client rejection date cannot be before the client submitted date.";
            throw new InvalidClientStateTransitionException("rejection", "date.cannot.before.client.submitted.date", errorMessage,
                    rejectionDate, client.getSubmittedOnDate());
        }
        client.reject(currentUser, rejectionReason, rejectionDate.toDate());
        this.clientRepository.saveAndFlush(client);
        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.CLIENTS_REJECT,
                constructEntityMap(BUSINESS_ENTITY.CLIENT, client));
        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .clientId(entityId) //
                .resourceId(entityId) //
                .build();
    }

    @Override
    public CommandProcessingResult withdrawClient(Long entityId, JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        this.fromApiJsonDeserializer.validateWithdrawn(command);

        final Client client = this.clientRepository.findOneWithNotFoundDetection(entityId);
        final LocalDate withdrawalDate = command.localDateValueOfParameterNamed(ClientApiConstants.withdrawalDateParamName);
        final Long withdrawalReasonId = command.longValueOfParameterNamed(ClientApiConstants.withdrawalReasonIdParamName);

        final CodeValue withdrawalReason = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                ClientApiConstants.CLIENT_WITHDRAW_REASON, withdrawalReasonId);

        if (client.isNotPending()) {
            final String errorMessage = "Only clients pending activation may be withdrawn.";
            throw new InvalidClientStateTransitionException("withdrawal", "on.account.not.in.pending.activation.status", errorMessage,
                    withdrawalDate, client.getSubmittedOnDate());
        } else if (client.getSubmittedOnDate().isAfter(withdrawalDate)) {
            final String errorMessage = "The client withdrawal date cannot be before the client submitted date.";
            throw new InvalidClientStateTransitionException("withdrawal", "date.cannot.before.client.submitted.date", errorMessage,
                    withdrawalDate, client.getSubmittedOnDate());
        }
        client.withdraw(currentUser, withdrawalReason, withdrawalDate.toDate());
        this.clientRepository.saveAndFlush(client);
        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .clientId(entityId) //
                .resourceId(entityId) //
                .build();
    }

    @Override
    public CommandProcessingResult reActivateClient(Long entityId, JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        this.fromApiJsonDeserializer.validateReactivate(command);

        final Client client = this.clientRepository.findOneWithNotFoundDetection(entityId);
        final LocalDate reactivateDate = command.localDateValueOfParameterNamed(ClientApiConstants.reactivationDateParamName);

        if (!client.isClosed()) {
            final String errorMessage = "only closed clients may be reactivated.";
            throw new InvalidClientStateTransitionException("reactivation", "on.nonclosed.account", errorMessage);
        } else if (client.getClosureDate().isAfter(reactivateDate)) {
            final String errorMessage = "The client reactivation date cannot be before the client closed date.";
            throw new InvalidClientStateTransitionException("reactivation", "date.cannot.before.client.closed.date", errorMessage,
                    reactivateDate, client.getClosureDate());
        }
        client.reActivate(currentUser, reactivateDate.toDate());
        this.clientRepository.saveAndFlush(client);
        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .clientId(entityId) //
                .resourceId(entityId) //
                .build();
    }

	@Override
	public CommandProcessingResult undoRejection(Long entityId, JsonCommand command) {
		final AppUser currentUser = this.context.authenticatedUser();
		this.fromApiJsonDeserializer.validateUndoRejection(command);

		final Client client = this.clientRepository.findOneWithNotFoundDetection(entityId);
		final LocalDate undoRejectDate = command
				.localDateValueOfParameterNamed(ClientApiConstants.reopenedDateParamName);

		if (!client.isRejected()) {
			final String errorMessage = "only rejected clients may be reactivated.";
			throw new InvalidClientStateTransitionException("undorejection", "on.nonrejected.account", errorMessage);
		} else if (client.getRejectedDate().isAfter(undoRejectDate)) {
			final String errorMessage = "The client reactivation date cannot be before the client rejected date.";
			throw new InvalidClientStateTransitionException("reopened", "date.cannot.before.client.rejected.date",
					errorMessage, undoRejectDate, client.getRejectedDate());
		}

		client.reOpened(currentUser, undoRejectDate.toDate());
		this.clientRepository.saveAndFlush(client);

		return CommandProcessingResult.builder() //
				.commandId(command.commandId()) //
				.clientId(entityId) //
				.resourceId(entityId) //
				.build();
	}

	@Override
	public CommandProcessingResult undoWithdrawal(Long entityId, JsonCommand command) {
		final AppUser currentUser = this.context.authenticatedUser();
		this.fromApiJsonDeserializer.validateUndoWithDrawn(command);

		final Client client = this.clientRepository.findOneWithNotFoundDetection(entityId);
		final LocalDate undoWithdrawalDate = command
				.localDateValueOfParameterNamed(ClientApiConstants.reopenedDateParamName);

		if (!client.isWithdrawn()) {
			final String errorMessage = "only withdrawal clients may be reactivated.";
			throw new InvalidClientStateTransitionException("undoWithdrawal", "on.nonwithdrawal.account", errorMessage);
		} else if (client.getWithdrawalDate().isAfter(undoWithdrawalDate)) {
			final String errorMessage = "The client reactivation date cannot be before the client withdrawal date.";
			throw new InvalidClientStateTransitionException("reopened", "date.cannot.before.client.withdrawal.date",
					errorMessage, undoWithdrawalDate, client.getWithdrawalDate());
		}
		client.reOpened(currentUser, undoWithdrawalDate.toDate());
		this.clientRepository.saveAndFlush(client);

		return CommandProcessingResult.builder() //
				.commandId(command.commandId()) //
				.clientId(entityId) //
				.resourceId(entityId) //
				.build();
	}

    private Map<BUSINESS_ENTITY, Object> constructEntityMap(final BUSINESS_ENTITY entityEvent, Object entity) {
        Map<BUSINESS_ENTITY, Object> map = new HashMap<>(1);
        map.put(entityEvent, entity);
        return map;
    }
}
