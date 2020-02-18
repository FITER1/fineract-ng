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
package org.apache.fineract.portfolio.account.domain;

import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.springframework.stereotype.Service;

import java.util.Locale;

import static org.apache.fineract.portfolio.account.AccountDetailConstants.*;

@Service
@RequiredArgsConstructor
public class AccountTransferDetailAssembler {

    private final ClientRepositoryWrapper clientRepository;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final SavingsAccountAssembler savingsAccountAssembler;
    private final FromJsonHelper fromApiJsonHelper;
    private final LoanAssembler loanAccountAssembler;

    public AccountTransferDetails assembleSavingsToSavingsTransfer(final JsonCommand command) {

        final Long fromSavingsId = command.longValueOfParameterNamed(fromAccountIdParamName);
        final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(fromSavingsId);

        final Long toSavingsId = command.longValueOfParameterNamed(toAccountIdParamName);
        final SavingsAccount toSavingsAccount = this.savingsAccountAssembler.assembleFrom(toSavingsId);

        return assembleSavingsToSavingsTransfer(command, fromSavingsAccount, toSavingsAccount);

    }

    public AccountTransferDetails assembleSavingsToLoanTransfer(final JsonCommand command) {

        final Long fromSavingsAccountId = command.longValueOfParameterNamed(fromAccountIdParamName);
        final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(fromSavingsAccountId);

        final Long toLoanAccountId = command.longValueOfParameterNamed(toAccountIdParamName);
        final Loan toLoanAccount = this.loanAccountAssembler.assembleFrom(toLoanAccountId);

        return assembleSavingsToLoanTransfer(command, fromSavingsAccount, toLoanAccount);

    }

    public AccountTransferDetails assembleLoanToSavingsTransfer(final JsonCommand command) {

        final Long fromLoanAccountId = command.longValueOfParameterNamed(fromAccountIdParamName);
        final Loan fromLoanAccount = this.loanAccountAssembler.assembleFrom(fromLoanAccountId);

        final Long toSavingsAccountId = command.longValueOfParameterNamed(toAccountIdParamName);
        final SavingsAccount toSavingsAccount = this.savingsAccountAssembler.assembleFrom(toSavingsAccountId);

        return assembleLoanToSavingsTransfer(command, fromLoanAccount, toSavingsAccount);
    }

    public AccountTransferDetails assembleSavingsToSavingsTransfer(final JsonCommand command, final SavingsAccount fromSavingsAccount,
            final SavingsAccount toSavingsAccount) {

        final JsonElement element = command.parsedJson();

        final Long fromOfficeId = this.fromApiJsonHelper.extractLongNamed(fromOfficeIdParamName, element);
        final Office fromOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(fromOfficeId);

        final Long fromClientId = this.fromApiJsonHelper.extractLongNamed(fromClientIdParamName, element);
        final Client fromClient = this.clientRepository.findOneWithNotFoundDetection(fromClientId);

        final Long toOfficeId = this.fromApiJsonHelper.extractLongNamed(toOfficeIdParamName, element);
        final Office toOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(toOfficeId);

        final Long toClientId = this.fromApiJsonHelper.extractLongNamed(toClientIdParamName, element);
        final Client toClient = this.clientRepository.findOneWithNotFoundDetection(toClientId);

        final Integer transfertype = this.fromApiJsonHelper.extractIntegerNamed(transferTypeParamName, element, Locale.getDefault());

        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromSavingsAccount(fromSavingsAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toSavingsAccount(toSavingsAccount)
            .transferType(transfertype)
            .build();
    }

    public AccountTransferDetails assembleSavingsToLoanTransfer(final JsonCommand command, final SavingsAccount fromSavingsAccount,
            final Loan toLoanAccount) {

        final JsonElement element = command.parsedJson();

        final Long fromOfficeId = this.fromApiJsonHelper.extractLongNamed(fromOfficeIdParamName, element);
        final Office fromOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(fromOfficeId);

        final Long fromClientId = this.fromApiJsonHelper.extractLongNamed(fromClientIdParamName, element);
        final Client fromClient = this.clientRepository.findOneWithNotFoundDetection(fromClientId);

        final Long toOfficeId = this.fromApiJsonHelper.extractLongNamed(toOfficeIdParamName, element);
        final Office toOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(toOfficeId);

        final Long toClientId = this.fromApiJsonHelper.extractLongNamed(toClientIdParamName, element);
        final Client toClient = this.clientRepository.findOneWithNotFoundDetection(toClientId);

        final Integer transfertype = this.fromApiJsonHelper.extractIntegerNamed(transferTypeParamName, element, Locale.getDefault());

        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromSavingsAccount(fromSavingsAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toLoanAccount(toLoanAccount)
            .transferType(transfertype)
            .build();
    }

    public AccountTransferDetails assembleLoanToSavingsTransfer(final JsonCommand command, final Loan fromLoanAccount,
            final SavingsAccount toSavingsAccount) {

        final JsonElement element = command.parsedJson();

        final Long fromOfficeId = this.fromApiJsonHelper.extractLongNamed(fromOfficeIdParamName, element);
        final Office fromOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(fromOfficeId);

        final Long fromClientId = this.fromApiJsonHelper.extractLongNamed(fromClientIdParamName, element);
        final Client fromClient = this.clientRepository.findOneWithNotFoundDetection(fromClientId);

        final Long toOfficeId = this.fromApiJsonHelper.extractLongNamed(toOfficeIdParamName, element);
        final Office toOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(toOfficeId);

        final Long toClientId = this.fromApiJsonHelper.extractLongNamed(toClientIdParamName, element);
        final Client toClient = this.clientRepository.findOneWithNotFoundDetection(toClientId);
        final Integer transfertype = this.fromApiJsonHelper.extractIntegerNamed(transferTypeParamName, element, Locale.getDefault());

        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromLoanAccount(fromLoanAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toSavingsAccount(toSavingsAccount)
            .transferType(transfertype)
            .build();
    }

    public AccountTransferDetails assembleSavingsToLoanTransfer(final SavingsAccount fromSavingsAccount, final Loan toLoanAccount,
            Integer transferType) {
        final Office fromOffice = fromSavingsAccount.office();
        final Client fromClient = fromSavingsAccount.getClient();
        final Office toOffice = toLoanAccount.getOffice();
        final Client toClient = toLoanAccount.getClient();

        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromSavingsAccount(fromSavingsAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toLoanAccount(toLoanAccount)
            .transferType(transferType)
            .build();
    }

    public AccountTransferDetails assembleSavingsToSavingsTransfer(final SavingsAccount fromSavingsAccount,
            final SavingsAccount toSavingsAccount, Integer transferType) {
        final Office fromOffice = fromSavingsAccount.office();
        final Client fromClient = fromSavingsAccount.getClient();
        final Office toOffice = toSavingsAccount.office();
        final Client toClient = toSavingsAccount.getClient();

        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromSavingsAccount(fromSavingsAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toSavingsAccount(toSavingsAccount)
            .transferType(transferType)
            .build();
    }

    public AccountTransferDetails assembleLoanToSavingsTransfer(final Loan fromLoanAccount, final SavingsAccount toSavingsAccount,
            Integer transferType) {
        final Office fromOffice = fromLoanAccount.getOffice();
        final Client fromClient = fromLoanAccount.getClient();
        final Office toOffice = toSavingsAccount.office();
        final Client toClient = toSavingsAccount.getClient();

        return AccountTransferDetails.builder()
            .fromOffice(fromOffice)
            .fromClient(fromClient)
            .fromLoanAccount(fromLoanAccount)
            .toOffice(toOffice)
            .toClient(toClient)
            .toSavingsAccount(toSavingsAccount)
            .transferType(transferType)
            .build();
    }

    public AccountTransferDetails assembleLoanToLoanTransfer(Loan fromLoanAccount, Loan toLoanAccount, Integer transferType) {
        return AccountTransferDetails.builder()
            .fromOffice(fromLoanAccount.getOffice())
            .fromClient(fromLoanAccount.getClient())
            .fromLoanAccount(fromLoanAccount)
            .toOffice(toLoanAccount.getOffice())
            .toClient(toLoanAccount.getClient())
            .toLoanAccount(toLoanAccount)
            .transferType(transferType)
            .build();
    }
}