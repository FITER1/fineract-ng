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
package org.apache.fineract.organisation.monetary.data;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CurrencyData {
    private String code;
    private String name;
    @Builder.Default
    private Integer decimalPlaces = 0;
    private Integer inMultiplesOf;
    private String displaySymbol;
    private String nameCode;
    private String displayLabel;

    public CurrencyData(final String code, final String name, final int decimalPlaces, final Integer inMultiplesOf, final String displaySymbol, final String nameCode) {
        this.code = code;
        this.name = name;
        this.decimalPlaces = decimalPlaces;
        this.inMultiplesOf = inMultiplesOf;
        this.displaySymbol = displaySymbol;
        this.nameCode = nameCode;
        this.displayLabel = generateDisplayLabel();
    }

    private String generateDisplayLabel() {

        final StringBuilder builder = new StringBuilder(this.name).append(' ');

        if (this.displaySymbol != null && !"".equalsIgnoreCase(this.displaySymbol.trim())) {
            builder.append('(').append(this.displaySymbol).append(')');
        } else {
            builder.append('[').append(this.code).append(']');
        }

        return builder.toString();
    }
}