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
package org.apache.fineract.portfolio.charge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityAccessType;
import org.apache.fineract.infrastructure.entityaccess.service.FineractEntityAccessUtil;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.charge.api.ChargesApiConstants;
import org.apache.fineract.portfolio.charge.domain.*;
import org.apache.fineract.portfolio.charge.exception.*;
import org.apache.fineract.portfolio.charge.serialization.ChargeDefinitionCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.tax.domain.TaxGroup;
import org.apache.fineract.portfolio.tax.domain.TaxGroupRepositoryWrapper;
import org.joda.time.MonthDay;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeWritePlatformServiceJpaRepositoryImpl implements ChargeWritePlatformService {

    private final PlatformSecurityContext context;
    private final ChargeDefinitionCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ChargeRepository chargeRepository;
    private final LoanProductRepository loanProductRepository;
    private final FineractEntityAccessUtil fineractEntityAccessUtil;
    private final GLAccountRepositoryWrapper gLAccountRepository;
    private final TaxGroupRepositoryWrapper taxGroupRepository;

    @Transactional
    @Override
    @CacheEvict(value = "charges", key = "@fineractProperties.getTenantId().concat('ch')")
    public CommandProcessingResult createCharge(final JsonCommand command) {
        try {
            this.context.authenticatedUser();
            this.fromApiJsonDeserializer.validateForCreate(command.json());

            // Retrieve linked GLAccount for Client charges (if present)
            final Long glAccountId = command.longValueOfParameterNamed(ChargesApiConstants.glAccountIdParamName);

            GLAccount glAccount = null;
            if (glAccountId != null) {
                glAccount = this.gLAccountRepository.findOneWithNotFoundDetection(glAccountId);
            }

            final Long taxGroupId = command.longValueOfParameterNamed(ChargesApiConstants.taxGroupIdParamName);
            TaxGroup taxGroup = null;
            if (taxGroupId != null) {
                taxGroup = this.taxGroupRepository.findOneWithNotFoundDetection(taxGroupId);
            }

            final Charge charge = fromJson(command, glAccount, taxGroup);
            this.chargeRepository.save(charge);

            // check if the office specific products are enabled. If yes, then
            // save this savings product against a specific office
            // i.e. this savings product is specific for this office.
            fineractEntityAccessUtil.checkConfigurationAndAddProductResrictionsForUserOffice(
                    FineractEntityAccessType.OFFICE_ACCESS_TO_CHARGES, charge.getId());

            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(charge.getId()).build();
        }catch (final DataIntegrityViolationException dve) {
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
    @CacheEvict(value = "charges", key = "@fineractProperties.getTenantId().concat('ch')")
    public CommandProcessingResult updateCharge(final Long chargeId, final JsonCommand command) {

        try {
            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            final Charge chargeForUpdate = this.chargeRepository.findById(chargeId)
                    .orElseThrow(() -> new ChargeNotFoundException(chargeId));

            final Map<String, Object> changes = update(chargeForUpdate, command);

            this.fromApiJsonDeserializer.validateChargeTimeNCalculationType(chargeForUpdate.getChargeTimeType(),
                    chargeForUpdate.getChargeCalculation());

            // MIFOSX-900: Check if the Charge has been active before and now is
            // deactivated:
            if (changes.containsKey("active")) {
                // IF the key exists then it has changed (otherwise it would
                // have been filtered), so check current state:
                if (!chargeForUpdate.isActive()) {
                    // TODO: Change this function to only check the mappings!!!
                    final Boolean isChargeExistWithLoans = isAnyLoanProductsAssociateWithThisCharge(chargeId);
                    final Boolean isChargeExistWithSavings = isAnySavingsProductsAssociateWithThisCharge(chargeId);

                    if (isChargeExistWithLoans || isChargeExistWithSavings) { throw new ChargeCannotBeUpdatedException(
                            "error.msg.charge.cannot.be.updated.it.is.used.in.loan", "This charge cannot be updated, it is used in loan"); }
                }
            } else if ((changes.containsKey("feeFrequency") || changes.containsKey("feeInterval")) && chargeForUpdate.isLoanCharge()) {
                final Boolean isChargeExistWithLoans = isAnyLoanProductsAssociateWithThisCharge(chargeId);
                if (isChargeExistWithLoans) { throw new ChargeCannotBeUpdatedException(
                        "error.msg.charge.frequency.cannot.be.updated.it.is.used.in.loan",
                        "This charge frequency cannot be updated, it is used in loan"); }
            }

            // Has account Id been changed ?
            if (changes.containsKey(ChargesApiConstants.glAccountIdParamName)) {
                final Long newValue = command.longValueOfParameterNamed(ChargesApiConstants.glAccountIdParamName);
                GLAccount newIncomeAccount = null;
                if (newValue != null) {
                    newIncomeAccount = this.gLAccountRepository.findOneWithNotFoundDetection(newValue);
                }
                chargeForUpdate.setAccount(newIncomeAccount);
            }

            if (changes.containsKey(ChargesApiConstants.taxGroupIdParamName)) {
                final Long newValue = command.longValueOfParameterNamed(ChargesApiConstants.taxGroupIdParamName);
                TaxGroup taxGroup = null;
                if (newValue != null) {
                    taxGroup = this.taxGroupRepository.findOneWithNotFoundDetection(newValue);
                }
                chargeForUpdate.setTaxGroup(taxGroup);
            }

            if (!changes.isEmpty()) {
                this.chargeRepository.save(chargeForUpdate);
            }

            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(chargeId).changes(changes).build();
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
    @CacheEvict(value = "charges", key = "@fineractProperties.getTenantId().concat('ch')")
    public CommandProcessingResult deleteCharge(final Long chargeId) {

        final Charge chargeForDelete = this.chargeRepository.findById(chargeId)
                .orElseThrow(() -> new ChargeNotFoundException(chargeId));
        if (chargeForDelete.isDeleted()) { throw new ChargeNotFoundException(chargeId); }

        final Collection<LoanProduct> loanProducts = this.loanProductRepository.retrieveLoanProductsByChargeId(chargeId);
        final Boolean isChargeExistWithLoans = isAnyLoansAssociateWithThisCharge(chargeId);
        final Boolean isChargeExistWithSavings = isAnySavingsAssociateWithThisCharge(chargeId);

        // TODO: Change error messages around:
        if (!loanProducts.isEmpty() || isChargeExistWithLoans || isChargeExistWithSavings) { throw new ChargeCannotBeDeletedException(
                "error.msg.charge.cannot.be.deleted.it.is.already.used.in.loan",
                "This charge cannot be deleted, it is already used in loan"); }

        chargeForDelete.toBuilder()
            .name(chargeForDelete.getId() + "_" + chargeForDelete.getName())
            .deleted(true)
            .build();

        this.chargeRepository.save(chargeForDelete);

        return CommandProcessingResult.builder().resourceId(chargeForDelete.getId()).build();
    }

    private Map<String, Object> update(final Charge charge, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String localeAsInput = command.locale();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("charges");

        final String nameParamName = "name";
        if (command.isChangeInStringParameterNamed(nameParamName, charge.getName())) {
            final String newValue = command.stringValueOfParameterNamed(nameParamName);
            actualChanges.put(nameParamName, newValue);
            charge.setName(newValue);
        }

        final String currencyCodeParamName = "currencyCode";
        if (command.isChangeInStringParameterNamed(currencyCodeParamName, charge.getCurrencyCode())) {
            final String newValue = command.stringValueOfParameterNamed(currencyCodeParamName);
            actualChanges.put(currencyCodeParamName, newValue);
            charge.setCurrencyCode(newValue);
        }

        final String amountParamName = "amount";
        if (command.isChangeInBigDecimalParameterNamed(amountParamName, charge.getAmount())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(amountParamName);
            actualChanges.put(amountParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            charge.setAmount(newValue);
        }

        final String chargeTimeParamName = "chargeTimeType";
        if (command.isChangeInIntegerParameterNamed(chargeTimeParamName, charge.getChargeTimeType())) {
            final Integer newValue = command.integerValueOfParameterNamed(chargeTimeParamName);
            actualChanges.put(chargeTimeParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            charge.setChargeTimeType(ChargeTimeType.fromInt(newValue).getValue());

            if (charge.isSavingsCharge()) {
                if (!charge.isAllowedSavingsChargeTime()) {
                    baseDataValidator.reset().parameter("chargeTimeType").value(charge.getChargeTimeType())
                        .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.time.for.savings");
                }
                // if charge time is changed to monthly then validate for
                // feeOnMonthDay and feeInterval
                if (charge.isMonthlyFee()) {
                    final MonthDay monthDay = command.extractMonthDayNamed("feeOnMonthDay");
                    baseDataValidator.reset().parameter("feeOnMonthDay").value(monthDay).notNull();

                    final Integer feeInterval = command.integerValueOfParameterNamed("feeInterval");
                    baseDataValidator.reset().parameter("feeInterval").value(feeInterval).notNull().inMinMaxRange(1, 12);
                }
            } else if (charge.isLoanCharge()) {
                if (!charge.isAllowedLoanChargeTime()) {
                    baseDataValidator.reset().parameter("chargeTimeType").value(charge.getChargeTimeType())
                        .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.time.for.loan");
                }
            } else if (charge.isClientCharge()) {
                if (!charge.isAllowedLoanChargeTime()) {
                    baseDataValidator.reset().parameter("chargeTimeType").value(charge.getChargeTimeType())
                        .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.time.for.client");
                }
            }
        }

        final String chargeAppliesToParamName = "chargeAppliesTo";
        if (command.isChangeInIntegerParameterNamed(chargeAppliesToParamName, charge.getChargeAppliesTo())) {
            /*
             * final Integer newValue =
             * command.integerValueOfParameterNamed(chargeAppliesToParamName);
             * actualChanges.put(chargeAppliesToParamName, newValue);
             * actualChanges.put("locale", localeAsInput); charge.getChargeAppliesTo()
             * = ChargeAppliesTo.fromInt(newValue).getValue();
             */

            // AA: Do not allow to change chargeAppliesTo.
            final String errorMessage = "Update of Charge applies to is not supported";
            throw new ChargeParameterUpdateNotSupportedException("charge.applies.to", errorMessage);
        }

        final String chargeCalculationParamName = "chargeCalculationType";
        if (command.isChangeInIntegerParameterNamed(chargeCalculationParamName, charge.getChargeCalculation())) {
            final Integer newValue = command.integerValueOfParameterNamed(chargeCalculationParamName);
            actualChanges.put(chargeCalculationParamName, newValue);
            actualChanges.put("locale", localeAsInput);
            charge.setChargeCalculation(ChargeCalculationType.fromInt(newValue).getValue());

            if (charge.isSavingsCharge()) {
                if (!charge.isAllowedSavingsChargeCalculationType()) {
                    baseDataValidator.reset().parameter("chargeCalculationType").value(charge.getChargeCalculation())
                        .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.calculation.type.for.savings");
                }

                if (!(ChargeTimeType.fromInt(charge.getChargeTimeType()).isWithdrawalFee() || ChargeTimeType.fromInt(charge.getChargeTimeType()).isSavingsNoActivityFee())
                    && ChargeCalculationType.fromInt(charge.getChargeCalculation()).isPercentageOfAmount()) {
                    baseDataValidator.reset().parameter("chargeCalculationType").value(charge.getChargeCalculation())
                        .failWithCodeNoParameterAddedToErrorCode("charge.calculation.type.percentage.allowed.only.for.withdrawal.or.noactivity");
                }
            } else if (charge.isClientCharge()) {
                if (!charge.isAllowedClientChargeCalculationType()) {
                    baseDataValidator.reset().parameter("chargeCalculationType").value(charge.getChargeCalculation())
                        .failWithCodeNoParameterAddedToErrorCode("not.allowed.charge.calculation.type.for.client");
                }
            }
        }

        if (charge.isLoanCharge()) {// validate only for loan charge
            final String paymentModeParamName = "chargePaymentMode";
            if (command.isChangeInIntegerParameterNamed(paymentModeParamName, charge.getChargePaymentMode())) {
                final Integer newValue = command.integerValueOfParameterNamed(paymentModeParamName);
                actualChanges.put(paymentModeParamName, newValue);
                actualChanges.put("locale", localeAsInput);
                charge.setChargePaymentMode(ChargePaymentMode.fromInt(newValue).getValue());
            }
        }

        if (command.hasParameter("feeOnMonthDay")) {
            final MonthDay monthDay = command.extractMonthDayNamed("feeOnMonthDay");
            final String actualValueEntered = command.stringValueOfParameterNamed("feeOnMonthDay");
            final Integer dayOfMonthValue = monthDay.getDayOfMonth();
            if (charge.getFeeOnDay() != dayOfMonthValue) {
                actualChanges.put("feeOnMonthDay", actualValueEntered);
                actualChanges.put("locale", localeAsInput);
                charge.setFeeOnDay(dayOfMonthValue);
            }

            final Integer monthOfYear = monthDay.getMonthOfYear();
            if (charge.getFeeOnMonth() != monthOfYear) {
                actualChanges.put("feeOnMonthDay", actualValueEntered);
                actualChanges.put("locale", localeAsInput);
                charge.setFeeOnMonth(monthOfYear);
            }
        }

        final String feeInterval = "feeInterval";
        if (command.isChangeInIntegerParameterNamed(feeInterval, charge.getFeeInterval())) {
            final Integer newValue = command.integerValueOfParameterNamed(feeInterval);
            actualChanges.put(feeInterval, newValue);
            actualChanges.put("locale", localeAsInput);
            charge.setFeeInterval(newValue);
        }

        final String feeFrequency = "feeFrequency";
        if (command.isChangeInIntegerParameterNamed(feeFrequency, charge.getFeeFrequency())) {
            final Integer newValue = command.integerValueOfParameterNamed(feeFrequency);
            actualChanges.put(feeFrequency, newValue);
            actualChanges.put("locale", localeAsInput);
            charge.setFeeFrequency(newValue);
        }

        if (charge.getFeeFrequency() != null) {
            baseDataValidator.reset().parameter("feeInterval").value(charge.getFeeInterval()).notNull();
        }

        final String penaltyParamName = "penalty";
        if (command.isChangeInBooleanParameterNamed(penaltyParamName, charge.isPenalty())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(penaltyParamName);
            actualChanges.put(penaltyParamName, newValue);
            charge.setPenalty(newValue);
        }

        final String activeParamName = "active";
        if (command.isChangeInBooleanParameterNamed(activeParamName, charge.isActive())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(activeParamName);
            actualChanges.put(activeParamName, newValue);
            charge.setActive(newValue);
        }
        // allow min and max cap to be only added to PERCENT_OF_AMOUNT for now
        if (charge.isPercentageOfApprovedAmount()) {
            final String minCapParamName = "minCap";
            if (command.isChangeInBigDecimalParameterNamed(minCapParamName, charge.getMinCap())) {
                final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(minCapParamName);
                actualChanges.put(minCapParamName, newValue);
                actualChanges.put("locale", localeAsInput);
                charge.setMinCap(newValue);
            }
            final String maxCapParamName = "maxCap";
            if (command.isChangeInBigDecimalParameterNamed(maxCapParamName, charge.getMaxCap())) {
                final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(maxCapParamName);
                actualChanges.put(maxCapParamName, newValue);
                actualChanges.put("locale", localeAsInput);
                charge.setMaxCap(newValue);
            }

        }

        if (charge.isPenalty() && ChargeTimeType.fromInt(charge.getChargeTimeType()).isTimeOfDisbursement()) { throw new ChargeDueAtDisbursementCannotBePenaltyException(
            charge.getName()); }
        if (!charge.isPenalty() && ChargeTimeType.fromInt(charge.getChargeTimeType()).isOverdueInstallment()) { throw new ChargeMustBePenaltyException(charge.getName()); }

        if (command.isChangeInLongParameterNamed(ChargesApiConstants.glAccountIdParamName, charge.getIncomeAccountId())) {
            final Long newValue = command.longValueOfParameterNamed(ChargesApiConstants.glAccountIdParamName);
            actualChanges.put(ChargesApiConstants.glAccountIdParamName, newValue);
        }

        if (command.isChangeInLongParameterNamed(ChargesApiConstants.taxGroupIdParamName, charge.getTaxGroupId())) {
            final Long newValue = command.longValueOfParameterNamed(ChargesApiConstants.taxGroupIdParamName);
            actualChanges.put(ChargesApiConstants.taxGroupIdParamName, newValue);
            if(charge.getTaxGroup() != null){
                baseDataValidator.reset().parameter(ChargesApiConstants.taxGroupIdParamName).failWithCode("modification.not.supported");
            }
        }

        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }

        return actualChanges;
    }

    private static Charge fromJson(final JsonCommand command, final GLAccount account, final TaxGroup taxGroup) {

        final String name = command.stringValueOfParameterNamed("name");
        final BigDecimal amount = command.bigDecimalValueOfParameterNamed("amount");
        final String currencyCode = command.stringValueOfParameterNamed("currencyCode");

        final ChargeAppliesTo chargeAppliesTo = ChargeAppliesTo.fromInt(command.integerValueOfParameterNamed("chargeAppliesTo"));
        final ChargeTimeType chargeTimeType = ChargeTimeType.fromInt(command.integerValueOfParameterNamed("chargeTimeType"));
        final ChargeCalculationType chargeCalculationType = ChargeCalculationType.fromInt(command
            .integerValueOfParameterNamed("chargeCalculationType"));
        final Integer chargePaymentMode = command.integerValueOfParameterNamed("chargePaymentMode");

        final ChargePaymentMode paymentMode = chargePaymentMode == null ? null : ChargePaymentMode.fromInt(chargePaymentMode);

        final boolean penalty = command.booleanPrimitiveValueOfParameterNamed("penalty");
        final boolean active = command.booleanPrimitiveValueOfParameterNamed("active");
        final MonthDay feeOnMonthDay = command.extractMonthDayNamed("feeOnMonthDay");
        final Integer feeInterval = command.integerValueOfParameterNamed("feeInterval");
        final BigDecimal minCap = command.bigDecimalValueOfParameterNamed("minCap");
        final BigDecimal maxCap = command.bigDecimalValueOfParameterNamed("maxCap");
        final Integer feeFrequency = command.integerValueOfParameterNamed("feeFrequency");

        return Charge.builder()
            .name(name)
            .amount(amount)
            .currencyCode(currencyCode)
            .chargeAppliesTo(chargeAppliesTo.getValue())
            .chargeTimeType(chargeTimeType.getValue())
            .chargeCalculation(chargeCalculationType.getValue())
            .penalty(penalty)
            .active(active)
            .chargePaymentMode(paymentMode.getValue())
            .feeOnDay(feeOnMonthDay.getDayOfMonth())
            .feeInterval(feeInterval)
            .minCap(minCap)
            .maxCap(maxCap)
            .feeFrequency(feeFrequency)
            .account(account)
            .taxGroup(taxGroup)
            .build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("name")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.charge.duplicate.name", "Charge with name `" + name + "` already exists",
                    "name", name);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.charge.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }

    private boolean isAnyLoansAssociateWithThisCharge(final Long chargeId) {

        final String sql = "select if((exists (select 1 from m_loan_charge lc where lc.charge_id = ? and lc.is_active = 1)) = 1, 'true', 'false')";
        final String isLoansUsingCharge = this.jdbcTemplate.queryForObject(sql, String.class, new Object[] { chargeId });
        return new Boolean(isLoansUsingCharge);
    }

    private boolean isAnySavingsAssociateWithThisCharge(final Long chargeId) {

        final String sql = "select if((exists (select 1 from m_savings_account_charge sc where sc.charge_id = ? and sc.is_active = 1)) = 1, 'true', 'false')";
        final String isSavingsUsingCharge = this.jdbcTemplate.queryForObject(sql, String.class, new Object[] { chargeId });
        return new Boolean(isSavingsUsingCharge);
    }

    private boolean isAnyLoanProductsAssociateWithThisCharge(final Long chargeId) {

        final String sql = "select if((exists (select 1 from m_product_loan_charge lc where lc.charge_id = ?)) = 1, 'true', 'false')";
        final String isLoansUsingCharge = this.jdbcTemplate.queryForObject(sql, String.class, new Object[] { chargeId });
        return new Boolean(isLoansUsingCharge);
    }

    private boolean isAnySavingsProductsAssociateWithThisCharge(final Long chargeId) {

        final String sql = "select if((exists (select 1 from m_savings_product_charge sc where sc.charge_id = ?)) = 1, 'true', 'false')";
        final String isSavingsUsingCharge = this.jdbcTemplate.queryForObject(sql, String.class, new Object[] { chargeId });
        return new Boolean(isSavingsUsingCharge);
    }
}
