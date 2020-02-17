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

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.interoperation.domain.InteropActionState;
import org.joda.time.LocalDateTime;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InteropTransferResponseData extends InteropResponseData {
    @NotNull
    private String transferCode;
    private String completedTimestamp;

    private InteropTransferResponseData(Long resourceId, Long officeId, Long commandId, Map<String, Object> changesOnly,
                                        @NotNull String transactionCode, @NotNull InteropActionState state, LocalDateTime expiration,
                                        List<ExtensionData> extensionList, @NotNull String transferCode, LocalDateTime completedTimestamp) {
        super(resourceId, officeId, commandId, changesOnly, transactionCode, state, expiration, extensionList);
        this.transferCode = transferCode;
        this.completedTimestamp = completedTimestamp.toString(ISO_DATE_TIME_FORMATTER);
    }

    public static InteropTransferResponseData build(Long commandId, @NotNull String transactionCode, @NotNull InteropActionState state,
                                                    LocalDateTime expiration, List<ExtensionData> extensionList, @NotNull String transferCode,
                                                    LocalDateTime completedTimestamp) {
        return new InteropTransferResponseData(null, null, commandId, null, transactionCode, state, expiration, extensionList,
                transferCode, completedTimestamp);
    }
}
