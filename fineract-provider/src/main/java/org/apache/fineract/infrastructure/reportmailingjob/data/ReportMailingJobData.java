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
package org.apache.fineract.infrastructure.reportmailingjob.data;

import lombok.*;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.dataqueries.data.ReportData;
import org.joda.time.DateTime;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReportMailingJobData {
    private Long id;
    private String name;
    private String description;
    private DateTime startDateTime;
    private String recurrence;
    private ReportMailingJobTimelineData timeline;
    private String emailRecipients;
    private String emailSubject;
    private String emailMessage;
    private EnumOptionData emailAttachmentFileFormat;
    private ReportData stretchyReport;
    private String stretchyReportParamMap;
    private DateTime previousRunDateTime;
    private DateTime nextRunDateTime;
    private String previousRunStatus;
    private String previousRunErrorLog;
    private String previousRunErrorMessage;
    private Integer numberOfRuns;
    private boolean active;
    private List<EnumOptionData> emailAttachmentFileFormatOptions;
    private List<EnumOptionData> stretchyReportParamDateOptions;
    private Long runAsUserId;
}
