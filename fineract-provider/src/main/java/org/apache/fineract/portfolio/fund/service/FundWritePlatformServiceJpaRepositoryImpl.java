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
package org.apache.fineract.portfolio.fund.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.fund.domain.Fund;
import org.apache.fineract.portfolio.fund.domain.FundRepository;
import org.apache.fineract.portfolio.fund.exception.FundNotFoundException;
import org.apache.fineract.portfolio.fund.serialization.FundCommandFromApiJsonDeserializer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundWritePlatformServiceJpaRepositoryImpl implements FundWritePlatformService {

    private final PlatformSecurityContext context;
    private final FundCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final FundRepository fundRepository;

    @Transactional
    @Override
    @CacheEvict(value = "funds", key = "@fineractProperties.getTenantId().concat('fn')")
    public CommandProcessingResult createFund(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final Fund fund = Fund.fromJson(command);

            this.fundRepository.save(fund);

            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(fund.getId()).build();
        } catch (final DataIntegrityViolationException dve) {
            handleFundDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleFundDataIntegrityIssues(command, throwable, dve);
         	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    @CacheEvict(value = "funds", key = "@fineractProperties.getTenantId().concat('fn')")
    public CommandProcessingResult updateFund(final Long fundId, final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            final Fund fund = this.fundRepository.findById(fundId)
                    .orElseThrow(() -> new FundNotFoundException(fundId));

            final Map<String, Object> changes = fund.update(command);
            if (!changes.isEmpty()) {
                this.fundRepository.saveAndFlush(fund);
            }

            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(fund.getId()).changes(changes).build();
        } catch (final DataIntegrityViolationException dve) {
            handleFundDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleFundDataIntegrityIssues(command, throwable, dve);
         	return new CommandProcessingResult();
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleFundDataIntegrityIssues(final JsonCommand command,  final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("fund_externalid_org")) {
            final String externalId = command.stringValueOfParameterNamed("externalId");
            throw new PlatformDataIntegrityException("error.msg.fund.duplicate.externalId", "A fund with external id '" + externalId
                    + "' already exists", "externalId", externalId);
        } else if (realCause.getMessage().contains("fund_name_org")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.fund.duplicate.name", "A fund with name '" + name + "' already exists",
                    "name", name);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.fund.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}