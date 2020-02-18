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
import org.apache.commons.codec.binary.Base64;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.constants.TwoFactorConstants;
import org.apache.fineract.infrastructure.security.data.AuthenticatedUserData;
import org.apache.fineract.infrastructure.security.service.SpringSecurityPlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.TwoFactorUtils;
import org.apache.fineract.organisation.staff.domain.StaffEnumerations;
import org.apache.fineract.useradministration.data.RoleData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

@Path("authentication")
@Component
@Profile("basicauth")
@Scope("singleton")
@Api(value = "Authentication HTTP Basic", description = "An API capability that allows client applications to verify authentication details using HTTP Basic Authentication.")
@RequiredArgsConstructor
public class AuthenticationApiResource {

    private final DaoAuthenticationProvider customAuthenticationProvider;
    private final ToApiJsonSerializer<AuthenticatedUserData> apiJsonSerializerService;
    private final SpringSecurityPlatformSecurityContext springSecurityPlatformSecurityContext;
    private final TwoFactorUtils twoFactorUtils;

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Verify authentication", notes = "Authenticates the credentials provided and returns the set roles and permissions allowed.")
    @ApiResponses({@ApiResponse(code = 200, message = "", response = AuthenticationApiResourceSwagger.PostAuthenticationResponse.class), @ApiResponse(code = 400, message = "Unauthenticated. Please login")})
    public String authenticate(@QueryParam("username") @ApiParam(value = "username") final String username, @QueryParam("password") @ApiParam(value = "password") final String password) {

        final Authentication authentication = new UsernamePasswordAuthenticationToken(username, password);
        final Authentication authenticationCheck = this.customAuthenticationProvider.authenticate(authentication);

        final Collection<String> permissions = new ArrayList<>();
        AuthenticatedUserData authenticatedUserData = AuthenticatedUserData.builder()
            .username(username)
            .permissions(permissions)
            .build();

        if (authenticationCheck.isAuthenticated()) {
            final Collection<GrantedAuthority> authorities = new ArrayList<>(authenticationCheck.getAuthorities());
            for (final GrantedAuthority grantedAuthority : authorities) {
                permissions.add(grantedAuthority.getAuthority());
            }

            byte[] base64EncodedAuthenticationKey = null;

            try {
                base64EncodedAuthenticationKey = new Base64().encode((username + ":" + password).getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }

            final AppUser principal = (AppUser) authenticationCheck.getPrincipal();
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

            boolean isTwoFactorRequired = twoFactorUtils.isTwoFactorAuthEnabled() && !
                    principal.hasSpecificPermissionTo(TwoFactorConstants.BYPASS_TWO_FACTOR_PERMISSION);
            if (this.springSecurityPlatformSecurityContext.doesPasswordHasToBeRenewed(principal)) {
                authenticatedUserData = AuthenticatedUserData.builder()
                    .username(username)
                    .userId(principal.getId())
                    .base64EncodedAuthenticationKey(new String(base64EncodedAuthenticationKey))
                    .twoFactorAuthenticationRequired(isTwoFactorRequired)
                    .build();
            } else {

                authenticatedUserData = AuthenticatedUserData.builder()
                    .username(username)
                    .officeId(officeId)
                    .officeName(officeName)
                    .staffId(staffId)
                    .staffDisplayName(staffDisplayName)
                    .organisationalRole(organisationalRole)
                    .roles(roles)
                    .permissions(permissions)
                    .userId(principal.getId())
                    .base64EncodedAuthenticationKey(new String(base64EncodedAuthenticationKey))
                    .twoFactorAuthenticationRequired(isTwoFactorRequired)
                    .build();
            }

        }

        return this.apiJsonSerializerService.serialize(authenticatedUserData);
    }
}