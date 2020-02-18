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
package org.apache.fineract.portfolio.client.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.address.domain.Address;

import javax.persistence.*;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_client_address")
public class ClientAddress extends AbstractPersistableCustom<Long> {

	@ManyToOne
	private Client client;

	@ManyToOne
	private Address address;

	@ManyToOne
	@JoinColumn(name = "address_type_id")
	private CodeValue addressType;

	@Column(name = "is_active")
	private boolean active;

	public static ClientAddress fromJson(final boolean isActive, final Client client, final Address address,
			final CodeValue address_type) {

		return new ClientAddress(client, address, address_type, isActive);
	}
}
