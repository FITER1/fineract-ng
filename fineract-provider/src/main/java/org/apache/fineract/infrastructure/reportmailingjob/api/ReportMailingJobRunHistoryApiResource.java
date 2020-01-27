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
package org.apache.fineract.infrastructure.reportmailingjob.api;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.reportmailingjob.ReportMailingJobConstants;
import org.apache.fineract.infrastructure.reportmailingjob.data.ReportMailingJobRunHistoryData;
import org.apache.fineract.infrastructure.reportmailingjob.service.ReportMailingJobRunHistoryReadPlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path(ReportMailingJobConstants.REPORT_MAILING_JOB_RUN_HISTORY_RESOURCE_NAME)
@Component
@Scope("singleton")
@Api(value = "List Report Mailing Job History", description = "")
@RequiredArgsConstructor
public class ReportMailingJobRunHistoryApiResource {
    
    private final PlatformSecurityContext platformSecurityContext;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final DefaultToApiJsonSerializer<ReportMailingJobRunHistoryData> reportMailingToApiJsonSerializer;
    private final ReportMailingJobRunHistoryReadPlatformService reportMailingJobRunHistoryReadPlatformService;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "List Report Mailing Job History", notes = "The list capability of report mailing job history can support pagination and sorting.\n" + "\n" + "Example Requests:\n" + "\n" + "reportmailingjobrunhistory/1")
    @ApiResponses({@ApiResponse(code = 200, message = "", response = ReportMailingJobRunHistoryData.class)})
    public String retrieveAllByReportMailingJobId(@Context final UriInfo uriInfo,
                                                  @QueryParam("reportMailingJobId") @ApiParam(value = "reportMailingJobId") final Long reportMailingJobId,
                                                  @QueryParam("offset") @ApiParam(value = "offset") final Integer offset,
                                                  @QueryParam("limit") @ApiParam(value = "limit") final Integer limit,
                                                  @QueryParam("orderBy") @ApiParam(value = "orderBy") final String orderBy,
                                                  @QueryParam("sortOrder") @ApiParam(value = "sortOrder") final String sortOrder) {
        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(ReportMailingJobConstants.REPORT_MAILING_JOB_ENTITY_NAME);
        final SearchParameters searchParameters = SearchParameters.fromReportMailingJobRunHistory(offset, limit, orderBy, sortOrder);
        
        final Page<ReportMailingJobRunHistoryData> reportMailingJobRunHistoryData = this.reportMailingJobRunHistoryReadPlatformService.
                retrieveRunHistoryByJobId(reportMailingJobId, searchParameters);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        
        return this.reportMailingToApiJsonSerializer.serialize(settings, reportMailingJobRunHistoryData);
    }
}
