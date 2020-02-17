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
import org.apache.fineract.interoperation.domain.InteropIdentifier;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InteropIdentifiersResponseData extends CommandProcessingResult {

    @NotNull
    private List<InteropIdentifierData> identifiers;

    public static InteropIdentifiersResponseData build(SavingsAccount account) {
        List<InteropIdentifierData> result = new ArrayList<>();
        if (account != null) {
            for (InteropIdentifier identifier : account.getIdentifiers()) {
                result.add(InteropIdentifierData.builder()
                    .idValue(identifier.getValue())
                    .idType(identifier.getType())
                    .subIdOrType(identifier.getSubValueOrType())
                    .build());
            }
        }
        return new InteropIdentifiersResponseData(result);
    }
}
