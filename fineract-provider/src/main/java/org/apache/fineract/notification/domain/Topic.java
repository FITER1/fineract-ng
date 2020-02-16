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
package org.apache.fineract.notification.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "topic")
public class Topic extends AbstractPersistableCustom<Long> {

	@Column(name = "title", unique = true, nullable = false, length = 100)
	private String title;
	
	@Column(name = "enabled", nullable = false)
	private Boolean enabled;
	
	@Column(name = "entity_id", nullable = false)
	private Long entityId;
	
	@Column(name = "entity_type")
	private String entityType;
	
	@Column(name = "member_type")
	private String memberType;
	
	public static Topic fromJson(final JsonCommand command) {
		String title = "";
		Boolean enabled = null;
		Long entityId = 0L;
		String entityType = "";
		String memberType = "";
		
		if (command.hasParameter("title")) {
			title = command.stringValueOfParameterNamed("title");
		}
		if (command.hasParameter("enabled")) {
			enabled = command.booleanPrimitiveValueOfParameterNamed("enabled");
		}
		if (command.hasParameter("entityId")) {
			entityId = command.longValueOfParameterNamed("entityId");
		}
		if (command.hasParameter("entityType")) {
			entityType = command.stringValueOfParameterNamed("entityType");
		}
		if (command.hasParameter("memberType")) {
			memberType = command.stringValueOfParameterNamed("memberType");
		}
		return new Topic(title, enabled, entityId, entityType, memberType);
	}
}
