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
package org.apache.fineract.interoperation.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InteropTransactionsData extends CommandProcessingResult {
    List<InteropTransactionData> transactions;

    public static InteropTransactionsData build(SavingsAccount account, @NotNull Predicate<SavingsAccountTransaction> filter) {
        if (account == null)
            return null;

        List<InteropTransactionData> trans = account.getTransactions().stream().filter(filter).sorted((t1, t2) -> {
            int i = t2.getDateOf().compareTo(t1.getDateOf());
            return i != 0 ? i : Long.signum(t2.getId() - t1.getId());
        }).map(InteropTransactionData::build).collect(Collectors.toList());
        return InteropTransactionsData.builder()
            .resourceIdentifier(account.getId()+"")
            .resourceId(account.getId())
            .transactions(trans)
            .build();
    }
}
