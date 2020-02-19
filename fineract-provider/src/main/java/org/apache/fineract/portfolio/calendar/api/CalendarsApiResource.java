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
package org.apache.fineract.portfolio.calendar.api;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.calendar.data.CalendarData;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.exception.CalendarEntityTypeNotSupportedException;
import org.apache.fineract.portfolio.calendar.service.CalendarDropdownReadPlatformService;
import org.apache.fineract.portfolio.calendar.service.CalendarReadPlatformService;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.joda.time.LocalDate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.*;

@Path("{entityType}/{entityId}/calendars")
@Component
@Scope("singleton")
@RequiredArgsConstructor
public class CalendarsApiResource {

    /**
     * The set of parameters that are supported in response for {@link Calendar}
     */
    private final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "entityId", "entityType", "title",
            "description", "location", "startDate", "endDate", "duration", "type", "repeating", "recurrence", "frequency", "interval",
            "repeatsOnDay", "remindBy", "firstReminder", "secondReminder", "humanReadable", "createdDate", "lastUpdatedDate",
            "createdByUserId", "createdByUsername", "lastUpdatedByUserId", "lastUpdatedByUsername", "recurringDates",
            "nextTenRecurringDates", "entityTypeOptions", "calendarTypeOptions", "remindByOptions", "frequencyOptions",
            "repeatsOnDayOptions"));
    private final String resourceNameForPermissions = "CALENDAR";

    private final PlatformSecurityContext context;
    private final CalendarReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<CalendarData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final CalendarDropdownReadPlatformService dropdownReadPlatformService;

    @GET
    @Path("{calendarId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveCalendar(@PathParam("calendarId") final Long calendarId, @PathParam("entityType") final String entityType,
            @PathParam("entityId") final Long entityId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);
        final Integer entityTypeId = CalendarEntityType.valueOf(entityType.toUpperCase()).getValue();
        CalendarData calendarData = this.readPlatformService.retrieveCalendar(calendarId, entityId, entityTypeId);

        // Include recurring date details
        final boolean withHistory = true;
        final LocalDate tillDate = null;
        final Collection<LocalDate> recurringDates = this.readPlatformService.generateRecurringDates(calendarData, withHistory, tillDate);
        final Collection<LocalDate> nextTenRecurringDates = this.readPlatformService.generateNextTenRecurringDates(calendarData);
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

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        if (settings.isTemplate()) {
            calendarData = handleTemplate(calendarData);
        }
        return this.toApiJsonSerializer.serialize(settings, calendarData, this.RESPONSE_DATA_PARAMETERS);
    }

    /**
     * @param entityType
     * @param entityId
     * @param uriInfo
     * @param calendarType
     * @return
     */
    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveCalendarsByEntity(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @Context final UriInfo uriInfo, @DefaultValue("all") @QueryParam("calendarType") final String calendarType) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        final Set<String> associationParameters = ApiParameterHelper.extractAssociationsForResponseIfProvided(uriInfo.getQueryParameters());

        Collection<CalendarData> calendarsData = new ArrayList<>();

        final List<Integer> calendarTypeOptions = CalendarUtils.createIntegerListFromQueryParameter(calendarType);

        if (!associationParameters.isEmpty()) {
            if (associationParameters.contains("parentCalendars")) {
                calendarsData.addAll(this.readPlatformService.retrieveParentCalendarsByEntity(entityId,
                        CalendarEntityType.valueOf(entityType.toUpperCase()).getValue(), calendarTypeOptions));
            }
        }

        calendarsData.addAll(this.readPlatformService.retrieveCalendarsByEntity(entityId,
                CalendarEntityType.valueOf(entityType.toUpperCase()).getValue(), calendarTypeOptions));

        // Add recurring dates
        calendarsData = this.readPlatformService.updateWithRecurringDates(calendarsData);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, calendarsData, this.RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveNewCalendarDetails(@Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        CalendarData calendarData = this.readPlatformService.retrieveNewCalendarDetails();
        calendarData = handleTemplate(calendarData);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, calendarData, this.RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createCalendar(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            final String apiRequestBodyAsJson) {

        final CalendarEntityType calendarEntityType = CalendarEntityType.getEntityType(entityType);
        if (calendarEntityType == null) { throw new CalendarEntityTypeNotSupportedException(entityType); }

        final CommandWrapper resourceDetails = getResourceDetails(calendarEntityType, entityId);
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCalendar(resourceDetails, entityType, entityId)
                .withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);

    }

    @PUT
    @Path("{calendarId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateCalendar(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @PathParam("calendarId") final Long calendarId, final String jsonRequestBody) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCalendar(entityType, entityId, calendarId)
                .withJson(jsonRequestBody).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{calendarId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteCalendar(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @PathParam("calendarId") final Long calendarId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteCalendar(entityType, entityId, calendarId).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    private CalendarData handleTemplate(final CalendarData calendarData) {
        final List<EnumOptionData> entityTypeOptions = this.dropdownReadPlatformService.retrieveCalendarEntityTypeOptions();
        final List<EnumOptionData> calendarTypeOptions = this.dropdownReadPlatformService.retrieveCalendarTypeOptions();
        final List<EnumOptionData> remindByOptions = this.dropdownReadPlatformService.retrieveCalendarRemindByOptions();
        final List<EnumOptionData> frequencyOptions = this.dropdownReadPlatformService.retrieveCalendarFrequencyTypeOptions();
        final List<EnumOptionData> repeatsOnDayOptions = this.dropdownReadPlatformService.retrieveCalendarWeekDaysTypeOptions();
        final List<EnumOptionData> frequencyNthDayTypeOptions = this.dropdownReadPlatformService.retrieveCalendarFrequencyNthDayTypeOptions();
        return CalendarData.builder()
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
            .recurringDates(calendarData.getRecurringDates())
            .nextTenRecurringDates(calendarData.getNextTenRecurringDates())
            .humanReadable(calendarData.getHumanReadable())
            .recentEligibleMeetingDate(calendarData.getRecentEligibleMeetingDate())
            .createdDate(calendarData.getCreatedDate())
            .lastUpdatedDate(calendarData.getLastUpdatedDate())
            .createdByUserId(calendarData.getCreatedByUserId())
            .createdByUsername(calendarData.getCreatedByUsername())
            .lastUpdatedByUserId(calendarData.getLastUpdatedByUserId())
            .lastUpdatedByUsername(calendarData.getLastUpdatedByUsername())
            .repeatsOnDayOfMonth(calendarData.getRepeatsOnDayOfMonth())
            .meetingTime(calendarData.getMeetingTime())
            .entityTypeOptions(entityTypeOptions)
            .calendarTypeOptions(calendarTypeOptions)
            .remindByOptions(remindByOptions)
            .frequencyOptions(frequencyOptions)
            .repeatsOnDayOptions(repeatsOnDayOptions)
            .frequencyNthDayTypeOptions(frequencyNthDayTypeOptions)
            .build();
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