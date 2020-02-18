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
package org.apache.fineract.portfolio.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants;
import org.apache.fineract.portfolio.account.data.AccountTransferDTO;
import org.apache.fineract.portfolio.account.data.StandingInstructionData;
import org.apache.fineract.portfolio.account.data.StandingInstructionDataValidator;
import org.apache.fineract.portfolio.account.data.StandingInstructionDuesData;
import org.apache.fineract.portfolio.account.domain.*;
import org.apache.fineract.portfolio.account.exception.StandingInstructionNotFoundException;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.DefaultScheduledDateGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.ScheduledDateGenerator;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.exception.InsufficientAccountBalanceException;
import org.joda.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.fineract.portfolio.account.AccountDetailConstants.*;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.statusParamName;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandingInstructionWritePlatformServiceImpl implements StandingInstructionWritePlatformService {

    private final StandingInstructionDataValidator standingInstructionDataValidator;
    private final StandingInstructionAssembler standingInstructionAssembler;
    private final AccountTransferDetailRepository accountTransferDetailRepository;
    private final StandingInstructionRepository standingInstructionRepository;
    private final StandingInstructionReadPlatformService standingInstructionReadPlatformService;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {

        this.standingInstructionDataValidator.validateForCreate(command);

        final Integer fromAccountTypeId = command.integerValueSansLocaleOfParameterNamed(fromAccountTypeParamName);
        final PortfolioAccountType fromAccountType = PortfolioAccountType.fromInt(fromAccountTypeId);

        final Integer toAccountTypeId = command.integerValueSansLocaleOfParameterNamed(toAccountTypeParamName);
        final PortfolioAccountType toAccountType = PortfolioAccountType.fromInt(toAccountTypeId);

        final Long fromClientId = command.longValueOfParameterNamed(fromClientIdParamName);

        Long standingInstructionId = null;
        try {
            if (isSavingsToSavingsAccountTransfer(fromAccountType, toAccountType)) {
                final AccountTransferDetails standingInstruction = this.standingInstructionAssembler
                        .assembleSavingsToSavingsTransfer(command);
                this.accountTransferDetailRepository.save(standingInstruction);
                standingInstructionId = standingInstruction.getAccountTransferStandingInstruction().getId();
            } else if (isSavingsToLoanAccountTransfer(fromAccountType, toAccountType)) {
                final AccountTransferDetails standingInstruction = this.standingInstructionAssembler.assembleSavingsToLoanTransfer(command);
                this.accountTransferDetailRepository.save(standingInstruction);
                standingInstructionId = standingInstruction.getAccountTransferStandingInstruction().getId();
            } else if (isLoanToSavingsAccountTransfer(fromAccountType, toAccountType)) {

                final AccountTransferDetails standingInstruction = this.standingInstructionAssembler.assembleLoanToSavingsTransfer(command);
                this.accountTransferDetailRepository.save(standingInstruction);
                standingInstructionId = standingInstruction.getAccountTransferStandingInstruction().getId();

            }
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve);
            return new CommandProcessingResult();
        }
        return CommandProcessingResult.builder()
            .resourceId(standingInstructionId)
            .clientId(fromClientId)
            .build();
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final DataIntegrityViolationException dve) {

        final Throwable realCause = dve.getMostSpecificCause();
        if (realCause.getMessage().contains("name")) {
            final String name = command.stringValueOfParameterNamed(StandingInstructionApiConstants.nameParamName);
            throw new PlatformDataIntegrityException("error.msg.standinginstruction.duplicate.name", "Standinginstruction with name `"
                    + name + "` already exists", "name", name);
        }
        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.client.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private boolean isLoanToSavingsAccountTransfer(final PortfolioAccountType fromAccountType, final PortfolioAccountType toAccountType) {
        return fromAccountType.isLoanAccount() && toAccountType.isSavingsAccount();
    }

    private boolean isSavingsToLoanAccountTransfer(final PortfolioAccountType fromAccountType, final PortfolioAccountType toAccountType) {
        return fromAccountType.isSavingsAccount() && toAccountType.isLoanAccount();
    }

    private boolean isSavingsToSavingsAccountTransfer(final PortfolioAccountType fromAccountType, final PortfolioAccountType toAccountType) {
        return fromAccountType.isSavingsAccount() && toAccountType.isSavingsAccount();
    }

    @Override
    public CommandProcessingResult update(final Long id, final JsonCommand command) {
        this.standingInstructionDataValidator.validateForUpdate(command);
        AccountTransferStandingInstruction standingInstructionsForUpdate = this.standingInstructionRepository.findById(id)
                .orElseThrow(() -> new StandingInstructionNotFoundException(id));
        final Map<String, Object> actualChanges = standingInstructionsForUpdate.update(command);
        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(id) //
                .changes(actualChanges) //
                .build();
    }

    @Override
    public CommandProcessingResult delete(final Long id) {
        AccountTransferStandingInstruction standingInstructionsForUpdate = this.standingInstructionRepository.findById(id).get();
        // update the "deleted" and "name" properties of the standing instruction
        standingInstructionsForUpdate.delete();
        
        final Map<String, Object> actualChanges = new HashMap<>();
        actualChanges.put(statusParamName, StandingInstructionStatus.DELETED.getValue());
        return CommandProcessingResult.builder() //
                .resourceId(id) //
                .changes(actualChanges) //
                .build();
    }

    @Override
    @CronTarget(jobName = JobName.EXECUTE_STANDING_INSTRUCTIONS)
    public void executeStandingInstructions() throws JobExecutionException {
        Collection<StandingInstructionData> instructionDatas = this.standingInstructionReadPlatformService
                .retrieveAll(StandingInstructionStatus.ACTIVE.getValue());
        final StringBuilder sb = new StringBuilder();
        for (StandingInstructionData data : instructionDatas) {
            boolean isDueForTransfer = false;
            AccountTransferRecurrenceType recurrenceType = data.recurrenceType();
            StandingInstructionType instructionType = data.instructionType();
            LocalDate transactionDate = LocalDate.now();
            if (recurrenceType.isPeriodicRecurrence()) {
                final ScheduledDateGenerator scheduledDateGenerator = new DefaultScheduledDateGenerator();
                PeriodFrequencyType frequencyType = data.recurrenceFrequency();
                LocalDate startDate = data.getValidFrom();
                if (frequencyType.isMonthly()) {
                    startDate = startDate.withDayOfMonth(data.recurrenceOnDay());
                    if (startDate.isBefore(data.getValidFrom())) {
                        startDate = startDate.plusMonths(1);
                    }
                } else if (frequencyType.isYearly()) {
                    startDate = startDate.withDayOfMonth(data.recurrenceOnDay()).withMonthOfYear(data.recurrenceOnMonth());
                    if (startDate.isBefore(data.getValidFrom())) {
                        startDate = startDate.plusYears(1);
                    }
                }
                isDueForTransfer = scheduledDateGenerator.isDateFallsInSchedule(frequencyType, data.getRecurrenceInterval(), startDate,
                        transactionDate);

            }
            BigDecimal transactionAmount = data.getAmount();
            if (data.toAccountType().isLoanAccount()
                    && (recurrenceType.isDuesRecurrence() || (isDueForTransfer && instructionType.isDuesAmoutTransfer()))) {
                StandingInstructionDuesData standingInstructionDuesData = this.standingInstructionReadPlatformService
                        .retriveLoanDuesData(data.getToAccount().getId());
                if (data.instructionType().isDuesAmoutTransfer()) {
                    transactionAmount = standingInstructionDuesData.getTotalDueAmount();
                }
                if (recurrenceType.isDuesRecurrence()) {
                    isDueForTransfer = LocalDate.now().equals(standingInstructionDuesData.getDueDate());
                }
            }

            if (isDueForTransfer && transactionAmount != null && transactionAmount.compareTo(BigDecimal.ZERO) > 0) {
                final SavingsAccount fromSavingsAccount = null;
                final boolean isRegularTransaction = true;
                final boolean isExceptionForBalanceCheck = false;
                AccountTransferDTO accountTransferDTO = AccountTransferDTO.builder()
                    .transactionDate(transactionDate)
                    .transactionAmount(transactionAmount)
                    .fromAccountType(data.fromAccountType())
                    .toAccountType(data.toAccountType())
                    .fromAccountId(data.getFromAccount().getId())
                    .toAccountId(data.getToAccount().getId())
                    .description(data.getName() + " Standing instruction transfer")
                    // skip 4
                    .toTransferType(data.toTransferType())
                    // skip 2
                    .transferType(data.transferType().getValue())
                    // skip 5
                    .fromSavingsAccount(fromSavingsAccount)
                    .regularTransaction(isRegularTransaction)
                    .exceptionForBalanceCheck(isExceptionForBalanceCheck)
                    .build();
                final boolean transferCompleted = transferAmount(sb, accountTransferDTO, data.getId());

                if(transferCompleted){
                    final String updateQuery = "UPDATE m_account_transfer_standing_instructions SET last_run_date = ? where id = ?";
                    this.jdbcTemplate.update(updateQuery, transactionDate.toDate(), data.getId());
                }

            }
        }
        if (sb.length() > 0) { throw new JobExecutionException(sb.toString()); }

    }

    /**
     * @param sb
     * @param accountTransferDTO
     */
    private boolean transferAmount(final StringBuilder sb, final AccountTransferDTO accountTransferDTO, final Long instructionId) {
        boolean transferCompleted = true;
        StringBuffer errorLog = new StringBuffer();
        StringBuffer updateQuery = new StringBuffer(
                "INSERT INTO `m_account_transfer_standing_instructions_history` (`standing_instruction_id`, `status`, `amount`,`execution_time`, `error_log`) VALUES (");
        try {
            this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
        } catch (final PlatformApiDataValidationException e) {
            sb.append("Validation exception while trasfering funds for standing Instruction id").append(instructionId).append(" from ")
                    .append(accountTransferDTO.getFromAccountId()).append(" to ").append(accountTransferDTO.getToAccountId())
                    .append("--------");
            errorLog.append("Validation exception while trasfering funds " + e.getDefaultUserMessage());
        } catch (final InsufficientAccountBalanceException e) {
            sb.append("InsufficientAccountBalance Exception while trasfering funds for standing Instruction id").append(instructionId)
                    .append(" from ").append(accountTransferDTO.getFromAccountId()).append(" to ")
                    .append(accountTransferDTO.getToAccountId()).append("--------");
            errorLog.append("InsufficientAccountBalance Exception ");
        } catch (final AbstractPlatformServiceUnavailableException e) {
            sb.append("Platform exception while trasfering funds for standing Instruction id").append(instructionId).append(" from ")
                    .append(accountTransferDTO.getFromAccountId()).append(" to ").append(accountTransferDTO.getToAccountId())
                    .append("--------");
            errorLog.append("Platform exception while trasfering funds " + e.getDefaultUserMessage());
        } catch (Exception e) {
            sb.append("Exception while trasfering funds for standing Instruction id").append(instructionId).append(" from ")
                    .append(accountTransferDTO.getFromAccountId()).append(" to ").append(accountTransferDTO.getToAccountId())
                    .append("--------");
            errorLog.append("Exception while trasfering funds " + e.getMessage());

        }
        updateQuery.append(instructionId).append(",");
        if (errorLog.length() > 0) {
            transferCompleted = false;
            updateQuery.append("'failed'").append(",");
        } else {
            updateQuery.append("'success'").append(",");
        }
        updateQuery.append(accountTransferDTO.getTransactionAmount().doubleValue());
        updateQuery.append(", now(),");
        updateQuery.append("'").append(errorLog.toString()).append("')");
        this.jdbcTemplate.update(updateQuery.toString());
        return transferCompleted;
    }
}