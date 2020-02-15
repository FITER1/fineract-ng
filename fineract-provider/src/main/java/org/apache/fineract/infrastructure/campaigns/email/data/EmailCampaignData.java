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
package org.apache.fineract.infrastructure.campaigns.email.data;

import lombok.*;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EmailCampaignData {
    private Long id;
    private String campaignName;
    private Integer campaignType;
    private Long businessRuleId;
    private String paramValue;
    private EnumOptionData campaignStatus;
    private String emailSubject;
    private String emailMessage;
    private String emailAttachmentFileFormat;
    private Long stretchyReportId;
    private String stretchyReportParamMap;
    private DateTime nextTriggerDate;
    private LocalDate lastTriggerDate;
    private EmailCampaignTimeLine emailCampaignTimeLine;
    private DateTime recurrenceStartDate;
    private String recurrence;
}
