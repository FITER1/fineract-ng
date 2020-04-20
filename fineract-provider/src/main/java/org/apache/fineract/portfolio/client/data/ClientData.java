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
package org.apache.fineract.portfolio.client.data;

import lombok.*;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.portfolio.address.data.AddressData;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsProductData;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
final public class ClientData implements Serializable {
    private Long id;
    private String accountNo;
    private String externalId;
    private EnumOptionData status;
    private CodeValueData subStatus;
    private Boolean active;
    private LocalDate activationDate;
    private String firstname;
    private String middlename;
    private String lastname;
    private String fullname;
    private String displayName;
    private String mobileNo;
	private String emailAddress;
    private LocalDate dateOfBirth;
    private CodeValueData gender;
    private CodeValueData clientType;
    private CodeValueData clientClassification;
    private Boolean isStaff;
    private Long officeId;
    private String officeName;
    private Long transferToOfficeId;
    private String transferToOfficeName;
    private Long imageId;
    private Boolean imagePresent;
    private Long staffId;
    private String staffName;
    private ClientTimelineData timeline;
    private Long savingsProductId;
    private String savingsProductName;
    private Long savingsAccountId;
    private EnumOptionData legalForm;
    // associations
    private Collection<GroupGeneralData> groups;
    // template
    private Collection<OfficeData> officeOptions;
    private Collection<StaffData> staffOptions;
    private Collection<CodeValueData> narrations;
    private Collection<SavingsProductData> savingProductOptions;
    private Collection<SavingsAccountData> savingAccountOptions;
    private Collection<CodeValueData> genderOptions;
    private Collection<CodeValueData> clientTypeOptions;
    private Collection<CodeValueData> clientClassificationOptions;
    private Collection<CodeValueData> clientNonPersonConstitutionOptions;
    private Collection<CodeValueData> clientNonPersonMainBusinessLineOptions;
    private List<EnumOptionData> clientLegalFormOptions;
    private ClientFamilyMembersData familyMemberOptions;
    private ClientNonPersonData clientNonPersonDetails;
    private Collection<AddressData> address;
	private Boolean isAddressEnabled;
	private List<DatatableData> datatables;
    // import fields
    private transient Integer rowIndex;
    private String dateFormat;
    private String locale;
    private Long clientTypeId;
    private Long genderId;
    private Long clientClassificationId;
    private Long legalFormId;
    private LocalDate submittedOnDate;
}