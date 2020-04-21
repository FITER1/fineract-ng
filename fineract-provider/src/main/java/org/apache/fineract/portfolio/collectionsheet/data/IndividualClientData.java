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
package org.apache.fineract.portfolio.collectionsheet.data;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IndividualClientData implements Serializable {
    private Long clientId;
    private String clientName;
    private Collection<LoanDueData> loans;
    private Collection<SavingsDueData> savings;

    public void addLoans(LoanDueData loans) {
        if (this.loans == null) {
            this.loans = new ArrayList<>();
        }
        this.loans.add(loans);
    }

    public void addSavings(SavingsDueData savings) {
        if (this.savings == null) {
            this.savings = new ArrayList<>();
        }
        this.savings.add(savings);
    }
}