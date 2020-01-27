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
package org.apache.fineract.infrastructure.campaigns.sms.api;

import com.google.gson.JsonElement;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.campaigns.constants.CampaignType;
import org.apache.fineract.infrastructure.campaigns.sms.constants.SmsCampaignConstants;
import org.apache.fineract.infrastructure.campaigns.sms.data.CampaignPreviewData;
import org.apache.fineract.infrastructure.campaigns.sms.data.SmsCampaignData;
import org.apache.fineract.infrastructure.campaigns.sms.service.SmsCampaignReadPlatformService;
import org.apache.fineract.infrastructure.campaigns.sms.service.SmsCampaignWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.api.JsonQuery;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;

@Path("smscampaigns")
@Component
@Scope("singleton")
@Api(value = "SMS Campaigns")
@RequiredArgsConstructor
public class SmsCampaignApiResource {

    private final PlatformSecurityContext platformSecurityContext;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<SmsCampaignData> toApiJsonSerializer;
    private final SmsCampaignReadPlatformService smsCampaignReadPlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final FromJsonHelper fromJsonHelper;
    private final DefaultToApiJsonSerializer<CampaignPreviewData> previewCampaignMessageDefaultToApiJsonSerializer;
    private final SmsCampaignWritePlatformService smsCampaignWritePlatformService;

    private final String resourceNameForPermissions = "SMS_CAMPAIGN";
    private final PlatformSecurityContext context;

    @GET
    @Path("template")
    @ApiOperation(value = "Retrieve a SMS Campaign", notes = "Example Requests:\n" + "\n" + "smscampaigns/1\n" + "\n" + "\n" + "smscampaigns/1?template=true\n" + "\n" + "\n" + "smscampaigns/template")
    @ApiResponse(code = 200, message = "", response = SmsCampaignData.class)
    public String template(@Context final UriInfo uriInfo) {
        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(SmsCampaignConstants.RESOURCE_NAME);
        final SmsCampaignData smsCampaignData = this.smsCampaignReadPlatformService.retrieveTemplate(CampaignType.SMS.name());
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, smsCampaignData);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Create a SMS Campaign", notes = "Mandatory Fields\n" + "campaignName, campaignType, triggerType, providerId, runReportId, message\n" + "\n" + "Mandatory Fields for Cash based on selected report id\n" + "paramValue in json format")
    @ApiImplicitParams({@ApiImplicitParam(value = "body", required = true, paramType = "body", dataType = "body", format = "body", dataTypeClass =CommandWrapper.class)})
    @ApiResponse(code = 200, message = "", response = CommandProcessingResult.class)
    public String createCampaign(@ApiParam(hidden = true) final String apiRequestBodyAsJson) {
        this.platformSecurityContext.authenticatedUser();
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createSmsCampaign().withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("{resourceId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Retrieve a SMS Campaign", notes = "Example Requests:\n" + "\n" + "smscampaigns/1\n")
    @ApiResponse(code = 200, message = "", response = SmsCampaignData.class)
    public String retrieveCampaign(@PathParam("resourceId") final Long resourceId, @Context final UriInfo uriInfo) {
        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(SmsCampaignConstants.RESOURCE_NAME);
        SmsCampaignData smsCampaignData = this.smsCampaignReadPlatformService.retrieveOne(resourceId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, smsCampaignData);

    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "List SMS Campaigns", notes = "Example Requests:\n" + "\n" + "smscampaigns")
    @ApiResponse(code = 200, message = "", response = SmsCampaignData.class)
    public String retrieveAllEmails(@QueryParam("sqlSearch") final String sqlSearch,
            @QueryParam("offset") final Integer offset, @QueryParam("limit") final Integer limit,
            @QueryParam("orderBy") final String orderBy, @QueryParam("sortOrder") final String sortOrder, @Context final UriInfo uriInfo) {
        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(SmsCampaignConstants.RESOURCE_NAME);
        final SearchParameters searchParameters = SearchParameters.forSMSCampaign(sqlSearch, offset, limit, orderBy, sortOrder);
        Page<SmsCampaignData> smsCampaignDataCollection = this.smsCampaignReadPlatformService.retrieveAll(searchParameters);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, smsCampaignDataCollection);
    }

    @PUT
    @Path("{campaignId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Update a Campaign")
    @ApiImplicitParams({@ApiImplicitParam(value = "body", required = true, paramType = "body", dataType = "body", format = "body", dataTypeClass = CommandWrapper.class)})
    @ApiResponse(code = 200, message = "", response = CommandProcessingResult.class)
    public String updateCampaign(@PathParam("campaignId") final Long campaignId, @ApiParam(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateSmsCampaign(campaignId).withJson(apiRequestBodyAsJson)
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("{campaignId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "SMS Campaign", notes = "Activates | Deactivates | Reactivates")
    @ApiResponse(code = 200, message = "", response = CommandProcessingResult.class)
    public String handleCommands(@PathParam("campaignId") final Long campaignId, @QueryParam("command") final String commandParam,
            @ApiParam(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);
        CommandProcessingResult result = null;
        CommandWrapper commandRequest = null;
        if (is(commandParam, "activate")) {
            commandRequest = builder.activateSmsCampaign(campaignId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "close")) {
            commandRequest = builder.closeSmsCampaign(campaignId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "reactivate")) {
            commandRequest = builder.reactivateSmsCampaign(campaignId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }
        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("preview")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String preview(final String apiRequestBodyAsJson, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        CampaignPreviewData campaignMessage = null;
        final JsonElement parsedQuery = this.fromJsonHelper.parse(apiRequestBodyAsJson);
        final JsonQuery query = JsonQuery.from(apiRequestBodyAsJson, parsedQuery, this.fromJsonHelper);
        campaignMessage = this.smsCampaignWritePlatformService.previewMessage(query);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.previewCampaignMessageDefaultToApiJsonSerializer.serialize(settings, campaignMessage, new HashSet<String>());

    }

    @DELETE
    @Path("{campaignId}")
    @ApiOperation(value = "Delete a SMS Campaign", notes = "Note: Only closed SMS Campaigns can be deleted")
    @ApiResponse(code = 200, message = "", response = CommandProcessingResult.class)
    public String delete(@PathParam("campaignId") final Long campaignId) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteSmsCampaign(campaignId).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
