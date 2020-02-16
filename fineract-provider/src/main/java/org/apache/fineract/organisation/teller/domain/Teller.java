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
package org.apache.fineract.organisation.teller.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.office.domain.Office;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_tellers", uniqueConstraints = {
        @UniqueConstraint(name = "ux_tellers_name", columnNames = {"name"})
})
public class Teller extends AbstractPersistableCustom<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "debit_account_id", nullable = true)
    private GLAccount debitAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "credit_account_id", nullable = true)
    private GLAccount creditAccount;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", nullable = true, length = 500)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "valid_from", nullable = true)
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "valid_to", nullable = true)
    private Date endDate;

    @Column(name = "state", nullable = false)
    private Integer status;

    @OneToMany(mappedBy = "teller", fetch = FetchType.LAZY)
    private Set<Cashier> cashiers;

    public static Teller fromJson(final Office tellerOffice, final JsonCommand command) {
        final String name = command.stringValueOfParameterNamed("name");
        final String description = command.stringValueOfParameterNamed("description");
        final LocalDate startDate = command.localDateValueOfParameterNamed("startDate");
        final LocalDate endDate = command.localDateValueOfParameterNamed("endDate");
        final Integer tellerStatusInt = command.integerValueOfParameterNamed("status");
        final TellerStatus status = TellerStatus.fromInt(tellerStatusInt);

        return Teller.builder()
            .office(tellerOffice)
            .name(name)
            .description(description)
            .startDate(startDate==null ? null : startDate.toDate())
            .endDate(endDate==null ? null : endDate.toDate())
            .status(status==null ? null : status.getValue())
            .build();
    }
    
    public Map<String, Object> update(Office tellerOffice, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();
        
        final String officeIdParamName = "officeId";
        if (command.isChangeInLongParameterNamed(officeIdParamName, this.getOffice().getId())) {
            final long newValue = command.longValueOfParameterNamed(officeIdParamName);
            actualChanges.put(officeIdParamName, newValue);
            this.office = tellerOffice;
        }

        final String nameParamName = "name";
        if (command.isChangeInStringParameterNamed(nameParamName, this.name)) {
            final String newValue = command.stringValueOfParameterNamed(nameParamName);
            actualChanges.put(nameParamName, newValue);
            this.name = newValue;
        }
        
        final String descriptionParamName = "description";
        if (command.isChangeInStringParameterNamed(descriptionParamName, this.description)) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            this.description = newValue;
        }

        final String startDateParamName = "startDate";
        if (command.isChangeInLocalDateParameterNamed(startDateParamName, getStartLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(startDateParamName);
            actualChanges.put(startDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(startDateParamName);
            this.startDate = newValue.toDate();
        }

        final String endDateParamName = "endDate";
        if (command.isChangeInLocalDateParameterNamed(endDateParamName, getEndLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(endDateParamName);
            actualChanges.put(endDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(endDateParamName);
            this.endDate = newValue.toDate();
        }
        
        final String statusParamName = "status";
        if (command.isChangeInIntegerParameterNamed(statusParamName, getStatus())) {
            final Integer valueAsInput = command.integerValueOfParameterNamed(statusParamName);
            actualChanges.put(statusParamName, valueAsInput);
            final Integer newValue = command.integerValueOfParameterNamed(statusParamName);
            final TellerStatus status = TellerStatus.fromInt(newValue);
            if (status != TellerStatus.INVALID) {
            	this.status = status.getValue(); 
            }
        }

        return actualChanges;
    }

    public LocalDate getStartLocalDate() {
        LocalDate startLocalDate = null;
        if (this.startDate != null) {
            startLocalDate = LocalDate.fromDateFields(this.startDate);
        }
        return startLocalDate;
    }

    public LocalDate getEndLocalDate() {
        LocalDate endLocalDate = null;
        if (this.endDate != null) {
            endLocalDate = LocalDate.fromDateFields(this.endDate);
        }
        return endLocalDate;
    }

    public void initializeLazyCollections() {
        this.office.getId();
        this.cashiers.size();
    }
}
