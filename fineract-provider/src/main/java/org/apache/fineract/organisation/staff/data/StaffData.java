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
package org.apache.fineract.organisation.staff.data;

import lombok.*;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.joda.time.LocalDate;

import java.util.Collection;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StaffData {
    private Long id;
    private String externalId;
    private String firstname;
    private String lastname;
    private String displayName;
    private String mobileNo;
    private Long officeId;
    private String officeName;
    private Boolean loanOfficer;
    private Boolean active;
    private LocalDate joiningDate;
    //import fields
    private Integer rowIndex;
    private String dateFormat;
    private String locale;
    private Collection<OfficeData> allowedOffices;
}