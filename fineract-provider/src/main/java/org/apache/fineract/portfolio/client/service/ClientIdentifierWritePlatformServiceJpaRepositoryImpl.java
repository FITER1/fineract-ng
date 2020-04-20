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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.exception.CodeValueNotFoundException;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.command.ClientIdentifierCommand;
import org.apache.fineract.portfolio.client.domain.*;
import org.apache.fineract.portfolio.client.exception.ClientIdentifierNotFoundException;
import org.apache.fineract.portfolio.client.exception.DuplicateClientIdentifierException;
import org.apache.fineract.portfolio.client.serialization.ClientIdentifierCommandFromApiJsonDeserializer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientIdentifierWritePlatformServiceJpaRepositoryImpl implements ClientIdentifierWritePlatformService {

    private final PlatformSecurityContext context;
    private final ClientRepositoryWrapper clientRepository;
    private final ClientIdentifierRepository clientIdentifierRepository;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final ClientIdentifierCommandFromApiJsonDeserializer clientIdentifierCommandFromApiJsonDeserializer;

    @Transactional
    @Override
    public CommandProcessingResult addClientIdentifier(final Long clientId, final JsonCommand command) {

        this.context.authenticatedUser();
        final ClientIdentifierCommand clientIdentifierCommand = this.clientIdentifierCommandFromApiJsonDeserializer
                .commandFromApiJson(command.json());
        clientIdentifierCommand.validateForCreate();

        final String documentKey = clientIdentifierCommand.getDocumentKey();
        String documentTypeLabel = null;
        Long documentTypeId = null;
        try {
            final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);

            final CodeValue documentType = this.codeValueRepository.findOneWithNotFoundDetection(clientIdentifierCommand
                    .getDocumentTypeId());
            documentTypeId = documentType.getId();
            documentTypeLabel = documentType.getLabel();

            final ClientIdentifier clientIdentifier = create(client, documentType, command);

            this.clientIdentifierRepository.save(clientIdentifier);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .officeId(client.getOffice().getId()) //
                    .clientId(clientId) //
                    .resourceId(clientIdentifier.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleClientIdentifierDataIntegrityViolation(documentTypeLabel, documentTypeId, documentKey, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch(final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleClientIdentifierDataIntegrityViolation(documentTypeLabel, documentTypeId, documentKey, throwable, dve);
         	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateClientIdentifier(final Long clientId, final Long identifierId, final JsonCommand command) {

        this.context.authenticatedUser();
        final ClientIdentifierCommand clientIdentifierCommand = this.clientIdentifierCommandFromApiJsonDeserializer
                .commandFromApiJson(command.json());
        clientIdentifierCommand.validateForUpdate();

        String documentTypeLabel = null;
        String documentKey = null;
        Long documentTypeId = clientIdentifierCommand.getDocumentTypeId();
        try {
            CodeValue documentType = null;

            final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
            final ClientIdentifier clientIdentifierForUpdate = this.clientIdentifierRepository.findById(identifierId)
                    .orElseThrow(() -> new ClientIdentifierNotFoundException(identifierId));

            final Map<String, Object> changes = update(clientIdentifierForUpdate, command);

            if (changes.containsKey("documentTypeId")) {
                documentType = this.codeValueRepository.findOneWithNotFoundDetection(documentTypeId);
                if (documentType == null) { throw new CodeValueNotFoundException(documentTypeId); }

                documentTypeId = documentType.getId();
                documentTypeLabel = documentType.getLabel();
                clientIdentifierForUpdate.setDocumentType(documentType);
            }

            if (changes.containsKey("documentTypeId") && changes.containsKey("documentKey")) {
                documentTypeId = clientIdentifierCommand.getDocumentTypeId();
                documentKey = clientIdentifierCommand.getDocumentKey();
            } else if (changes.containsKey("documentTypeId") && !changes.containsKey("documentKey")) {
                documentTypeId = clientIdentifierCommand.getDocumentTypeId();
                documentKey = clientIdentifierForUpdate.getDocumentKey();
            } else if (!changes.containsKey("documentTypeId") && changes.containsKey("documentKey")) {
                documentTypeId = clientIdentifierForUpdate.getDocumentType().getId();
                documentKey = clientIdentifierForUpdate.getDocumentKey();
            }

            if (!changes.isEmpty()) {
                this.clientIdentifierRepository.saveAndFlush(clientIdentifierForUpdate);
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .officeId(client.getOffice().getId()) //
                    .clientId(clientId) //
                    .resourceId(identifierId) //
                    .changes(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleClientIdentifierDataIntegrityViolation(documentTypeLabel, documentTypeId, documentKey, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.builder().resourceId(-1L).build();
        }catch(final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleClientIdentifierDataIntegrityViolation(documentTypeLabel, documentTypeId, documentKey, throwable, dve);
         	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteClientIdentifier(final Long clientId, final Long identifierId, final Long commandId) {

        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);

        final ClientIdentifier clientIdentifier = this.clientIdentifierRepository.findById(identifierId)
                .orElseThrow(() -> new ClientIdentifierNotFoundException(identifierId));
        this.clientIdentifierRepository.delete(clientIdentifier);

        return CommandProcessingResult.builder() //
                .commandId(commandId) //
                .officeId(client.getOffice().getId()) //
                .clientId(clientId) //
                .resourceId(identifierId) //
                .build();
    }

    private ClientIdentifier create(final Client client, final CodeValue documentType, final JsonCommand command) {
        final String documentKey = command.stringValueOfParameterNamed("documentKey");
        final String description = command.stringValueOfParameterNamed("description");
        final String status = command.stringValueOfParameterNamed("status");
        ClientIdentifierStatus statusEnum = ClientIdentifierStatus.valueOf(status.toUpperCase());
        return ClientIdentifier.builder()
            .client(client)
            .documentType(documentType)
            .documentKey(documentKey)
            .status(statusEnum.getValue())
            .description(description)
            .active(statusEnum.isActive() ? statusEnum.getValue() : null)
            .build();
    }

    private Map<String, Object> update(final ClientIdentifier clientIdentifier, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String documentTypeIdParamName = "documentTypeId";
        if (command.isChangeInLongParameterNamed(documentTypeIdParamName, clientIdentifier.getDocumentType().getId())) {
            final Long newValue = command.longValueOfParameterNamed(documentTypeIdParamName);
            actualChanges.put(documentTypeIdParamName, newValue);
        }

        final String documentKeyParamName = "documentKey";
        if (command.isChangeInStringParameterNamed(documentKeyParamName, clientIdentifier.getDocumentKey())) {
            final String newValue = command.stringValueOfParameterNamed(documentKeyParamName);
            actualChanges.put(documentKeyParamName, newValue);
            clientIdentifier.setDocumentKey(StringUtils.defaultIfEmpty(newValue, null));
        }

        final String descriptionParamName = "description";
        if (command.isChangeInStringParameterNamed(descriptionParamName, clientIdentifier.getDescription())) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            clientIdentifier.setDescription(StringUtils.defaultIfEmpty(newValue, null));
        }

        final String statusParamName = "status";
        if(command.isChangeInStringParameterNamed(statusParamName, ClientIdentifierStatus.fromInt(clientIdentifier.getStatus()).getCode())){
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, ClientIdentifierStatus.valueOf(newValue));
            clientIdentifier.setStatus(ClientIdentifierStatus.valueOf(newValue).getValue());
        }

        return actualChanges;
    }

    private void handleClientIdentifierDataIntegrityViolation(final String documentTypeLabel, final Long documentTypeId,
            final String documentKey, final Throwable cause, final Exception dve) {
        if (cause.getMessage().contains("unique_active_client_identifier")) {
            throw new DuplicateClientIdentifierException(documentTypeLabel);
        } else if (cause.getMessage().contains("unique_identifier_key")) { throw new DuplicateClientIdentifierException(
                documentTypeId, documentTypeLabel, documentKey); }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.clientIdentifier.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}