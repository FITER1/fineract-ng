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
package org.apache.fineract.useradministration.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.security.domain.PlatformUser;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@SuperBuilder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_appuser", uniqueConstraints = @UniqueConstraint(columnNames = { "username" }, name = "username_org"))
public class AppUser extends AbstractPersistableCustom<Long> implements PlatformUser {

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "firstname", nullable = false, length = 100)
    private String firstname;

    @Column(name = "lastname", nullable = false, length = 100)
    private String lastname;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nonexpired", nullable = false)
    private boolean accountNonExpired;

    @Column(name = "nonlocked", nullable = false)
    private boolean accountNonLocked;

    @Column(name = "nonexpired_credentials", nullable = false)
    private boolean credentialsNonExpired;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "firsttime_login_remaining", nullable = false)
    private boolean firstTimeLoginRemaining;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "m_appuser_role", joinColumns = @JoinColumn(name = "appuser_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "last_time_password_updated")
    // @Temporal(TemporalType.DATE)
    private Date lastTimePasswordUpdated;

    @Column(name = "password_never_expires", nullable = false)
    private boolean passwordNeverExpires;

    @Column(name = "is_self_service_user", nullable = false)
	private boolean selfServiceUser;
    
    @OneToMany(cascade = CascadeType.ALL,  orphanRemoval = true, fetch=FetchType.EAGER)
    @JoinColumn(name = "appuser_id", referencedColumnName= "id", nullable = false)
    private Set<AppUserClientMapping> appUserClientMappings = new HashSet<>();

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return roles.stream().flatMap(role -> role.getPermissions().stream()).map(p -> new SimpleGrantedAuthority(p.getCode())).collect(Collectors.toList());
    }

    public boolean hasNotPermissionForReport(final String reportName) {

        if (hasNotPermissionForAnyOf("ALL_FUNCTIONS", "ALL_FUNCTIONS_READ", "REPORTING_SUPER_USER", "READ_" + reportName)) { return true; }

        return false;
    }

    public boolean hasNotPermissionForDatatable(final String datatable, final String accessType) {

        final String matchPermission = accessType + "_" + datatable;

        if (accessType.equalsIgnoreCase("READ")) {

            if (hasNotPermissionForAnyOf("ALL_FUNCTIONS", "ALL_FUNCTIONS_READ", matchPermission)) { return true; }

            return false;
        }

        if (hasNotPermissionForAnyOf("ALL_FUNCTIONS", matchPermission)) { return true; }

        return false;
    }

    public boolean hasNotPermissionForAnyOf(final String... permissionCodes) {
        boolean hasNotPermission = true;
        for (final String permissionCode : permissionCodes) {
            final boolean checkPermission = hasPermissionTo(permissionCode);
            if (checkPermission) {
                hasNotPermission = false;
                break;
            }
        }
        return hasNotPermission;
    }

    /**
     * Checks whether the user has a given permission explicitly.
     *
     * @param permissionCode the permission code to check for.
     * @return whether the user has the specified permission
     */
    public boolean hasSpecificPermissionTo(final String permissionCode) {
        boolean hasPermission = false;
        for (final Role role : this.roles) {
            if(role.hasPermissionTo(permissionCode)) {
                hasPermission = true;
                break;
            }
        }
        return hasPermission;
    }

    public void validateHasReadPermission(final String resourceType) {

        final String authorizationMessage = "User has no authority to view " + resourceType.toLowerCase() + "s";
        final String matchPermission = "READ_" + resourceType.toUpperCase();

        if (!hasNotPermissionForAnyOf("ALL_FUNCTIONS", "ALL_FUNCTIONS_READ", matchPermission)) { return; }

        throw new NoAuthorizationException(authorizationMessage);
    }

    private boolean hasPermissionTo(final String permissionCode) {
        boolean hasPermission = hasAllFunctionsPermission();
        if (!hasPermission) {
            for (final Role role : this.roles) {
                if (role.hasPermissionTo(permissionCode)) {
                    hasPermission = true;
                    break;
                }
            }
        }
        return hasPermission;
    }

    private boolean hasAllFunctionsPermission() {
        boolean match = false;
        for (final Role role : this.roles) {
            if (role.hasPermissionTo("ALL_FUNCTIONS")) {
                match = true;
                break;
            }
        }
        return match;
    }

    public void validateHasPermissionTo(final String fn) {
        if (!hasPermissionTo(fn)) {
            final String authorizationMessage = "User has no authority to: " + fn;
            log.error("Unauthorized access: userId: {} action: {} allowed: {}", getId(), fn, getAuthorities());
            throw new NoAuthorizationException(authorizationMessage);
        }
    }

    public void validateHasReadPermission(final String function, final Long userId) {
        if ("USER".equalsIgnoreCase(function) && userId.equals(getId())) {
            // abstain from validation as user allowed fetch their own data no
            // matter what permissions they have.
        } else {
            validateHasReadPermission(function);
        }
    }

    public void validateHasCheckerPermissionTo(final String function) {
        final String checkerPermissionName = function.toUpperCase() + "_CHECKER";
        if (!hasPermissionTo("CHECKER_SUPER_USER") && !hasPermissionTo(checkerPermissionName)) {
            final String authorizationMessage = "User has no authority to be a checker for: " + function;
            throw new NoAuthorizationException(authorizationMessage);
        }
    }

    public void validateHasDatatableReadPermission(final String datatable) {
        if (hasNotPermissionForDatatable(datatable, "READ")) { throw new NoAuthorizationException("Not authorised to read datatable: "
                + datatable); }
    }

    public String getEncodedPassword(final JsonCommand command, final PlatformPasswordEncoder platformPasswordEncoder) {
        final String passwordParamName = "password";
        final String passwordEncodedParamName = "passwordEncoded";
        String passwordEncodedValue = null;

        if (command.hasParameter(passwordParamName)) {
            if (command.isChangeInPasswordParameterNamed(passwordParamName, this.password, platformPasswordEncoder, getId())) {

                passwordEncodedValue = command.passwordValueOfParameterNamed(passwordParamName, platformPasswordEncoder, getId());

            }
        } else if (command.hasParameter(passwordEncodedParamName)) {
            if (command.isChangeInStringParameterNamed(passwordEncodedParamName, this.password)) {

                passwordEncodedValue = command.stringValueOfParameterNamed(passwordEncodedParamName);

            }
        }

        return passwordEncodedValue;
    }
}