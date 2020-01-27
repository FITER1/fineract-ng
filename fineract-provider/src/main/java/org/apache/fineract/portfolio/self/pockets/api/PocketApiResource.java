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

package org.apache.fineract.portfolio.self.pockets.api;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.portfolio.self.pockets.service.PocketAccountMappingReadPlatformService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path("self/pockets")
@Component
@Scope("singleton")
@RequiredArgsConstructor
public class PocketApiResource {
	private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
	@SuppressWarnings("rawtypes")
	private final DefaultToApiJsonSerializer toApiJsonSerializer;
	private final PocketAccountMappingReadPlatformService pocketAccountMappingReadPlatformService;

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String handleCommands(@QueryParam("command") final String commandParam, @Context final UriInfo uriInfo,
			final String apiRequestBodyAsJson) {

		final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

		CommandProcessingResult result = null;

		if (is(commandParam, PocketApiConstants.linkAccountsToPocketCommandParam)) {
			final CommandWrapper commandRequest = builder.linkAccountsToPocket().build();
			result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
		} else if (is(commandParam, PocketApiConstants.delinkAccountsFromPocketCommandParam)) {
			final CommandWrapper commandRequest = builder.delinkAccountsFromPocket().build();
			result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
		}

		if (result == null) {
			throw new UnrecognizedQueryParamException("command", commandParam);
		}

		return this.toApiJsonSerializer.serialize(result);
	}

	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveAll() {
		return this.toApiJsonSerializer.serialize(this.pocketAccountMappingReadPlatformService.retrieveAll());
	}

	private boolean is(final String commandParam, final String commandValue) {
		return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
	}

}
