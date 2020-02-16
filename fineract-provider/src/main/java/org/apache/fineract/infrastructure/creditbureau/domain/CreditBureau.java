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
package org.apache.fineract.infrastructure.creditbureau.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_creditbureau")
public class CreditBureau extends AbstractPersistableCustom<Long> {

	private String name;

	private String product;

	private String country;

	private String implementationKey;

	@Builder.Default
	@OneToMany(mappedBy = "organisation_creditbureau", cascade = CascadeType.ALL)
	private List<CreditBureauLoanProductMapping> CreditBureauLoanProductMapping = new ArrayList<>();

	public static CreditBureau fromJson(final JsonCommand command) {

		final String tname = command.stringValueOfParameterNamed("name");
		final String tproduct = command.stringValueOfParameterNamed("product");
		final String tcountry = command.stringValueOfParameterNamed("country");
		final String timplementationKey = command.stringValueOfParameterNamed("implementationKey");

		return CreditBureau.builder()
			.name(tname)
			.product(tproduct)
			.country(tcountry)
			.implementationKey(timplementationKey)
			.build();
	}
}
