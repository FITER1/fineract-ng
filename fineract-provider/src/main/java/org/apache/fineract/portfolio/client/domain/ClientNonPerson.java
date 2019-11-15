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
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.util.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_client_non_person")
public class ClientNonPerson extends AbstractPersistableCustom<Long> {
	
	@OneToOne(optional = false)
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false, unique = true)
    private Client client;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constitution_cv_id", nullable = false)
    private CodeValue constitution;
	
	@Column(name = "incorp_no", length = 50, nullable = true)
	private String incorpNumber;
	
	@Column(name = "incorp_validity_till", nullable = true)
	@Temporal(TemporalType.DATE)
	private Date incorpValidityTill;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "main_business_line_cv_id", nullable = true)
    private CodeValue mainBusinessLine;
	
	@Column(name = "remarks", length = 150, nullable = true)
	private String remarks;
	

	public static ClientNonPerson createNew(final Client client, final CodeValue constitution, final CodeValue mainBusinessLine, String incorpNumber, LocalDate incorpValidityTill, String remarks) {
        validate(client, incorpValidityTill);

	    return ClientNonPerson.builder()
            .client(client)
            .constitution(constitution)
            .mainBusinessLine(mainBusinessLine)
            .incorpNumber(incorpNumber)
            .incorpValidityTill(incorpValidityTill==null ? null : incorpValidityTill.toDateTimeAtStartOfDay().toDate())
            .remarks(remarks)
            .build();
	}

	private static void validate(final Client client, LocalDate incorpValidityTill) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        if (incorpValidityTill!=null && client.dateOfBirthLocalDate() != null && client.dateOfBirthLocalDate().isAfter(incorpValidityTill)) {
            final String defaultUserMessage = "incorpvaliditytill date cannot be after the incorporation date";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.clients.incorpValidityTill.after.incorp.date", defaultUserMessage, ClientApiConstants.incorpValidityTillParamName, incorpValidityTill);
            dataValidationErrors.add(error);
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

	private LocalDate getIncorpValidityTillLocalDate() {
        LocalDate incorpValidityTillLocalDate = null;
        if (this.incorpValidityTill != null) {
            incorpValidityTillLocalDate = LocalDate.fromDateFields(this.incorpValidityTill);
        }
        return incorpValidityTillLocalDate;
    }

	private Long constitutionId() {
        Long constitutionId = null;
        if (this.constitution != null) {
            constitutionId = this.constitution.getId();
        }
        return constitutionId;
    }
	
	private Long mainBusinessLineId() {
        Long mainBusinessLineId = null;
        if (this.mainBusinessLine != null) {
            mainBusinessLineId = this.mainBusinessLine.getId();
        }
        return mainBusinessLineId;
    }
	
	public Map<String, Object> update(final JsonCommand command) {
		
		final Map<String, Object> actualChanges = new LinkedHashMap<>(9);
		
		if (command.isChangeInStringParameterNamed(ClientApiConstants.incorpNumberParamName, this.incorpNumber)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.incorpNumberParamName);
            actualChanges.put(ClientApiConstants.incorpNumberParamName, newValue);
            this.incorpNumber = StringUtils.defaultIfEmpty(newValue, null);
        }
		
		if (command.isChangeInStringParameterNamed(ClientApiConstants.remarksParamName, this.remarks)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.remarksParamName);
            actualChanges.put(ClientApiConstants.remarksParamName, newValue);
            this.remarks = StringUtils.defaultIfEmpty(newValue, null);
        }
		
		final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();
        
		if (command.isChangeInLocalDateParameterNamed(ClientApiConstants.incorpValidityTillParamName, getIncorpValidityTillLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(ClientApiConstants.incorpValidityTillParamName);
            actualChanges.put(ClientApiConstants.incorpValidityTillParamName, valueAsInput);
            actualChanges.put(ClientApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(ClientApiConstants.localeParamName, localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(ClientApiConstants.incorpValidityTillParamName);
            this.incorpValidityTill = newValue.toDate();
        }
		
		if (command.isChangeInLongParameterNamed(ClientApiConstants.constitutionIdParamName, constitutionId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.constitutionIdParamName);
            actualChanges.put(ClientApiConstants.constitutionIdParamName, newValue);
        }
		
		if (command.isChangeInLongParameterNamed(ClientApiConstants.mainBusinessLineIdParamName, mainBusinessLineId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.mainBusinessLineIdParamName);
            actualChanges.put(ClientApiConstants.mainBusinessLineIdParamName, newValue);
        }
		
		//validate();

        return actualChanges;
	}
}