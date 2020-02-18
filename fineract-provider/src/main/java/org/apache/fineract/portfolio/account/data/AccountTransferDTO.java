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
package org.apache.fineract.portfolio.account.data;

import lombok.*;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetails;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.util.Locale;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AccountTransferDTO {
    private LocalDate transactionDate;
    private BigDecimal transactionAmount;
    private PortfolioAccountType fromAccountType;
    private PortfolioAccountType toAccountType;
    private Long fromAccountId;
    private Long toAccountId;
    private String description;
    private Locale locale;
    private DateTimeFormatter fmt;
    private PaymentDetail paymentDetail;
    private Integer fromTransferType;
    private Integer toTransferType;
    private Long chargeId;
    private Integer loanInstallmentNumber;
    private Integer transferType;
    private AccountTransferDetails accountTransferDetails;
    private String noteText;
    private String txnExternalId;
    private Loan loan;
    private Loan fromLoan;
    private Loan toLoan;
    private SavingsAccount toSavingsAccount;
    private SavingsAccount fromSavingsAccount;
    private Boolean regularTransaction;
    private Boolean exceptionForBalanceCheck;
}