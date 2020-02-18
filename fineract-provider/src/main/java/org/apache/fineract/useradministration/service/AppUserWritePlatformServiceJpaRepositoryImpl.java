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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.PlatformEmailSendException;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.RandomPasswordGenerator;
import org.apache.fineract.notification.service.TopicDomainService;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.useradministration.api.AppUserApiConstant;
import org.apache.fineract.useradministration.domain.*;
import org.apache.fineract.useradministration.exception.PasswordPreviouslyUsedException;
import org.apache.fineract.useradministration.exception.RoleNotFoundException;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.persistence.PersistenceException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserWritePlatformServiceJpaRepositoryImpl implements AppUserWritePlatformService {

    private final PlatformSecurityContext context;
    private final UserDomainService userDomainService;
    private final PlatformPasswordEncoder platformPasswordEncoder;
    private final AppUserRepository appUserRepository;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final RoleRepository roleRepository;
    private final UserDataValidator fromApiJsonDeserializer;
    private final AppUserPreviousPasswordRepository appUserPreviewPasswordRepository;
    private final StaffRepositoryWrapper staffRepositoryWrapper;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final TopicDomainService topicDomainService;

    @Transactional
    @Override
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult createUser(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final String officeIdParamName = "officeId";
            final Long officeId = command.longValueOfParameterNamed(officeIdParamName);

            final Office userOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);

            final String[] roles = command.arrayValueOfParameterNamed("roles");
            final Set<Role> allRoles = assembleSetOfRoles(roles);

            AppUser appUser;

            final String staffIdParamName = "staffId";
            final Long staffId = command.longValueOfParameterNamed(staffIdParamName);

            Staff linkedStaff = null;
            if (staffId != null) {
                linkedStaff = this.staffRepositoryWrapper.findByOfficeWithNotFoundDetection(staffId, userOffice.getId());
            }
            
            Collection<Client> clients = null;
            if(command.hasParameter(AppUserConstants.IS_SELF_SERVICE_USER)
            		&& command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.IS_SELF_SERVICE_USER)
            		&& command.hasParameter(AppUserConstants.CLIENTS)){
            	JsonArray clientsArray = command.arrayOfParameterNamed(AppUserConstants.CLIENTS);
            	Collection<Long> clientIds = new HashSet<>();
            	for(JsonElement clientElement : clientsArray){
            		clientIds.add(clientElement.getAsLong());
            	}
            	clients = this.clientRepositoryWrapper.findAll(clientIds);
            }

            appUser = fromJson(userOffice, linkedStaff, allRoles, clients, command);

            final Boolean sendPasswordToEmail = command.booleanObjectValueOfParameterNamed("sendPasswordToEmail");
            this.userDomainService.create(appUser, sendPasswordToEmail);
            
            this.topicDomainService.subscribeUserToTopic(appUser);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(appUser.getId()) //
                    .officeId(userOffice.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException | AuthenticationServiceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .build();
        }catch (final PlatformEmailSendException e) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

            final String email = command.stringValueOfParameterNamed("email");
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.user.email.invalid",
                    "The parameter email is invalid.", "email", email);
            dataValidationErrors.add(error);

            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    @Transactional
    @Override
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult updateUser(final Long userId, final JsonCommand command) {

        try {

            this.context.authenticatedUser(new CommandWrapperBuilder().updateUser(null).build());

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            final AppUser userToUpdate = this.appUserRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            final AppUserPreviousPassword currentPasswordToSaveAsPreview = getCurrentPasswordToSaveAsPreview(userToUpdate, command);
            
            Collection<Client> clients = null;
            boolean isSelfServiceUser = userToUpdate.isSelfServiceUser();
            if(command.hasParameter(AppUserConstants.IS_SELF_SERVICE_USER)){
            	isSelfServiceUser = command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.IS_SELF_SERVICE_USER); 
            }
            
            if(isSelfServiceUser
            		&& command.hasParameter(AppUserConstants.CLIENTS)){
            	JsonArray clientsArray = command.arrayOfParameterNamed(AppUserConstants.CLIENTS);
            	Collection<Long> clientIds = new HashSet<>();
            	for(JsonElement clientElement : clientsArray){
            		clientIds.add(clientElement.getAsLong());
            	}
            	clients = this.clientRepositoryWrapper.findAll(clientIds);
            }

            final Map<String, Object> changes = update(userToUpdate, command, this.platformPasswordEncoder, clients);

            this.topicDomainService.updateUserSubscription(userToUpdate, changes);
            if (changes.containsKey("officeId")) {
                final Long officeId = (Long) changes.get("officeId");
                final Office office = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
                userToUpdate.setOffice(office);
            }

            if (changes.containsKey("staffId")) {
                final Long staffId = (Long) changes.get("staffId");
                Staff linkedStaff = null;
                if (staffId != null) {
                    linkedStaff = this.staffRepositoryWrapper.findByOfficeWithNotFoundDetection(staffId, userToUpdate.getOffice().getId());
                }
                userToUpdate.setStaff(linkedStaff);
            }

            if (changes.containsKey("roles")) {
                final String[] roleIds = (String[]) changes.get("roles");
                final Set<Role> allRoles = assembleSetOfRoles(roleIds);

                userToUpdate.setRoles(allRoles);
            }

            if (!changes.isEmpty()) {
                this.appUserRepository.saveAndFlush(userToUpdate);

                if (currentPasswordToSaveAsPreview != null) {
                    this.appUserPreviewPasswordRepository.save(currentPasswordToSaveAsPreview);
                }

            }

            return CommandProcessingResult.builder() //
                    .resourceId(userId) //
                    .officeId(userToUpdate.getOffice().getId()) //
                    .changes(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException | AuthenticationServiceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .build();
        }
    }

    private Map<String, Object> update(final AppUser user, final JsonCommand command, final PlatformPasswordEncoder platformPasswordEncoder,
                                      final Collection<Client> clients) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        // unencoded password provided
        final String passwordParamName = "password";
        final String passwordEncodedParamName = "passwordEncoded";
        if (command.hasParameter(passwordParamName)) {
            if (command.isChangeInPasswordParameterNamed(passwordParamName, user.getPassword(), platformPasswordEncoder, user.getId())) {
                final String passwordEncodedValue = command.passwordValueOfParameterNamed(passwordParamName, platformPasswordEncoder,
                    user.getId());
                actualChanges.put(passwordEncodedParamName, passwordEncodedValue);
                user.toBuilder()
                    .password(passwordEncodedValue)
                    .firstTimeLoginRemaining(false)
                    .lastTimePasswordUpdated(DateUtils.getDateOfTenant())
                    .build();
            }
        }

        if (command.hasParameter(passwordEncodedParamName)) {
            if (command.isChangeInStringParameterNamed(passwordEncodedParamName, user.getPassword())) {
                final String newValue = command.stringValueOfParameterNamed(passwordEncodedParamName);
                actualChanges.put(passwordEncodedParamName, newValue);
                user.toBuilder()
                    .password(newValue)
                    .firstTimeLoginRemaining(false)
                    .lastTimePasswordUpdated(DateUtils.getDateOfTenant())
                    .build();
            }
        }

        final String officeIdParamName = "officeId";
        if (command.isChangeInLongParameterNamed(officeIdParamName, user.getOffice().getId())) {
            final Long newValue = command.longValueOfParameterNamed(officeIdParamName);
            actualChanges.put(officeIdParamName, newValue);
        }

        final String staffIdParamName = "staffId";
        if (command.hasParameter(staffIdParamName)
            && (user.getStaff() == null || command.isChangeInLongParameterNamed(staffIdParamName, user.getStaff().getId()))) {
            final Long newValue = command.longValueOfParameterNamed(staffIdParamName);
            actualChanges.put(staffIdParamName, newValue);
        }

        final String rolesParamName = "roles";

        if (command.isChangeInArrayParameterNamed(rolesParamName, user.getRoles().stream().map(r -> r.getId().toString()).collect(Collectors.toList()).toArray(new String[user.getRoles().size()]))) {
            final String[] newValue = command.arrayValueOfParameterNamed(rolesParamName);
            actualChanges.put(rolesParamName, newValue);
        }

        final String usernameParamName = "username";
        if (command.isChangeInStringParameterNamed(usernameParamName, user.getUsername())) {
            final String newValue = command.stringValueOfParameterNamed(usernameParamName);
            actualChanges.put(usernameParamName, newValue);
            user.setUsername(newValue);
        }

        final String firstnameParamName = "firstname";
        if (command.isChangeInStringParameterNamed(firstnameParamName, user.getFirstname())) {
            final String newValue = command.stringValueOfParameterNamed(firstnameParamName);
            actualChanges.put(firstnameParamName, newValue);
            user.setFirstname(newValue);
        }

        final String lastnameParamName = "lastname";
        if (command.isChangeInStringParameterNamed(lastnameParamName, user.getLastname())) {
            final String newValue = command.stringValueOfParameterNamed(lastnameParamName);
            actualChanges.put(lastnameParamName, newValue);
            user.setLastname(newValue);
        }

        final String emailParamName = "email";
        if (command.isChangeInStringParameterNamed(emailParamName, user.getEmail())) {
            final String newValue = command.stringValueOfParameterNamed(emailParamName);
            actualChanges.put(emailParamName, newValue);
            user.setEmail(newValue);
        }

        final String passwordNeverExpire = "passwordNeverExpires";

        if (command.hasParameter(passwordNeverExpire)) {
            if (command.isChangeInBooleanParameterNamed(passwordNeverExpire, user.isPasswordNeverExpires())) {
                final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(passwordNeverExpire);
                actualChanges.put(passwordNeverExpire, newValue);
                user.setPasswordNeverExpires(newValue);
            }
        }

        if(command.hasParameter(AppUserConstants.IS_SELF_SERVICE_USER)){
            if (command.isChangeInBooleanParameterNamed(AppUserConstants.IS_SELF_SERVICE_USER, user.isSelfServiceUser())){
                final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.IS_SELF_SERVICE_USER);
                actualChanges.put(AppUserConstants.IS_SELF_SERVICE_USER, newValue);
                user.setSelfServiceUser(newValue);
            }
        }

        if(user.isSelfServiceUser() && command.hasParameter(AppUserConstants.CLIENTS)){
            actualChanges.put(AppUserConstants.CLIENTS, command.arrayValueOfParameterNamed(AppUserConstants.CLIENTS));
            if(clients!=null) {
                user.setAppUserClientMappings(clients.stream().map(AppUserClientMapping::new).collect(Collectors.toSet()));
            }
        }else if(!user.isSelfServiceUser() && actualChanges.containsKey(AppUserConstants.IS_SELF_SERVICE_USER)){
            actualChanges.put(AppUserConstants.CLIENTS, new ArrayList<>());
            user.setAppUserClientMappings(Collections.emptySet());
        }

        return actualChanges;
    }

    private AppUser fromJson(final Office userOffice, final Staff linkedStaff, final Set<Role> allRoles, final Collection<Client> clients, final JsonCommand command) {

        final String username = command.stringValueOfParameterNamed("username");
        String password = command.stringValueOfParameterNamed("password");
        final Boolean sendPasswordToEmail = command.booleanObjectValueOfParameterNamed("sendPasswordToEmail");

        if (sendPasswordToEmail.booleanValue()) {
            password = new RandomPasswordGenerator(13).generate();
        }

        boolean passwordNeverExpire = false;

        if (command.parameterExists(AppUserConstants.PASSWORD_NEVER_EXPIRES)) {
            passwordNeverExpire = command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.PASSWORD_NEVER_EXPIRES);
        }

        final boolean userEnabled = true;
        final boolean userAccountNonExpired = true;
        final boolean userCredentialsNonExpired = true;
        final boolean userAccountNonLocked = true;

        final Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("DUMMY_ROLE_NOT_USED_OR_PERSISTED_TO_AVOID_EXCEPTION"));

        final User user = new User(username, password, userEnabled, userAccountNonExpired, userCredentialsNonExpired, userAccountNonLocked,
            authorities);

        final String email = command.stringValueOfParameterNamed("email");
        final String firstname = command.stringValueOfParameterNamed("firstname");
        final String lastname = command.stringValueOfParameterNamed("lastname");

        final boolean isSelfServiceUser = command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.IS_SELF_SERVICE_USER);

        return AppUser.builder()
            .office(userOffice)
            .username(user.getUsername().trim())
            .password(user.getPassword().trim())
            .accountNonExpired(user.isAccountNonExpired())
            .accountNonLocked(user.isAccountNonLocked())
            .credentialsNonExpired(user.isCredentialsNonExpired())
            .enabled(user.isEnabled())
            .roles(allRoles)
            .email(email)
            .firstname(firstname)
            .lastname(lastname)
            .passwordNeverExpires(passwordNeverExpire)
            .selfServiceUser(isSelfServiceUser)
            .appUserClientMappings(clients.stream().map(AppUserClientMapping::new).collect(Collectors.toSet()))
            .build();
    }

    /**
     * encode the new submitted password retrieve the last n used password check
     * if the current submitted password, match with one of them
     * 
     * @param user
     * @param command
     * @return
     */
    private AppUserPreviousPassword getCurrentPasswordToSaveAsPreview(final AppUser user, final JsonCommand command) {

        final String passWordEncodedValue = user.getEncodedPassword(command, this.platformPasswordEncoder);

        AppUserPreviousPassword currentPasswordToSaveAsPreview = null;

        if (passWordEncodedValue != null) {

            PageRequest pageRequest = PageRequest.of(0, AppUserApiConstant.numberOfPreviousPasswords, Sort.Direction.DESC, "removalDate");

            final List<AppUserPreviousPassword> nLastUsedPasswords = this.appUserPreviewPasswordRepository.findByUserId(user.getId(),
                    pageRequest);

            for (AppUserPreviousPassword aPreviewPassword : nLastUsedPasswords) {

                if (aPreviewPassword.getPassword().equals(passWordEncodedValue)) {

                throw new PasswordPreviouslyUsedException();

                }
            }

            currentPasswordToSaveAsPreview = AppUserPreviousPassword.builder()
                .userId(user.getId())
                .password(user.getPassword().trim())
                .removalDate(DateUtils.getDateOfTenant())
                .build();
        }

        return currentPasswordToSaveAsPreview;

    }

    private Set<Role> assembleSetOfRoles(final String[] rolesArray) {

        final Set<Role> allRoles = new HashSet<>();

        if (!ObjectUtils.isEmpty(rolesArray)) {
            for (final String roleId : rolesArray) {
                final Long id = Long.valueOf(roleId);
                final Role role = this.roleRepository.findById(id)
                        .orElseThrow(() -> new RoleNotFoundException(id));
                allRoles.add(role);
            }
        }

        return allRoles;
    }

    @Transactional
    @Override
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult deleteUser(final Long userId) {

        AppUser user = this.appUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.isDeleted()) { throw new UserNotFoundException(userId); }

        user = user.toBuilder()
            .deleted(true)
            .enabled(false)
            .accountNonExpired(false)
            .firstTimeLoginRemaining(true)
            .username(user.getId() + "_DELETED_" + user.getUsername())
            .roles(Collections.emptySet())
            .build();
        this.topicDomainService.unsubcribeUserFromTopic(user);
        this.appUserRepository.save(user);

        return CommandProcessingResult.builder().resourceId(userId).officeId(user.getOffice().getId()).build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("'username_org'")) {
            final String username = command.stringValueOfParameterNamed("username");
            final StringBuilder defaultMessageBuilder = new StringBuilder("User with username ").append(username)
                    .append(" already exists.");
            throw new PlatformDataIntegrityException("error.msg.user.duplicate.username", defaultMessageBuilder.toString(), "username",
                    username);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.unknown.data.integrity.issue", "Unknown data integrity issue with resource.");
    }
}