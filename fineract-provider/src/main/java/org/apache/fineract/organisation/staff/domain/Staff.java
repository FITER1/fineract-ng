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
package org.apache.fineract.organisation.staff.domain;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.documentmanagement.domain.Image;
import org.apache.fineract.organisation.office.domain.Office;
import org.joda.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_staff", uniqueConstraints = { @UniqueConstraint(columnNames = { "display_name" }, name = "display_name"),
        @UniqueConstraint(columnNames = { "external_id" }, name = "external_id_UNIQUE"),
        @UniqueConstraint(columnNames = { "mobile_no" }, name = "mobile_no_UNIQUE") })
public class Staff extends AbstractPersistableCustom<Long> {

    @Column(name = "firstname", length = 50)
    private String firstname;

    @Column(name = "lastname", length = 50)
    private String lastname;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "mobile_no", length = 50, nullable = false, unique = true)
    private String mobileNo;

    @Column(name = "external_id", length = 100, nullable = true, unique = true)
    private String externalId;

	@Column(name = "email_address", length = 50, unique = true)
    private String emailAddress;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Column(name = "is_loan_officer", nullable = false)
    private boolean loanOfficer;

    @Column(name = "organisational_role_enum", nullable = true)
    private Integer organisationalRoleType;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "joining_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date joiningDate;

    @ManyToOne
    @JoinColumn(name = "organisational_role_parent_staff_id", nullable = true)
    private Staff organisationalRoleParentStaff;

    @OneToOne(optional = true)
    @JoinColumn(name = "image_id", nullable = true)
    private Image image;

    public static Staff fromJson(final Office staffOffice, final JsonCommand command) {

        final String firstnameParamName = "firstname";
        final String firstname = command.stringValueOfParameterNamed(firstnameParamName);

        final String lastnameParamName = "lastname";
        final String lastname = command.stringValueOfParameterNamed(lastnameParamName);

        final String externalIdParamName = "externalId";
        final String externalId = command.stringValueOfParameterNamedAllowingNull(externalIdParamName);

        final String mobileNoParamName = "mobileNo";
        final String mobileNo = command.stringValueOfParameterNamedAllowingNull(mobileNoParamName);

        final String isLoanOfficerParamName = "isLoanOfficer";
        final boolean isLoanOfficer = command.booleanPrimitiveValueOfParameterNamed(isLoanOfficerParamName);

        final String isActiveParamName = "isActive";
        final Boolean isActive = command.booleanObjectValueOfParameterNamed(isActiveParamName);

        LocalDate joiningDate = null;

        final String joiningDateParamName = "joiningDate";
        if (command.hasParameter(joiningDateParamName)) {
            joiningDate = command.localDateValueOfParameterNamed(joiningDateParamName);
        }

        return Staff.builder()
            .office(staffOffice)
            .firstname(firstname)
            .lastname(lastname)
            .externalId(externalId)
            .mobileNo(mobileNo)
            .loanOfficer(isLoanOfficer)
            .active(isActive)
            .joiningDate(joiningDate==null ? null : joiningDate.toDate())
            .displayName(deriveDisplayName(firstname, lastname))
            .build();
    }

    public EnumOptionData organisationalRoleData() {
        EnumOptionData organisationalRole = null;
        if (this.organisationalRoleType != null) {
            organisationalRole = StaffEnumerations.organisationalRole(this.organisationalRoleType);
        }
        return organisationalRole;
    }

    public void changeOffice(final Office newOffice) {
        this.office = newOffice;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String officeIdParamName = "officeId";
        if (command.isChangeInLongParameterNamed(officeIdParamName, this.office.getId())) {
            final Long newValue = command.longValueOfParameterNamed(officeIdParamName);
            actualChanges.put(officeIdParamName, newValue);
        }

        boolean firstnameChanged = false;
        final String firstnameParamName = "firstname";
        if (command.isChangeInStringParameterNamed(firstnameParamName, this.firstname)) {
            final String newValue = command.stringValueOfParameterNamed(firstnameParamName);
            actualChanges.put(firstnameParamName, newValue);
            this.firstname = newValue;
            firstnameChanged = true;
        }

        boolean lastnameChanged = false;
        final String lastnameParamName = "lastname";
        if (command.isChangeInStringParameterNamed(lastnameParamName, this.lastname)) {
            final String newValue = command.stringValueOfParameterNamed(lastnameParamName);
            actualChanges.put(lastnameParamName, newValue);
            this.lastname = newValue;
            lastnameChanged = true;
        }

        if (firstnameChanged || lastnameChanged) {
            this.displayName = deriveDisplayName(this.firstname, this.lastname);
        }

        final String externalIdParamName = "externalId";
        if (command.isChangeInStringParameterNamed(externalIdParamName, this.externalId)) {
            final String newValue = command.stringValueOfParameterNamed(externalIdParamName);
            actualChanges.put(externalIdParamName, newValue);
            this.externalId = newValue;
        }

        final String mobileNoParamName = "mobileNo";
        if (command.isChangeInStringParameterNamed(mobileNoParamName, this.mobileNo)) {
            final String newValue = command.stringValueOfParameterNamed(mobileNoParamName);
            actualChanges.put(mobileNoParamName, newValue);
            this.mobileNo = StringUtils.defaultIfEmpty(newValue, null);
        }

        final String isLoanOfficerParamName = "isLoanOfficer";
        if (command.isChangeInBooleanParameterNamed(isLoanOfficerParamName, this.loanOfficer)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(isLoanOfficerParamName);
            actualChanges.put(isLoanOfficerParamName, newValue);
            this.loanOfficer = newValue;
        }

        final String isActiveParamName = "isActive";
        if (command.isChangeInBooleanParameterNamed(isActiveParamName, this.active)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(isActiveParamName);
            actualChanges.put(isActiveParamName, newValue);
            this.active = newValue;
        }

        final String joiningDateParamName = "joiningDate";
        if (command.isChangeInDateParameterNamed(joiningDateParamName, this.joiningDate)) {
            final String valueAsInput = command.stringValueOfParameterNamed(joiningDateParamName);
            actualChanges.put(joiningDateParamName, valueAsInput);
            final LocalDate newValue = command.localDateValueOfParameterNamed(joiningDateParamName);
            this.joiningDate = newValue.toDate();
        }

        return actualChanges;
    }

    public boolean identifiedBy(final Staff staff) {
        return getId().equals(staff.getId());
    }

    private static String deriveDisplayName(String firstname, String lastname) {
        if (!StringUtils.isBlank(firstname)) {
            return lastname + ", " + firstname;
        } else {
            return lastname;
        }
    }
}