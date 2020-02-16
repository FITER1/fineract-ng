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
package org.apache.fineract.infrastructure.accountnumberformat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.accountnumberformat.data.AccountNumberFormatDataValidator;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormat;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatEnumerations.AccountNumberPrefixType;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatRepositoryWrapper;
import org.apache.fineract.infrastructure.accountnumberformat.domain.EntityAccountType;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountNumberFormatWritePlatformServiceJpaRepositoryImpl implements AccountNumberFormatWritePlatformService {

    private final AccountNumberFormatRepositoryWrapper accountNumberFormatRepository;
    private final AccountNumberFormatDataValidator accountNumberFormatDataValidator;

    @Override
    @Transactional
    public CommandProcessingResult createAccountNumberFormat(JsonCommand command) {
        try {
            this.accountNumberFormatDataValidator.validateForCreate(command.json());

            final Integer accountTypeId = command.integerValueSansLocaleOfParameterNamed(AccountNumberFormatConstants.accountTypeParamName);
            final EntityAccountType entityAccountType = EntityAccountType.fromInt(accountTypeId);

            final Integer prefixTypeId = command.integerValueSansLocaleOfParameterNamed(AccountNumberFormatConstants.prefixTypeParamName);
            AccountNumberPrefixType accountNumberPrefixType = null;
            if (prefixTypeId != null) {
                accountNumberPrefixType = AccountNumberPrefixType.fromInt(prefixTypeId);
            }

            AccountNumberFormat accountNumberFormat = AccountNumberFormat.builder()
                .accountTypeEnum(entityAccountType!=null ? entityAccountType.getValue() : null)
                .prefixEnum(accountNumberPrefixType!=null ? accountNumberPrefixType.getValue() : null)
                .build();

            this.accountNumberFormatRepository.save(accountNumberFormat);

            return CommandProcessingResult.builder() //
                    .resourceId(accountNumberFormat.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException ee) {
        	Throwable throwable = ExceptionUtils.getRootCause(ee.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, ee);
        	return new CommandProcessingResult();
        }
    }

    @Override
    @Transactional
    public CommandProcessingResult updateAccountNumberFormat(Long accountNumberFormatId, JsonCommand command) {
        try {

            final AccountNumberFormat accountNumberFormatForUpdate = this.accountNumberFormatRepository
                    .findOneWithNotFoundDetection(accountNumberFormatId);
            EntityAccountType accountType = EntityAccountType.fromInt(accountNumberFormatForUpdate.getAccountTypeEnum());

            this.accountNumberFormatDataValidator.validateForUpdate(command.json(), accountType);

            final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

            if (command.isChangeInIntegerSansLocaleParameterNamed(AccountNumberFormatConstants.prefixTypeParamName,
                    accountNumberFormatForUpdate.getPrefixEnum())) {
                final Integer newValue = command.integerValueSansLocaleOfParameterNamed(AccountNumberFormatConstants.prefixTypeParamName);
                final AccountNumberPrefixType accountNumberPrefixType = AccountNumberPrefixType.fromInt(newValue);
                actualChanges.put(AccountNumberFormatConstants.prefixTypeParamName, accountNumberPrefixType);
                accountNumberFormatForUpdate.setPrefixEnum(accountNumberPrefixType.getValue());
            }

            if (!actualChanges.isEmpty()) {
                this.accountNumberFormatRepository.saveAndFlush(accountNumberFormatForUpdate);
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(accountNumberFormatId) //
                    .changes(actualChanges) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        } catch (final PersistenceException ee) {
        	Throwable throwable = ExceptionUtils.getRootCause(ee.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, ee);
        	return new CommandProcessingResult();
        }
    }

    @Override
    @Transactional
    public CommandProcessingResult deleteAccountNumberFormat(Long accountNumberFormatId) {
        AccountNumberFormat accountNumberFormat = this.accountNumberFormatRepository.findOneWithNotFoundDetection(accountNumberFormatId);
        this.accountNumberFormatRepository.delete(accountNumberFormat);

        return CommandProcessingResult.builder() //
                .resourceId(accountNumberFormatId) //
                .build();
    }

        
    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains(AccountNumberFormatConstants.ACCOUNT_TYPE_UNIQUE_CONSTRAINT_NAME)) {

            final Integer accountTypeId = command.integerValueSansLocaleOfParameterNamed(AccountNumberFormatConstants.accountTypeParamName);
            final EntityAccountType entityAccountType = EntityAccountType.fromInt(accountTypeId);
            throw new PlatformDataIntegrityException(AccountNumberFormatConstants.EXCEPTION_DUPLICATE_ACCOUNT_TYPE,
                    "Account Format preferences for Account type `" + entityAccountType.getCode() + "` already exists", "externalId",
                    entityAccountType.getValue(), entityAccountType.getCode());
        }
        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.account.number.format.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
}
