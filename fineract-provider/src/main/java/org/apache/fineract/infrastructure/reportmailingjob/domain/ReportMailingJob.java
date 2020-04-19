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
package org.apache.fineract.infrastructure.reportmailingjob.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.infrastructure.dataqueries.domain.Report;
import org.apache.fineract.infrastructure.reportmailingjob.ReportMailingJobConstants;
import org.apache.fineract.infrastructure.reportmailingjob.data.ReportMailingJobEmailAttachmentFileFormat;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.persistence.*;
import java.util.LinkedHashMap;
import java.util.Map;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_report_mailing_job", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }, name = "unique_name") })
public class ReportMailingJob extends AbstractAuditableCustom<AppUser, Long> {
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "start_datetime", nullable = false)
    // @Temporal(TemporalType.TIMESTAMP)
    private DateTime startDateTime;
    
    @Column(name = "recurrence")
    private String recurrence;
    
    @Column(name = "email_recipients", nullable = false)
    private String emailRecipients;
    
    @Column(name = "email_subject", nullable = false)
    private String emailSubject;
    
    @Column(name = "email_message", nullable = false)
    private String emailMessage;
    
    @Column(name = "email_attachment_file_format", nullable = false)
    private String emailAttachmentFileFormat;
    
    @ManyToOne
    @JoinColumn(name = "stretchy_report_id", nullable = false)
    private Report stretchyReport;
    
    @Column(name = "stretchy_report_param_map")
    private String stretchyReportParamMap;
    
    @Column(name = "previous_run_datetime")
    // @Temporal(TemporalType.TIMESTAMP)
    private DateTime previousRunDateTime;

    @Column(name = "next_run_datetime")
    // @Temporal(TemporalType.TIMESTAMP)
    private DateTime nextRunDateTime;
    
    @Column(name = "previous_run_status")
    private String previousRunStatus;
    
    @Column(name = "previous_run_error_log")
    private String previousRunErrorLog;
    
    @Column(name = "previous_run_error_message")
    private String previousRunErrorMessage;
    
    @Column(name = "number_of_runs", nullable = false)
    @Builder.Default
    private Integer numberOfRuns = 0;
    
    @Column(name = "is_active", nullable = false)
    private boolean active;
    
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "run_as_userid", nullable = false)
    private AppUser runAsUser;

    public static ReportMailingJob newInstance(JsonCommand jsonCommand, final Report stretchyReport, final AppUser runAsUser) {
        final String name = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.NAME_PARAM_NAME);
        final String description = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.DESCRIPTION_PARAM_NAME);
        final String recurrence = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.RECURRENCE_PARAM_NAME);
        final boolean isActive = jsonCommand.booleanPrimitiveValueOfParameterNamed(ReportMailingJobConstants.IS_ACTIVE_PARAM_NAME);
        final String emailRecipients = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.EMAIL_RECIPIENTS_PARAM_NAME);
        final String emailSubject = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.EMAIL_SUBJECT_PARAM_NAME);
        final String emailMessage = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.EMAIL_MESSAGE_PARAM_NAME);
        final String stretchyReportParamMap = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.STRETCHY_REPORT_PARAM_MAP_PARAM_NAME);
        final Integer emailAttachmentFileFormatId = jsonCommand.integerValueOfParameterNamed(ReportMailingJobConstants.EMAIL_ATTACHMENT_FILE_FORMAT_ID_PARAM_NAME);
        final ReportMailingJobEmailAttachmentFileFormat emailAttachmentFileFormat = ReportMailingJobEmailAttachmentFileFormat.newInstance(emailAttachmentFileFormatId);
        LocalDateTime startDateTime = new LocalDateTime();
        
        if (jsonCommand.hasParameter(ReportMailingJobConstants.START_DATE_TIME_PARAM_NAME)) {
            final String startDateTimeString = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.START_DATE_TIME_PARAM_NAME);
            
            if (startDateTimeString != null) {
                final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(jsonCommand.dateFormat()).withLocale(jsonCommand.extractLocale());
                startDateTime = LocalDateTime.parse(startDateTimeString, dateTimeFormatter); 
            }
        }
        
        return ReportMailingJob.builder()
            .name(name)
            .description(description)
            .startDateTime(startDateTime.toDateTime())
            .previousRunDateTime(startDateTime.toDateTime())
            .recurrence(recurrence)
            .emailRecipients(emailRecipients)
            .emailSubject(emailSubject)
            .emailMessage(emailMessage)
            .emailAttachmentFileFormat(emailAttachmentFileFormat.getValue())
            .stretchyReport(stretchyReport)
            .stretchyReportParamMap(stretchyReportParamMap)
            .active(isActive)
            .deleted(false)
            .runAsUser(runAsUser)
            .build();
    }
    
    public Map<String, Object> updateStretchyReport(final JsonCommand jsonCommand) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();
        
        if (jsonCommand.isChangeInStringParameterNamed(ReportMailingJobConstants.NAME_PARAM_NAME, this.name)) {
            final String name = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.NAME_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.NAME_PARAM_NAME, name);
            
            this.name = name;
        }
        
        if (jsonCommand.isChangeInStringParameterNamed(ReportMailingJobConstants.DESCRIPTION_PARAM_NAME, this.description)) {
            final String description = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.DESCRIPTION_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.DESCRIPTION_PARAM_NAME, description);
            
            this.description = description;
        }
        
        if (jsonCommand.isChangeInStringParameterNamed(ReportMailingJobConstants.RECURRENCE_PARAM_NAME, this.recurrence)) {
            final String recurrence = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.RECURRENCE_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.RECURRENCE_PARAM_NAME, recurrence);
            
            this.recurrence = recurrence;
        }
        
        if (jsonCommand.isChangeInBooleanParameterNamed(ReportMailingJobConstants.IS_ACTIVE_PARAM_NAME, this.active)) {
            final boolean isActive = jsonCommand.booleanPrimitiveValueOfParameterNamed(ReportMailingJobConstants.IS_ACTIVE_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.IS_ACTIVE_PARAM_NAME, isActive);
            
            this.active = isActive;
        }
        
        if (jsonCommand.isChangeInStringParameterNamed(ReportMailingJobConstants.EMAIL_RECIPIENTS_PARAM_NAME, this.emailRecipients)) {
            final String emailRecipients = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.EMAIL_RECIPIENTS_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.EMAIL_RECIPIENTS_PARAM_NAME, emailRecipients);
            
            this.emailRecipients = emailRecipients;
        }
        
        if (jsonCommand.isChangeInStringParameterNamed(ReportMailingJobConstants.EMAIL_SUBJECT_PARAM_NAME, this.emailSubject)) {
            final String emailSubject = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.EMAIL_SUBJECT_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.EMAIL_SUBJECT_PARAM_NAME, emailSubject);
            
            this.emailSubject = emailSubject;
        }
        
        if (jsonCommand.isChangeInStringParameterNamed(ReportMailingJobConstants.EMAIL_MESSAGE_PARAM_NAME, this.emailMessage)) {
            final String emailMessage = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.EMAIL_MESSAGE_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.EMAIL_MESSAGE_PARAM_NAME, emailMessage);
            
            this.emailMessage = emailMessage;
        }
        
        if (jsonCommand.isChangeInStringParameterNamed(ReportMailingJobConstants.STRETCHY_REPORT_PARAM_MAP_PARAM_NAME, this.stretchyReportParamMap)) {
            final String stretchyReportParamMap = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.STRETCHY_REPORT_PARAM_MAP_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.STRETCHY_REPORT_PARAM_MAP_PARAM_NAME, stretchyReportParamMap);
            
            this.stretchyReportParamMap = stretchyReportParamMap;
        }
        
        final ReportMailingJobEmailAttachmentFileFormat emailAttachmentFileFormat = ReportMailingJobEmailAttachmentFileFormat.newInstance(this.emailAttachmentFileFormat);
        
        if (jsonCommand.isChangeInIntegerParameterNamed(ReportMailingJobConstants.EMAIL_ATTACHMENT_FILE_FORMAT_ID_PARAM_NAME, emailAttachmentFileFormat.getId())) {
            final Integer emailAttachmentFileFormatId = jsonCommand.integerValueOfParameterNamed(ReportMailingJobConstants.EMAIL_ATTACHMENT_FILE_FORMAT_ID_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.EMAIL_ATTACHMENT_FILE_FORMAT_ID_PARAM_NAME, emailAttachmentFileFormatId);
            
            final ReportMailingJobEmailAttachmentFileFormat newEmailAttachmentFileFormat = ReportMailingJobEmailAttachmentFileFormat.newInstance(emailAttachmentFileFormatId);
            this.emailAttachmentFileFormat = newEmailAttachmentFileFormat.getValue();
        }
        
        final String newStartDateTimeString = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.START_DATE_TIME_PARAM_NAME);
        
        if (!StringUtils.isEmpty(newStartDateTimeString)) {
            final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(jsonCommand.dateFormat()).withLocale(jsonCommand.extractLocale());
            final LocalDateTime newStartDateTime = LocalDateTime.parse(newStartDateTimeString, dateTimeFormatter);
            final LocalDateTime oldStartDateTime = (this.startDateTime != null) ? new LocalDateTime(this.startDateTime) : null;
            
            if ((oldStartDateTime != null) && !newStartDateTime.equals(oldStartDateTime)) {
                actualChanges.put(ReportMailingJobConstants.START_DATE_TIME_PARAM_NAME, newStartDateTimeString);
                
                this.startDateTime = newStartDateTime.toDateTime();
            }
        }
        
        Long currentStretchyReportId = null;
        
        if (this.stretchyReport != null) {
            currentStretchyReportId = this.stretchyReport.getId();
        }
        
        if (jsonCommand.isChangeInLongParameterNamed(ReportMailingJobConstants.STRETCHY_REPORT_ID_PARAM_NAME, currentStretchyReportId)) {
            final Long updatedStretchyReportId = jsonCommand.longValueOfParameterNamed(
                    ReportMailingJobConstants.STRETCHY_REPORT_ID_PARAM_NAME);
            actualChanges.put(ReportMailingJobConstants.STRETCHY_REPORT_ID_PARAM_NAME, updatedStretchyReportId);
        }
        
        return actualChanges;
    }
    
    public void delete() {
        this.deleted = true;
        this.active = false;
        this.name = this.name + "_deleted_" + this.getId();
    }
}
