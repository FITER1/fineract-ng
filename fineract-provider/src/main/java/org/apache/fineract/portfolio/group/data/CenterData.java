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
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CenterData implements Serializable {
    private Long id;
    private String accountNo;
    private String name;
    private String externalId;
    private Long officeId;
    private String officeName;
    private Long staffId;
    private String staffName;
    private String hierarchy;
    private EnumOptionData status;
    private boolean active;
    private LocalDate activationDate;
    private GroupTimelineData timeline;
    // associations
    private Collection<GroupGeneralData> groupMembers;
    // template
    private Collection<GroupGeneralData> groupMembersOptions;
    private CalendarData collectionMeetingCalendar;
    private Collection<CodeValueData> closureReasons;
    private Collection<OfficeData> officeOptions;
    private Collection<StaffData> staffOptions;
    private BigDecimal totalCollected;
    private BigDecimal totalOverdue;
    private BigDecimal totaldue;
    private BigDecimal installmentDue;
    private List<DatatableData> datatables = null;
    // import fields
    private Integer rowIndex;
    private String dateFormat;
    private String locale;
    private LocalDate submittedOnDate;
}