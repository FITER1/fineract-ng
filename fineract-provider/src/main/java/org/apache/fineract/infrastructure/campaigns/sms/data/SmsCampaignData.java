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
package org.apache.fineract.infrastructure.campaigns.sms.data;

import lombok.*;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.Collection;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SmsCampaignData {
    private Long id;
    private String campaignName;
    private EnumOptionData campaignType;
    private Long runReportId;
    private String reportName;
    private String paramValue;
    private EnumOptionData campaignStatus;
    private EnumOptionData triggerType;
    private String campaignMessage;
    private DateTime nextTriggerDate;
    private LocalDate lastTriggerDate;
    private SmsCampaignTimeLine smsCampaignTimeLine;
    private DateTime recurrenceStartDate;
    private String recurrence;
    private Long providerId;
    private boolean notification;
    private Collection<SmsProviderData> smsProviderOptions;
    private Collection<EnumOptionData> campaignTypeOptions;
    private Collection<EnumOptionData> triggerTypeOptions;
    private Collection<SmsBusinessRulesData> businessRulesOptions;
    private Collection<EnumOptionData> months;
    private Collection<EnumOptionData> weekDays;
    private Collection<EnumOptionData> frequencyTypeOptions;
    private Collection<EnumOptionData> periodFrequencyOptions;
}
