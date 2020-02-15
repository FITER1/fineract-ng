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
package org.apache.fineract.infrastructure.campaigns.email.domain;

import lombok.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.campaigns.email.ScheduledEmailConstants;
import org.apache.fineract.infrastructure.campaigns.email.data.EmailCampaignValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.dataqueries.domain.Report;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.persistence.*;
import java.util.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "scheduled_email_campaign")
public class EmailCampaign extends AbstractPersistableCustom<Long> {

    @Column(name = "campaign_name", nullable = false)
    private String campaignName;

    @Column(name = "campaign_type", nullable = false)
    private Integer campaignType;

    @ManyToOne
    @JoinColumn(name = "businessRule_id", nullable = false)
    private Report businessRuleId ;

    @Column(name = "param_value")
    private String paramValue;

    @Column(name = "status_enum", nullable = false)
    private Integer status;

    @Column(name = "email_subject", nullable = false)
    private String emailSubject;

    @Column(name = "email_message", nullable = false)
    private String emailMessage;

    @Column(name = "email_attachment_file_format", nullable = false)
    private String emailAttachmentFileFormat;

    @ManyToOne
    @JoinColumn(name = "stretchy_report_id", nullable = false)
    private Report stretchyReport;

    @Column(name = "stretchy_report_param_map", nullable = true)
    private String stretchyReportParamMap;

    @Column(name = "closedon_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date closureDate;

    @ManyToOne(optional = true)
    @JoinColumn(name = "closedon_userid", nullable = true)
    private AppUser closedBy;

    @Column(name = "submittedon_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date submittedOnDate;

    @ManyToOne(optional = true)
    @JoinColumn(name = "submittedon_userid", nullable = true)
    private AppUser submittedBy;

    @Column(name = "approvedon_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date approvedOnDate;

    @ManyToOne(optional = true)
    @JoinColumn(name = "approvedon_userid", nullable = true)
    private AppUser approvedBy;

    @Column(name = "recurrence", nullable = false)
    private String recurrence;

    @Column(name = "next_trigger_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date nextTriggerDate;

    @Column(name = "last_trigger_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastTriggerDate;

    @Column(name = "recurrence_start_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date recurrenceStartDate;

    @Column(name="is_visible",nullable = true)
    private boolean isVisible;

    @Column(name = "previous_run_status", nullable = true)
    private String previousRunStatus;

    @Column(name = "previous_run_error_log", nullable = true)
    private String previousRunErrorLog;

    @Column(name = "previous_run_error_message", nullable = true)
    private String previousRunErrorMessage;

    public static EmailCampaign instance(final AppUser submittedBy, final Report businessRuleId, final Report stretchyReport, final JsonCommand command){

        final Integer emailAttachmentFileFormatId = command.integerValueOfParameterNamed(ScheduledEmailConstants.EMAIL_ATTACHMENT_FILE_FORMAT_ID_PARAM_NAME);
        final Long  campaignType = command.longValueOfParameterNamed(EmailCampaignValidator.campaignType);

        LocalDate submittedOnDate = new LocalDate();
        if (command.hasParameter(EmailCampaignValidator.submittedOnDateParamName)) {
            submittedOnDate = command.localDateValueOfParameterNamed(EmailCampaignValidator.submittedOnDateParamName);
        }

        final  DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(command.extractLocale());
        LocalDateTime recurrenceStartDate = new  LocalDateTime();
        if(EmailCampaignType.fromInt(campaignType.intValue()).isSchedule()) {
            if(command.hasParameter(EmailCampaignValidator.recurrenceStartDate)){
                recurrenceStartDate = LocalDateTime.parse(command.stringValueOfParameterNamed(EmailCampaignValidator.recurrenceStartDate), fmt);
            }
        } else {
            recurrenceStartDate = null;
        }

        return EmailCampaign.builder()
            .campaignName(command.stringValueOfParameterNamed(EmailCampaignValidator.campaignName))
            .campaignType(command.integerValueOfParameterNamed(EmailCampaignValidator.campaignType))
            .paramValue(command.stringValueOfParameterNamed(EmailCampaignValidator.paramValue))
            .emailSubject(command.stringValueOfParameterNamed(EmailCampaignValidator.emailSubject))
            .emailMessage(command.stringValueOfParameterNamed(EmailCampaignValidator.emailMessage))
            .stretchyReportParamMap(command.stringValueOfParameterNamed(ScheduledEmailConstants.STRETCHY_REPORT_PARAM_MAP_PARAM_NAME))
            .emailAttachmentFileFormat((
                emailAttachmentFileFormatId!=null ?
                    ScheduledEmailAttachmentFileFormat.instance(command.integerValueOfParameterNamed(ScheduledEmailConstants.EMAIL_ATTACHMENT_FILE_FORMAT_ID_PARAM_NAME)) :
                    ScheduledEmailAttachmentFileFormat.instance(2)).getValue())
            .recurrence(command.stringValueOfParameterNamed(EmailCampaignValidator.recurrenceParamName))
            .recurrenceStartDate(recurrenceStartDate==null ? null : recurrenceStartDate.toDate())
            .submittedOnDate(submittedOnDate.toDate())
            .submittedBy(submittedBy)
            .stretchyReport(stretchyReport)
            .businessRuleId(businessRuleId)
            .build();
    }

    public Map<String,Object> update(JsonCommand command){

        final Map<String, Object> actualChanges = new LinkedHashMap<>(5);

        if (command.isChangeInStringParameterNamed(EmailCampaignValidator.campaignName, this.campaignName)) {
            final String newValue = command.stringValueOfParameterNamed(EmailCampaignValidator.campaignName);
            actualChanges.put(EmailCampaignValidator.campaignName, newValue);
            this.campaignName = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(EmailCampaignValidator.emailMessage, this.emailMessage)) {
            final String newValue = command.stringValueOfParameterNamed(EmailCampaignValidator.emailMessage);
            actualChanges.put(EmailCampaignValidator.emailMessage, newValue);
            this.emailMessage = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInStringParameterNamed(EmailCampaignValidator.paramValue, this.paramValue)) {
            final String newValue = command.stringValueOfParameterNamed(EmailCampaignValidator.paramValue);
            actualChanges.put(EmailCampaignValidator.paramValue, newValue);
            this.paramValue = StringUtils.defaultIfEmpty(newValue, null);
        }
        if (command.isChangeInIntegerParameterNamed(EmailCampaignValidator.campaignType, this.campaignType)) {
            final Integer newValue = command.integerValueOfParameterNamed(EmailCampaignValidator.campaignType);
            actualChanges.put(EmailCampaignValidator.campaignType, EmailCampaignType.fromInt(newValue));
            this.campaignType = EmailCampaignType.fromInt(newValue).getValue();
        }
        if (command.isChangeInLongParameterNamed(EmailCampaignValidator.businessRuleId, (this.businessRuleId != null) ? this.businessRuleId.getId() : null)) {
            final String newValue = command.stringValueOfParameterNamed(EmailCampaignValidator.businessRuleId);
            actualChanges.put(EmailCampaignValidator.businessRuleId, newValue);
        }
        if (command.isChangeInStringParameterNamed(EmailCampaignValidator.recurrenceParamName, this.recurrence)) {
            final String newValue = command.stringValueOfParameterNamed(EmailCampaignValidator.recurrenceParamName);
            actualChanges.put(EmailCampaignValidator.recurrenceParamName, newValue);
            this.recurrence = StringUtils.defaultIfEmpty(newValue, null);
        }
        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();
        final Locale locale = command.extractLocale();
        final  DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);

        if (command.isChangeInLocalDateParameterNamed(EmailCampaignValidator.recurrenceStartDate, getRecurrenceStartDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(EmailCampaignValidator.recurrenceStartDate);
            actualChanges.put(EmailCampaignValidator.recurrenceStartDate, valueAsInput);
            actualChanges.put(ClientApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(ClientApiConstants.localeParamName, localeAsInput);

            final LocalDateTime newValue =  LocalDateTime.parse(valueAsInput, fmt);

            this.recurrenceStartDate = newValue.toDate();
        }

        return actualChanges;
    }

    public void activate(final AppUser currentUser, final DateTimeFormatter formatter, final LocalDate activationLocalDate){

        if(isActive()){
            //handle errors if already activated
            final String defaultUserMessage = "Cannot activate campaign. Campaign is already active.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.already.active", defaultUserMessage,
                    EmailCampaignValidator.activationDateParamName, activationLocalDate.toString(formatter));

            final List<ApiParameterError> dataValidationErrors = new ArrayList<ApiParameterError>();
            dataValidationErrors.add(error);

            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        this.approvedOnDate = activationLocalDate.toDate();
        this.approvedBy = currentUser;
        this.status = EmailCampaignStatus.ACTIVE.getValue();

       validate();
    }

    public void close(final AppUser currentUser,final DateTimeFormatter dateTimeFormatter, final LocalDate closureLocalDate){
        if(isClosed()){
            //handle errors if already activated
            final String defaultUserMessage = "Cannot close campaign. Campaign already in closed state.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.already.closed", defaultUserMessage,
                    EmailCampaignValidator.statusParamName, EmailCampaignStatus.fromInt(this.status).getCode());

            final List<ApiParameterError> dataValidationErrors = new ArrayList<ApiParameterError>();
            dataValidationErrors.add(error);

            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        if(this.campaignType.intValue() == EmailCampaignType.SCHEDULE.getValue()){
            this.nextTriggerDate = null;
            this.lastTriggerDate = null;
        }
        this.closedBy = currentUser;
        this.closureDate = closureLocalDate.toDate();
        this.status     = EmailCampaignStatus.CLOSED.getValue();
        validateClosureDate();
    }

    public void reactivate(final AppUser currentUser, final DateTimeFormatter dateTimeFormat,final LocalDate reactivateLocalDate){

        if(!isClosed()){
            //handle errors if already activated
            final String defaultUserMessage = "Cannot reactivate campaign. Campaign must be in closed state.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.must.be.closed", defaultUserMessage,
                    EmailCampaignValidator.statusParamName, EmailCampaignStatus.fromInt(this.status).getCode());

            final List<ApiParameterError> dataValidationErrors = new ArrayList<ApiParameterError>();
            dataValidationErrors.add(error);

            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

        this.approvedOnDate = reactivateLocalDate.toDate();
        this.status  = EmailCampaignStatus.ACTIVE.getValue();
        this.approvedBy =currentUser;
        this.closureDate = null;
        this.isVisible = true;
        this.closedBy = null;

        validateReactivate();
    }

    public void delete(){
        if(!isClosed()){
            //handle errors if already activated
            final String defaultUserMessage = "Cannot delete campaign. Campaign must be in closed state.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.must.be.closed", defaultUserMessage,
                    EmailCampaignValidator.statusParamName, EmailCampaignStatus.fromInt(this.status).getCode());

            final List<ApiParameterError> dataValidationErrors = new ArrayList<ApiParameterError>();
            dataValidationErrors.add(error);

            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        this.isVisible = false;
    }

    public boolean isActive(){
        return EmailCampaignStatus.fromInt(this.status).isActive();
    }

    public boolean isPending(){
        return  EmailCampaignStatus.fromInt(this.status).isPending();
    }

    public boolean isClosed(){
        return EmailCampaignStatus.fromInt(this.status).isClosed();
    }

    public boolean isDirect(){
        return EmailCampaignType.fromInt(this.campaignType).isDirect();
    }

    public boolean isSchedule(){
        return EmailCampaignType.fromInt(this.campaignType).isSchedule();
    }

    private void validate() {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<ApiParameterError>();
        validateActivationDate(dataValidationErrors);
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }

    private void validateReactivate(){
        final List<ApiParameterError> dataValidationErrors = new ArrayList<ApiParameterError>();
        validateReactivationDate(dataValidationErrors);
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }

    private void validateClosureDate(){
        final List<ApiParameterError> dataValidationErrors = new ArrayList<ApiParameterError>();
        validateClosureDate(dataValidationErrors);
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }

    private void validateActivationDate(final List<ApiParameterError> dataValidationErrors) {

        if (getSubmittedOnDate() != null && isDateInTheFuture(getSubmittedOnDate())) {

            final String defaultUserMessage = "submitted date cannot be in the future.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.submittedOnDate.in.the.future",
                    defaultUserMessage, EmailCampaignValidator.submittedOnDateParamName, this.submittedOnDate);

            dataValidationErrors.add(error);
        }

        if (getActivationLocalDate() != null && getSubmittedOnDate() != null && getSubmittedOnDate().isAfter(getActivationLocalDate())) {

            final String defaultUserMessage = "submitted date cannot be after the activation date";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.submittedOnDate.after.activation.date",
                    defaultUserMessage, EmailCampaignValidator.submittedOnDateParamName, this.submittedOnDate);

            dataValidationErrors.add(error);
        }

        if (getActivationLocalDate() != null && isDateInTheFuture(getActivationLocalDate())) {

            final String defaultUserMessage = "Activation date cannot be in the future.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.activationDate.in.the.future",
                    defaultUserMessage, EmailCampaignValidator.activationDateParamName, getActivationLocalDate());

            dataValidationErrors.add(error);
        }

    }

    public LocalDate getSubmittedOnDate() {
        return (LocalDate) ObjectUtils.defaultIfNull(new LocalDate(this.submittedOnDate), null);

    }

    public LocalDate getClosureDate() {
        return (LocalDate) ObjectUtils.defaultIfNull(new LocalDate(this.closureDate), null);
    }

    public LocalDate getActivationLocalDate() {
        LocalDate activationLocalDate = null;
        if (this.approvedOnDate != null) {
            activationLocalDate = LocalDate.fromDateFields(this.approvedOnDate);
        }
        return activationLocalDate;
    }

    public LocalDate getRecurrenceStartDate() {
        return ObjectUtils.defaultIfNull(new LocalDate(this.recurrenceStartDate), null);
    }
    public LocalDateTime getRecurrenceStartDateTime() {
        return ObjectUtils.defaultIfNull(new LocalDateTime(this.recurrenceStartDate), null);
    }

    public LocalDateTime getNextTriggerDate() {
        return ObjectUtils.defaultIfNull(new LocalDateTime(this.nextTriggerDate), null);

    }

    private void validateReactivationDate(final List<ApiParameterError> dataValidationErrors) {
        if (getActivationLocalDate() != null && isDateInTheFuture(getActivationLocalDate())) {

            final String defaultUserMessage = "Activation date cannot be in the future.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.activationDate.in.the.future",
                defaultUserMessage, EmailCampaignValidator.activationDateParamName, getActivationLocalDate());

            dataValidationErrors.add(error);
        }
        if (getActivationLocalDate() != null && getSubmittedOnDate() != null && getSubmittedOnDate().isAfter(getActivationLocalDate())) {

            final String defaultUserMessage = "submitted date cannot be after the activation date";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.submittedOnDate.after.activation.date",
                defaultUserMessage, EmailCampaignValidator.submittedOnDateParamName, this.submittedOnDate);

            dataValidationErrors.add(error);
        }
        if (getSubmittedOnDate() != null && isDateInTheFuture(getSubmittedOnDate())) {

            final String defaultUserMessage = "submitted date cannot be in the future.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.submittedOnDate.in.the.future",
                defaultUserMessage, EmailCampaignValidator.submittedOnDateParamName, this.submittedOnDate);

            dataValidationErrors.add(error);
        }

    }

    private void validateClosureDate(final List<ApiParameterError> dataValidationErrors){
        if (getClosureDate() != null && isDateInTheFuture(getClosureDate())) {
            final String defaultUserMessage = "closure date cannot be in the future.";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.campaign.closureDate.in.the.future",
                defaultUserMessage, EmailCampaignValidator.closureDateParamName, this.closureDate);

            dataValidationErrors.add(error);
        }
    }

    private boolean isDateInTheFuture(final LocalDate localDate) {
        return localDate.isAfter(DateUtils.getLocalDateOfTenant());
    }
}
