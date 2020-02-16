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
package org.apache.fineract.infrastructure.hooks.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static org.apache.fineract.infrastructure.hooks.api.HookApiConstants.nameParamName;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_hook_templates")
public class HookTemplate extends AbstractPersistableCustom<Long> {

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "template", orphanRemoval = true, fetch=FetchType.EAGER)
	private Set<Schema> fields = new HashSet<>();

	public static HookTemplate fromJson(final JsonCommand command) {
		final String name = command.stringValueOfParameterNamed(nameParamName);
		return HookTemplate.builder()
			.name(StringUtils.isEmpty(name) ? name : name.trim())
			.build();
	}
}
