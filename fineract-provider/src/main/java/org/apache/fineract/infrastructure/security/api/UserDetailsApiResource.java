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
package org.apache.fineract.infrastructure.security.api;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.constants.TwoFactorConstants;
import org.apache.fineract.infrastructure.security.data.AuthenticatedOauthUserData;
import org.apache.fineract.infrastructure.security.service.SpringSecurityPlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.TwoFactorUtils;
import org.apache.fineract.organisation.staff.domain.StaffEnumerations;
import org.apache.fineract.useradministration.data.RoleData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/*
 * Implementation of Oauth2 authentication APIs, loaded only when "oauth" profile is enabled. 
 */
@Path("userdetails")
@Component
@Profile("oauth")
@Scope("singleton")
@Api(value = "Fetch authenticated user details", description = "")
@RequiredArgsConstructor
public class UserDetailsApiResource {

    private final ResourceServerTokenServices tokenServices;
    private final ToApiJsonSerializer<AuthenticatedOauthUserData> apiJsonSerializerService;
    private final SpringSecurityPlatformSecurityContext springSecurityPlatformSecurityContext;
    private final TwoFactorUtils twoFactorUtils;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Fetch authenticated user details\n", notes = "checks the Authentication and returns the set roles and permissions allowed.")
    @ApiResponses({@ApiResponse(code = 200, message = "", response = UserDetailsApiResourceSwagger.GetUserDetailsResponse.class)})
    public String fetchAuthenticatedUserData(@QueryParam("access_token") @ApiParam(value = "access_token") final String accessToken) {

        final Authentication authentication = this.tokenServices.loadAuthentication(accessToken);
        if (authentication.isAuthenticated()) {
            final AppUser principal = (AppUser) authentication.getPrincipal();

            final Collection<String> permissions = new ArrayList<>();
            AuthenticatedOauthUserData authenticatedUserData;

            final Collection<GrantedAuthority> authorities = new ArrayList<>(authentication.getAuthorities());
            for (final GrantedAuthority grantedAuthority : authorities) {
                permissions.add(grantedAuthority.getAuthority());
            }

            final Collection<RoleData> roles = new ArrayList<>();
            final Set<Role> userRoles = principal.getRoles();
            for (final Role role : userRoles) {
                roles.add(RoleData.builder()
                    .id(role.getId())
                    .name(role.getName())
                    .description(role.getDescription())
                    .disabled(role.getDisabled())
                    .build());
            }

            final Long officeId = principal.getOffice().getId();
            final String officeName = principal.getOffice().getName();

            final Long staffId = principal.getStaff()!=null ? principal.getStaff().getId() : null;
            final String staffDisplayName = principal.getStaff()!=null ? principal.getStaff().getDisplayName() : null;

            final EnumOptionData organisationalRole = principal.getStaff()!=null ? StaffEnumerations.organisationalRole(principal.getStaff().getOrganisationalRoleType()) : null;

            final boolean requireTwoFactorAuth = twoFactorUtils.isTwoFactorAuthEnabled()
                    && !principal.hasSpecificPermissionTo(TwoFactorConstants.BYPASS_TWO_FACTOR_PERMISSION);
            if (this.springSecurityPlatformSecurityContext.doesPasswordHasToBeRenewed(principal)) {
                authenticatedUserData = AuthenticatedOauthUserData.builder()
                    .username(principal.getUsername())
                    .userId(principal.getId())
                    .accessToken(accessToken)
                    .twoFactorAuthenticationRequired(requireTwoFactorAuth)
                    .build();
            } else {
                authenticatedUserData = AuthenticatedOauthUserData.builder()
                    .username(principal.getUsername())
                    .officeId(officeId)
                    .officeName(officeName)
                    .staffId(staffId)
                    .staffDisplayName(staffDisplayName)
                    .organisationalRole(organisationalRole)
                    .roles(roles)
                    .permissions(permissions)
                    .userId(principal.getId())
                    .accessToken(accessToken)
                    .twoFactorAuthenticationRequired(requireTwoFactorAuth)
                    .build();
            }
            return this.apiJsonSerializerService.serialize(authenticatedUserData);
        }
        return null;

    }
}