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
package org.apache.fineract.infrastructure.codes.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.exception.SystemDefinedCodeCannotBeChangedException;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import javax.persistence.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_code", uniqueConstraints = { @UniqueConstraint(columnNames = { "code_name" }, name = "code_name") })
public class Code extends AbstractPersistableCustom<Long> {

    @Column(name = "code_name", length = 100)
    private String name;

    @Column(name = "is_system_defined")
    private boolean systemDefined;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "code", orphanRemoval = true)
    private Set<CodeValue> values;

    public static Code fromJson(final JsonCommand command) {
        return Code.builder()
            .name(command.stringValueOfParameterNamed("name"))
            .systemDefined(false)
            .build();
    }

    public Map<String, Object> update(final JsonCommand command) {

        if (this.systemDefined) { throw new SystemDefinedCodeCannotBeChangedException(); }

        final Map<String, Object> actualChanges = new LinkedHashMap<>(1);

        final String firstnameParamName = "name";
        if (command.isChangeInStringParameterNamed(firstnameParamName, this.name)) {
            final String newValue = command.stringValueOfParameterNamed(firstnameParamName);
            actualChanges.put(firstnameParamName, newValue);
            this.name = StringUtils.defaultIfEmpty(newValue, null);
        }

        return actualChanges;
    }
}