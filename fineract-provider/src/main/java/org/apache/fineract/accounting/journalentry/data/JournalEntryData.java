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
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable object representing a General Ledger Account
 * 
 * Note: no getter/setters required as google will produce json from fields of
 * object.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class JournalEntryData {
    private Long id;
    private Long officeId;
    private String officeName;
    private String glAccountName;
    private Long glAccountId;
    private String glAccountCode;
    private EnumOptionData glAccountType;
    private LocalDate transactionDate;
    private EnumOptionData entryType;
    private BigDecimal amount;
    private CurrencyData currency;
    private String transactionId;
    private Boolean manualEntry;
    private EnumOptionData entityType;
    private Long entityId;
    private Long createdByUserId;
    private LocalDate createdDate;
    private String createdByUserName;
    private String comments;
    private Boolean reversed;
    private String referenceNumber;
    private BigDecimal officeRunningBalance;
    private BigDecimal organizationRunningBalance;
    private Boolean runningBalanceComputed;
    private TransactionDetailData transactionDetails;
    //import fields
    private transient Integer rowIndex;
    private String dateFormat;
    private String locale;
    private List<CreditDebit> credits;
    private List<CreditDebit> debits;
    private Long paymentTypeId;
    private String currencyCode;
    private String accountNumber;
    private String checkNumber;
    private String routingCode;
    private String receiptNumber;
    private String bankNumber;

    public void addDebits(CreditDebit debit) {
        if(this.debits==null) {
            this.debits = new ArrayList<>();
        }
        this.debits.add(debit);
    }

    public void addCredits(CreditDebit credit) {
        if(this.credits==null) {
            this.credits = new ArrayList<>();
        }
        this.credits.add(credit);
    }
}