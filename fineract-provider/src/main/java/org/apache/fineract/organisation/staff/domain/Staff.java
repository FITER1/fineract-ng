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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.documentmanagement.domain.Image;
import org.apache.fineract.organisation.office.domain.Office;

import javax.persistence.*;
import java.util.Date;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_staff", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "display_name" }, name = "display_name"),
    @UniqueConstraint(columnNames = { "external_id" }, name = "external_id_UNIQUE"),
    @UniqueConstraint(columnNames = { "mobile_no" }, name = "mobile_no_UNIQUE")
})
public class Staff extends AbstractPersistableCustom<Long> {

    @Column(name = "firstname", length = 50)
    private String firstname;

    @Column(name = "lastname", length = 50)
    private String lastname;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "mobile_no", length = 50, nullable = false, unique = true)
    private String mobileNo;

    @Column(name = "external_id", length = 100, unique = true)
    private String externalId;

	@Column(name = "email_address", length = 50, unique = true)
    private String emailAddress;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Column(name = "is_loan_officer", nullable = false)
    private boolean loanOfficer;

    @Column(name = "organisational_role_enum")
    private Integer organisationalRoleType;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "joining_date")
    // @Temporal(TemporalType.DATE)
    private Date joiningDate;

    @ManyToOne
    @JoinColumn(name = "organisational_role_parent_staff_id")
    private Staff organisationalRoleParentStaff;

    @OneToOne(optional = true)
    @JoinColumn(name = "image_id")
    private Image image;

    public boolean identifiedBy(final Staff staff) {
        return getId().equals(staff.getId());
    }
}