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
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.joda.time.LocalDate;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OfficeOpeningBalancesData {
    private Long officeId;
    private String officeName;
    private LocalDate transactionDate;
    private GLAccountData contraAccount;
    private List<JournalEntryData> assetAccountOpeningBalances;
    private List<JournalEntryData> liabityAccountOpeningBalances;
    private List<JournalEntryData> incomeAccountOpeningBalances;
    private List<JournalEntryData> equityAccountOpeningBalances;
    private List<JournalEntryData> expenseAccountOpeningBalances;
}
