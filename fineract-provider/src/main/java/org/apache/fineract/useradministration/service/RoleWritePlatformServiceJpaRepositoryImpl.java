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
package org.apache.fineract.useradministration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.notification.service.TopicDomainService;
import org.apache.fineract.useradministration.command.PermissionsCommand;
import org.apache.fineract.useradministration.domain.Permission;
import org.apache.fineract.useradministration.domain.PermissionRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.apache.fineract.useradministration.exception.PermissionNotFoundException;
import org.apache.fineract.useradministration.exception.RoleAssociatedException;
import org.apache.fineract.useradministration.exception.RoleNotFoundException;
import org.apache.fineract.useradministration.serialization.PermissionsCommandFromApiJsonDeserializer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleWritePlatformServiceJpaRepositoryImpl implements RoleWritePlatformService {

    private final PlatformSecurityContext context;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleDataValidator roleCommandFromApiJsonDeserializer;
    private final PermissionsCommandFromApiJsonDeserializer permissionsFromApiJsonDeserializer;
    private final TopicDomainService topicDomainService;

    @Transactional
    @Override
    public CommandProcessingResult createRole(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.roleCommandFromApiJsonDeserializer.validateForCreate(command.json());

            final Role entity = fromJson(command);
            this.roleRepository.save(entity);
            
            this.topicDomainService.createTopic(entity);

            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(entity.getId()).build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .build();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .build();
        }
    }

    private Role fromJson(final JsonCommand command) {
        final String name = command.stringValueOfParameterNamed("name");
        final String description = command.stringValueOfParameterNamed("description");
        return Role.builder()
            .name(name)
            .description(description)
            .build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("unq_name")) {

            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.role.duplicate.name", "Role with name `" + name + "` already exists",
                    "name", name);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.role.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    @Transactional
    @Override
    public CommandProcessingResult updateRole(final Long roleId, final JsonCommand command) {
        try {
            this.context.authenticatedUser();

            this.roleCommandFromApiJsonDeserializer.validateForUpdate(command.json());

            final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));

            String previousRoleName = role.getName();
            final Map<String, Object> changes = update(role, command);
            if (!changes.isEmpty()) {
                this.roleRepository.saveAndFlush(role);
                if (changes.containsKey("name")) {
                	this.topicDomainService.updateTopic( previousRoleName, role, changes);
                }
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(roleId) //
                    .changes(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .build();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .build();
        }
    }

    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    @Transactional
    @Override
    public CommandProcessingResult updateRolePermissions(final Long roleId, final JsonCommand command) {
        this.context.authenticatedUser();

        final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));

        final Collection<Permission> allPermissions = this.permissionRepository.findAll();

        final PermissionsCommand permissionsCommand = this.permissionsFromApiJsonDeserializer.commandFromApiJson(command.json());

        final Map<String, Boolean> commandPermissions = permissionsCommand.getPermissions();
        final Map<String, Object> changes = new HashMap<>();
        final Map<String, Boolean> changedPermissions = new HashMap<>();
        for (final String permissionCode : commandPermissions.keySet()) {
            final boolean isSelected = commandPermissions.get(permissionCode).booleanValue();

            final Permission permission = findPermissionByCode(allPermissions, permissionCode);
            final boolean changed = isSelected ? role.getPermissions().add(permission) : role.getPermissions().remove(permission);
            if (changed) {
                changedPermissions.put(permissionCode, isSelected);
            }
        }

        if (!changedPermissions.isEmpty()) {
            changes.put("permissions", changedPermissions);
            this.roleRepository.save(role);
        }

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(roleId) //
                .changes(changes) //
                .build();
    }

    private Permission findPermissionByCode(final Collection<Permission> allPermissions, final String permissionCode) {

        if (allPermissions != null) {
            for (final Permission permission : allPermissions) {
                if (permission.getCode().equalsIgnoreCase(permissionCode)) { return permission; }
            }
        }
        throw new PermissionNotFoundException(permissionCode);
    }

    /**
     * Method for Delete Role
     */
    @Transactional
    @Override
    public CommandProcessingResult deleteRole(Long roleId) {

        try {
            /**
             * Checking the role present in DB or not using role_id
             */
            final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));

            /**
             * Roles associated with users can't be deleted
             */
            final Integer count = this.roleRepository.getCountOfRolesAssociatedWithUsers(roleId);
            if (count > 0) { throw new RoleAssociatedException("error.msg.role.associated.with.users.deleted", roleId); }
            
            this.topicDomainService.deleteTopic(role);
            
            this.roleRepository.delete(role);
            return CommandProcessingResult.builder().resourceId(roleId).build();
        } catch (final DataIntegrityViolationException e) {
            throw new PlatformDataIntegrityException("error.msg.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource: " + e.getMostSpecificCause());
        }
    }

    /**
     * Method for disabling the role
     */
    @Transactional
    @Override
    public CommandProcessingResult disableRole(Long roleId) {
        try {
            /**
             * Checking the role present in DB or not using role_id
             */
            final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
            //if(role.isDisabled()){throw new RoleNotFoundException(roleId);}
            
            /**
             * Roles associated with users can't be disable
             */
            final Integer count = this.roleRepository.getCountOfRolesAssociatedWithUsers(roleId);
            if (count > 0) { throw new RoleAssociatedException("error.msg.role.associated.with.users.disabled", roleId); }
            
            /**
             * Disabling the role
             */
            role.setDisabled(true);
            this.roleRepository.save(role);
            return CommandProcessingResult.builder().resourceId(roleId).build();

        } catch (final DataIntegrityViolationException e) {
            throw new PlatformDataIntegrityException("error.msg.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource: " + e.getMostSpecificCause());
        }
    }

    /**
     * Method for Enabling the role
     */
    @Transactional
    @Override
    public CommandProcessingResult enableRole(Long roleId) {
        try {
            /**
             * Checking the role present in DB or not using role_id
             */
            final Role role = this.roleRepository.findById(roleId)
                    .orElseThrow(() -> new RoleNotFoundException(roleId));
            //if(!role.isEnabled()){throw new RoleNotFoundException(roleId);}
            
            role.setDisabled(false);
            this.roleRepository.save(role);
            return CommandProcessingResult.builder().resourceId(roleId).build();

        } catch (final DataIntegrityViolationException e) {
            throw new PlatformDataIntegrityException("error.msg.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource: " + e.getMostSpecificCause());
        }
    }

    private Map<String, Object> update(final Role role, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        final String nameParamName = "name";
        if (command.isChangeInStringParameterNamed(nameParamName, role.getName())) {
            final String newValue = command.stringValueOfParameterNamed(nameParamName);
            actualChanges.put(nameParamName, newValue);
            role.setName(newValue);
        }

        final String descriptionParamName = "description";
        if (command.isChangeInStringParameterNamed(descriptionParamName, role.getDescription())) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            role.setDescription(newValue);
        }

        return actualChanges;
    }
}