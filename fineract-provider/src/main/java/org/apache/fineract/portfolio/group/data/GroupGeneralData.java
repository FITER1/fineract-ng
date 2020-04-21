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
package org.apache.fineract.portfolio.group.data;

import lombok.*;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.portfolio.calendar.data.CalendarData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GroupGeneralData implements Serializable {
    private Long id;
    private String accountNo;
    private String name;
    private String externalId;
    private EnumOptionData status;
    private Boolean active;
    private LocalDate activationDate;
    private Long officeId;
    private String officeName;
    private Long centerId;
    private String centerName;
    private Long staffId;
    private String staffName;
    private String hierarchy;
    private String groupLevel;
    // associations
    private Collection<ClientData> clientMembers;
    private Collection<ClientData> activeClientMembers;
    private Collection<GroupRoleData> groupRoles;
    private Collection<CalendarData> calendarsData;
    private CalendarData collectionMeetingCalendar;
    // template
    private Collection<CenterData> centerOptions;
    private Collection<OfficeData> officeOptions;
    private Collection<StaffData> staffOptions;
    private Collection<ClientData> clientOptions;
    private Collection<CodeValueData> availableRoles;
    private GroupRoleData selectedRole;
    private Collection<CodeValueData> closureReasons;
    private GroupTimelineData timeline;
    private List<DatatableData> datatables = null;
    // import fields
    private Integer rowIndex;
    private String dateFormat;
    private String locale;
    private LocalDate submittedOnDate;
}