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
package org.apache.fineract.useradministration.data;

import lombok.*;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.portfolio.client.data.ClientData;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AppUserData {
    private Long id;
    private String username;
    private Long officeId;
    private String officeName;
    private String firstname;
    private String lastname;
    private String email;
    private Boolean passwordNeverExpires;
    //import fields
    private List<Long> roles;
    private Boolean sendPasswordToEmail;
    private Long staffId;
    private Integer rowIndex;
    private Collection<OfficeData> allowedOffices;
    private Collection<RoleData> availableRoles;
    private Collection<RoleData> selfServiceRoles;
    private Collection<RoleData> selectedRoles;
    private StaffData staff;
    private Boolean selfServiceUser;
    private Set<ClientData> clients;
}