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
package org.apache.fineract.accounting.journalentry.data;

import lombok.*;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionEnumData;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LoanTransactionDTO {
    private Long officeId;
    private String transactionId;
    private Date transactionDate;
    private Long paymentTypeId;
    private LoanTransactionEnumData transactionType;
    private BigDecimal amount;
    /*** Breakup of amounts in case of repayments **/
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal fees;
    private BigDecimal penalties;
    private BigDecimal overPayment;
    /*** Boolean values determines if the transaction is reversed ***/
    private boolean reversed;
    /** Breakdowns of fees and penalties this Transaction pays **/
    private List<ChargePaymentDTO> penaltyPayments;
    private List<ChargePaymentDTO> feePayments;
    private boolean isAccountTransfer;
    private boolean isLoanToLoanTransfer;
}
