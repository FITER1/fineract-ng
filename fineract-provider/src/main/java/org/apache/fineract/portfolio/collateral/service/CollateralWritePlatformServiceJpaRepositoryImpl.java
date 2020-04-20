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
package org.apache.fineract.portfolio.collateral.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.collateral.api.CollateralApiConstants;
import org.apache.fineract.portfolio.collateral.api.CollateralApiConstants.COLLATERAL_JSON_INPUT_PARAMS;
import org.apache.fineract.portfolio.collateral.command.CollateralCommand;
import org.apache.fineract.portfolio.collateral.domain.LoanCollateral;
import org.apache.fineract.portfolio.collateral.domain.LoanCollateralRepository;
import org.apache.fineract.portfolio.collateral.exception.CollateralCannotBeCreatedException;
import org.apache.fineract.portfolio.collateral.exception.CollateralCannotBeCreatedException.LOAN_COLLATERAL_CANNOT_BE_CREATED_REASON;
import org.apache.fineract.portfolio.collateral.exception.CollateralCannotBeDeletedException;
import org.apache.fineract.portfolio.collateral.exception.CollateralCannotBeDeletedException.LOAN_COLLATERAL_CANNOT_BE_DELETED_REASON;
import org.apache.fineract.portfolio.collateral.exception.CollateralCannotBeUpdatedException;
import org.apache.fineract.portfolio.collateral.exception.CollateralCannotBeUpdatedException.LOAN_COLLATERAL_CANNOT_BE_UPDATED_REASON;
import org.apache.fineract.portfolio.collateral.exception.CollateralNotFoundException;
import org.apache.fineract.portfolio.collateral.serialization.CollateralCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollateralWritePlatformServiceJpaRepositoryImpl implements CollateralWritePlatformService {

    private final PlatformSecurityContext context;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanCollateralRepository collateralRepository;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final CollateralCommandFromApiJsonDeserializer collateralCommandFromApiJsonDeserializer;

    @Transactional
    @Override
    public CommandProcessingResult addCollateral(final Long loanId, final JsonCommand command) {

        this.context.authenticatedUser();
        final CollateralCommand collateralCommand = this.collateralCommandFromApiJsonDeserializer.commandFromApiJson(command.json());
        collateralCommand.validateForCreate();

        try {
            final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
            final CodeValue collateralType = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                    CollateralApiConstants.COLLATERAL_CODE_NAME, collateralCommand.getCollateralTypeId());
            final LoanCollateral collateral = createNew(loan, collateralType, command);

            /**
             * Collaterals may be added only when the loan associated with them
             * are yet to be approved
             **/
            if (!loan.status().isSubmittedAndPendingApproval()) { throw new CollateralCannotBeCreatedException(
                    LOAN_COLLATERAL_CANNOT_BE_CREATED_REASON.LOAN_NOT_IN_SUBMITTED_AND_PENDING_APPROVAL_STAGE, loan.getId()); }

            this.collateralRepository.save(collateral);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .loanId(loan.getId())//
                    .resourceId(collateral.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleCollateralDataIntegrityViolation(dve);
            return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateCollateral(final Long loanId, final Long collateralId, final JsonCommand command) {

        this.context.authenticatedUser();
        final CollateralCommand collateralCommand = this.collateralCommandFromApiJsonDeserializer.commandFromApiJson(command.json());
        collateralCommand.validateForUpdate();

        final Long collateralTypeId = collateralCommand.getCollateralTypeId();
        try {
            final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
            CodeValue collateralType = null;

            final LoanCollateral collateralForUpdate = this.collateralRepository.findById(collateralId)
                    .orElseThrow(() -> new CollateralNotFoundException(loanId, collateralId));

            final Map<String, Object> changes = update(collateralForUpdate, command);

            if (changes.containsKey(COLLATERAL_JSON_INPUT_PARAMS.COLLATERAL_TYPE_ID.getValue())) {

                collateralType = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                        CollateralApiConstants.COLLATERAL_CODE_NAME, collateralTypeId);
                collateralForUpdate.setType(collateralType);
            }

            /**
             * Collaterals may be updated only when the loan associated with
             * them are yet to be approved
             **/
            if (!loan.status().isSubmittedAndPendingApproval()) { throw new CollateralCannotBeUpdatedException(
                    LOAN_COLLATERAL_CANNOT_BE_UPDATED_REASON.LOAN_NOT_IN_SUBMITTED_AND_PENDING_APPROVAL_STAGE, loan.getId()); }

            if (!changes.isEmpty()) {
                this.collateralRepository.saveAndFlush(collateralForUpdate);
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .loanId(command.getLoanId())//
                    .resourceId(collateralId) //
                    .changes(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleCollateralDataIntegrityViolation(dve);
            return CommandProcessingResult.builder().resourceId(-1L).build();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteCollateral(final Long loanId, final Long collateralId, final Long commandId) {
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true) ;
        final LoanCollateral collateral = this.collateralRepository.findByLoanIdAndId(loanId, collateralId);
        if (collateral == null) { throw new CollateralNotFoundException(loanId, collateralId); }

        /**
         * Collaterals may be deleted only when the loan associated with them
         * are yet to be approved
         **/
        if (!loan.status().isSubmittedAndPendingApproval()) { throw new CollateralCannotBeDeletedException(
                LOAN_COLLATERAL_CANNOT_BE_DELETED_REASON.LOAN_NOT_IN_SUBMITTED_AND_PENDING_APPROVAL_STAGE, loanId, collateralId); }

        this.collateralRepository.delete(collateral);
        return CommandProcessingResult.builder().commandId(commandId).loanId(loanId).resourceId(collateralId).build();
    }

    private void handleCollateralDataIntegrityViolation(final DataIntegrityViolationException dve) {
        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.collateral.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private LoanCollateral createNew(final Loan loan, final CodeValue collateralType, final JsonCommand command) {
        final String description = command.stringValueOfParameterNamed(COLLATERAL_JSON_INPUT_PARAMS.DESCRIPTION.getValue());
        final BigDecimal value = command.bigDecimalValueOfParameterNamed(COLLATERAL_JSON_INPUT_PARAMS.VALUE.getValue());

        return LoanCollateral.builder()
            .loan(loan)
            .type(collateralType)
            .value(value)
            .description(description)
            .build();
    }

    private Map<String, Object> update(LoanCollateral loanCollateral, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String collateralTypeIdParamName = COLLATERAL_JSON_INPUT_PARAMS.COLLATERAL_TYPE_ID.getValue();
        if (command.isChangeInLongParameterNamed(collateralTypeIdParamName, loanCollateral.getType().getId())) {
            final Long newValue = command.longValueOfParameterNamed(collateralTypeIdParamName);
            actualChanges.put(collateralTypeIdParamName, newValue);
        }

        final String descriptionParamName = COLLATERAL_JSON_INPUT_PARAMS.DESCRIPTION.getValue();
        if (command.isChangeInStringParameterNamed(descriptionParamName, loanCollateral.getDescription())) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            loanCollateral.setDescription(StringUtils.defaultIfEmpty(newValue, null));
        }

        final String valueParamName = COLLATERAL_JSON_INPUT_PARAMS.VALUE.getValue();
        if (command.isChangeInBigDecimalParameterNamed(valueParamName, loanCollateral.getValue())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(valueParamName);
            actualChanges.put(valueParamName, newValue);
            loanCollateral.setValue(newValue);
        }

        return actualChanges;
    }
}