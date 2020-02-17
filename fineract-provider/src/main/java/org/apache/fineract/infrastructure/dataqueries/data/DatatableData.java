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
package org.apache.fineract.infrastructure.dataqueries.data;

import lombok.*;

import java.util.List;
import java.util.Objects;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DatatableData {
    private String applicationTableName;
    private String registeredTableName;
    private List<ResultsetColumnHeaderData> columnHeaderData;

    public boolean hasColumn(final String columnName) {
        if(columnHeaderData==null) {
            return false;
        }

        return Objects.requireNonNull(columnHeaderData).stream().anyMatch(data -> data.getColumnName().equals(columnName));
    }
}