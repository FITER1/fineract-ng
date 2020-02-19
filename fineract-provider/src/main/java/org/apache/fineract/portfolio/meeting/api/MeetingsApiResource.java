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
package org.apache.fineract.portfolio.meeting.api;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.calendar.data.CalendarData;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.exception.CalendarEntityTypeNotSupportedException;
import org.apache.fineract.portfolio.calendar.service.CalendarReadPlatformService;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.meeting.MeetingApiConstants;
import org.apache.fineract.portfolio.meeting.attendance.data.ClientAttendanceData;
import org.apache.fineract.portfolio.meeting.attendance.service.AttendanceDropdownReadPlatformService;
import org.apache.fineract.portfolio.meeting.attendance.service.ClientAttendanceReadPlatformService;
import org.apache.fineract.portfolio.meeting.data.MeetingData;
import org.apache.fineract.portfolio.meeting.exception.MeetingNotSupportedResourceException;
import org.apache.fineract.portfolio.meeting.service.MeetingReadPlatformService;
import org.joda.time.LocalDate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.fineract.portfolio.meeting.MeetingApiConstants.MEETING_RESOURCE_NAME;

@Path("{entityType}/{entityId}/meetings")
@Component
@Scope("singleton")
@RequiredArgsConstructor
public class MeetingsApiResource {

    private final PlatformSecurityContext context;
    private final MeetingReadPlatformService readPlatformService;
    private final ClientAttendanceReadPlatformService attendanceReadPlatformService;
    private final ClientReadPlatformService clientReadPlatformService;
    private final CalendarReadPlatformService calendarReadPlatformService;
    private final AttendanceDropdownReadPlatformService attendanceDropdownReadPlatformService;
    private final DefaultToApiJsonSerializer<MeetingData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
	private static final Set<String> MEETING_RESPONSE_DATA_PARAMETERS = new HashSet<>(
			Arrays.asList(MeetingApiConstants.idParamName, MeetingApiConstants.meetingDateParamName,
					MeetingApiConstants.clientsAttendance, MeetingApiConstants.clients,
					MeetingApiConstants.calendarData, MeetingApiConstants.attendanceTypeOptions));

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String template(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @QueryParam("calendarId") final Long calendarId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(MEETING_RESOURCE_NAME);
        final Integer entityTypeId = CalendarEntityType.valueOf(entityType.toUpperCase()).getValue();
        Collection<ClientData> clients = null;
        CalendarData calendarData = null;

        if (CalendarEntityType.isGroup(entityType)) {
            clients = this.clientReadPlatformService.retrieveActiveClientMembersOfGroup(entityId);
        } else if (CalendarEntityType.isCenter(entityType)) {
            clients = this.clientReadPlatformService.retrieveActiveClientMembersOfCenter(entityId);
        } else {
            final String defaultUserMessage = "Meeting attendance is not supported for the resource " + entityType
                    + ". The supported resources are [" + CalendarEntityType.GROUPS.name() + ", " + CalendarEntityType.CENTERS.name() + "]";
            throw new MeetingNotSupportedResourceException(defaultUserMessage, CalendarEntityType.GROUPS.name(),
                    CalendarEntityType.CENTERS.name());
        }

        if (calendarId != null) {
            calendarData = this.calendarReadPlatformService.retrieveCalendar(calendarId, entityId, entityTypeId);
            final boolean withHistory = true;
            final Collection<LocalDate> recurringDates = this.calendarReadPlatformService.generateRecurringDates(calendarData, withHistory,
                    DateUtils.getLocalDateOfTenant());
            final Collection<LocalDate> nextTenRecurringDates = this.calendarReadPlatformService
                    .generateNextTenRecurringDates(calendarData);
            final LocalDate recentEligibleMeetingDate = null;
            calendarData = CalendarData.builder()
                .id(calendarData.getId())
                .calendarInstanceId(calendarData.getCalendarInstanceId())
                .entityId(calendarData.getEntityId())
                .entityType(calendarData.getEntityType())
                .title(calendarData.getTitle())
                .description(calendarData.getDescription())
                .location(calendarData.getLocation())
                .startDate(calendarData.getStartDate())
                .endDate(calendarData.getEndDate())
                .duration(calendarData.getDuration())
                .type(calendarData.getType())
                .repeating(calendarData.isRepeating())
                .recurrence(calendarData.getRecurrence())
                .frequency(calendarData.getFrequency())
                .interval(calendarData.getInterval())
                .repeatsOnDay(calendarData.getRepeatsOnDay())
                .repeatsOnNthDayOfMonth(calendarData.getRepeatsOnNthDayOfMonth())
                .remindBy(calendarData.getRemindBy())
                .firstReminder(calendarData.getFirstReminder())
                .secondReminder(calendarData.getSecondReminder())
                .humanReadable(calendarData.getHumanReadable())
                .createdDate(calendarData.getCreatedDate())
                .lastUpdatedDate(calendarData.getLastUpdatedDate())
                .createdByUserId(calendarData.getCreatedByUserId())
                .createdByUsername(calendarData.getCreatedByUsername())
                .lastUpdatedByUserId(calendarData.getLastUpdatedByUserId())
                .lastUpdatedByUsername(calendarData.getLastUpdatedByUsername())
                .repeatsOnDayOfMonth(calendarData.getRepeatsOnDayOfMonth())
                .entityTypeOptions(calendarData.getEntityTypeOptions())
                .calendarTypeOptions(calendarData.getCalendarTypeOptions())
                .remindByOptions(calendarData.getRemindByOptions())
                .frequencyOptions(calendarData.getFrequencyOptions())
                .repeatsOnDayOptions(calendarData.getRepeatsOnDayOptions())
                .meetingTime(calendarData.getMeetingTime())
                .frequencyNthDayTypeOptions(calendarData.getFrequencyNthDayTypeOptions())
                .recurringDates(recurringDates)
                .nextTenRecurringDates(nextTenRecurringDates)
                .recentEligibleMeetingDate(recentEligibleMeetingDate)
                .build();
        }

        final MeetingData meetingData = MeetingData.template(clients, calendarData,
                this.attendanceDropdownReadPlatformService.retrieveAttendanceTypeOptions());

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, meetingData, MEETING_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveMeetings(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @QueryParam("limit") final Integer limit, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(MEETING_RESOURCE_NAME);

        final Collection<MeetingData> meetingsData = this.readPlatformService.retrieveMeetingsByEntity(entityId, CalendarEntityType
                .valueOf(entityType.toUpperCase()).getValue(), limit);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, meetingsData, MEETING_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{meetingId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveMeeting(@PathParam("meetingId") final Long meetingId, @PathParam("entityType") final String entityType,
            @PathParam("entityId") final Long entityId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(MEETING_RESOURCE_NAME);
        final Integer entityTypeId = CalendarEntityType.valueOf(entityType.toUpperCase()).getValue();
        MeetingData meetingData = this.readPlatformService.retrieveMeeting(meetingId, entityId, entityTypeId);
        final Collection<ClientAttendanceData> clientsAttendance = this.attendanceReadPlatformService
                .retrieveClientAttendanceByMeetingId(meetingId);
        meetingData = MeetingData.withClientsAttendanceAndAttendanceTypeOptions(meetingData, clientsAttendance,
                this.attendanceDropdownReadPlatformService.retrieveAttendanceTypeOptions());
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, meetingData, MEETING_RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createMeeting(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            final String apiRequestBodyAsJson) {

        final CalendarEntityType calendarEntityType = CalendarEntityType.getEntityType(entityType);
        if (calendarEntityType == null) { throw new CalendarEntityTypeNotSupportedException(entityType); }

        final CommandWrapper resourceDetails = getResourceDetails(calendarEntityType, entityId);
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createMeeting(resourceDetails, entityType, entityId)
                .withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);

    }

    @POST
    @Path("{meetingId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String performMeetingCommands(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @PathParam("meetingId") final Long meetingId, @QueryParam("command") final String commandParam,
            final String apiRequestBodyAsJson) {

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

        CommandProcessingResult result = null;
        if (is(commandParam, "saveOrUpdateAttendance")) {
            final CommandWrapper commandRequest = builder.saveOrUpdateAttendance(meetingId, entityType, entityId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else {
            throw new UnrecognizedQueryParamException("command", commandParam, new Object[] { "saveOrUpdateAttendance" });
        }

        return this.toApiJsonSerializer.serialize(result);

    }

    @PUT
    @Path("{meetingId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateMeeting(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @PathParam("meetingId") final Long meetingId, final String jsonRequestBody) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateMeeting(entityType, entityId, meetingId)
                .withJson(jsonRequestBody).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{meetingId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteMeeting(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @PathParam("meetingId") final Long meetingId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteMeeting(entityType, entityId, meetingId).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    private CommandWrapper getResourceDetails(final CalendarEntityType type, final Long entityId) {
        CommandWrapperBuilder resourceDetails = new CommandWrapperBuilder();
        switch (type) {
            case CENTERS:
                resourceDetails.withGroupId(entityId);
            break;
            case CLIENTS:
                resourceDetails.withClientId(entityId);
            break;
            case GROUPS:
                resourceDetails.withGroupId(entityId);
            break;
            case LOANS:
                resourceDetails.withLoanId(entityId);
            break;
            case SAVINGS:
                resourceDetails.withSavingsId(entityId);
            break;
            case INVALID:
            break;
            case LOAN_RECALCULATION_REST_DETAIL:
            break;
            default:
            break;
        }
        return resourceDetails.build();
    }

}