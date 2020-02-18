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
package org.apache.fineract.organisation.office.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.notification.service.TopicDomainService;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.office.domain.OfficeTransaction;
import org.apache.fineract.organisation.office.domain.OfficeTransactionRepository;
import org.apache.fineract.organisation.office.exception.CannotUpdateOfficeWithParentOfficeSameAsSelf;
import org.apache.fineract.organisation.office.exception.RootOfficeParentCannotBeUpdated;
import org.apache.fineract.organisation.office.serialization.OfficeCommandFromApiJsonDeserializer;
import org.apache.fineract.organisation.office.serialization.OfficeTransactionCommandFromApiJsonDeserializer;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficeWritePlatformServiceJpaRepositoryImpl implements OfficeWritePlatformService {

    private final PlatformSecurityContext context;
    private final OfficeCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final OfficeTransactionCommandFromApiJsonDeserializer moneyTransferCommandFromApiJsonDeserializer;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final OfficeTransactionRepository officeTransactionRepository;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final TopicDomainService topicDomainService;

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "offices", key = "@fineractProperties.getTenantId().concat(#root.target.context.authenticatedUser().getOffice().getHierarchy()+'of')"),
            @CacheEvict(value = "officesForDropdown", key = "@fineractProperties.getTenantId().concat(#root.target.context.authenticatedUser().getOffice().getHierarchy()+'ofd')") })
    public CommandProcessingResult createOffice(final JsonCommand command) {

        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            Long parentId = null;
            if (command.parameterExists("parentId")) {
                parentId = command.longValueOfParameterNamed("parentId");
            }

            final Office parent = validateUserPriviledgeOnOfficeAndRetrieve(currentUser, parentId);
            final Office office = fromJson(parent, command);

            // pre save to generate id for use in office hierarchy
            this.officeRepositoryWrapper.save(office);

            office.generateHierarchy();

            this.officeRepositoryWrapper.save(office);
            
            this.topicDomainService.createTopic(office);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(office.getId()) //
                    .officeId(office.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleOfficeDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleOfficeDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "offices", key = "@fineractProperties.getTenantId().concat(#root.target.context.authenticatedUser().getOffice().getHierarchy()+'of')"),
            @CacheEvict(value = "officesForDropdown", key = "@fineractProperties.getTenantId().concat(#root.target.context.authenticatedUser().getOffice().getHierarchy()+'ofd')"),
            @CacheEvict(value = "officesById", key = "@fineractProperties.getTenantId().concat(#officeId)") })
    public CommandProcessingResult updateOffice(final Long officeId, final JsonCommand command) {

        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            Long parentId = null;
            if (command.parameterExists("parentId")) {
                parentId = command.longValueOfParameterNamed("parentId");
            }

            final Office office = validateUserPriviledgeOnOfficeAndRetrieve(currentUser, officeId);

            final Map<String, Object> changes = update(office, command);

            if (changes.containsKey("parentId")) {
                final Office parent = validateUserPriviledgeOnOfficeAndRetrieve(currentUser, parentId);
                update(office, parent);
            }

            if (!changes.isEmpty()) {
                this.officeRepositoryWrapper.saveAndFlush(office);
                
                this.topicDomainService.updateTopic(office, changes);
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(office.getId()) //
                    .officeId(office.getId()) //
                    .changes(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleOfficeDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleOfficeDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult officeTransaction(final JsonCommand command) {

        this.context.authenticatedUser();

        this.moneyTransferCommandFromApiJsonDeserializer.validateOfficeTransfer(command.json());

        Long officeId = null;
        Office fromOffice = null;
        final Long fromOfficeId = command.longValueOfParameterNamed("fromOfficeId");
        if (fromOfficeId != null) {
            fromOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(fromOfficeId);
            officeId = fromOffice.getId();
        }
        Office toOffice = null;
        final Long toOfficeId = command.longValueOfParameterNamed("toOfficeId");
        if (toOfficeId != null) {
            toOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(toOfficeId);
            officeId = toOffice.getId();
        }

        final String currencyCode = command.stringValueOfParameterNamed("currencyCode");
        final ApplicationCurrency appCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currencyCode);

        final MonetaryCurrency currency = new MonetaryCurrency(appCurrency.getCode(), appCurrency.getDecimalPlaces(),
                appCurrency.getInMultiplesOf());
        final Money amount = Money.of(currency, command.bigDecimalValueOfParameterNamed("transactionAmount"));

        final OfficeTransaction entity = fromJson(fromOffice, toOffice, amount, command);

        this.officeTransactionRepository.save(entity);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(entity.getId()) //
                .officeId(officeId) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteOfficeTransaction(final Long transactionId, final JsonCommand command) {

        this.context.authenticatedUser();

        this.officeTransactionRepository.deleteById(transactionId);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(transactionId) //
                .build();
    }

    private Office fromJson(final Office parentOffice, final JsonCommand command) {

        final String name = command.stringValueOfParameterNamed("name");
        final LocalDate openingDate = command.localDateValueOfParameterNamed("openingDate");
        final String externalId = command.stringValueOfParameterNamed("externalId");

        return Office.builder().parent(parentOffice).name(name).openingDate(openingDate.toDate()).externalId(externalId).build();
    }

    public void update(final Office office, final Office newParent) {

        if (office.getParent() == null) {
            throw new RootOfficeParentCannotBeUpdated();
        }

        if (office.getId().equals(newParent.getId())) {
            throw new CannotUpdateOfficeWithParentOfficeSameAsSelf(office.getId(), newParent.getId());
        }

        office.setParent(newParent);
        office.generateHierarchy();
    }

    private OfficeTransaction fromJson(final Office fromOffice, final Office toOffice, final Money amount, final JsonCommand command) {

        final LocalDate transactionLocalDate = command.localDateValueOfParameterNamed("transactionDate");
        final String description = command.stringValueOfParameterNamed("description");

        return OfficeTransaction.builder()
            .from(fromOffice)
            .to(toOffice)
            .transactionDate(transactionLocalDate.toDate())
            .transactionAmount(amount.getAmount())
            .description(description)
            .build();
    }

    private Map<String, Object> update(final Office office, final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        final String parentIdParamName = "parentId";

        if (command.parameterExists(parentIdParamName) && office.getParent() == null) {
            throw new RootOfficeParentCannotBeUpdated();
        }

        if (office.getParent() != null && command.isChangeInLongParameterNamed(parentIdParamName, office.getParent().getId())) {
            final Long newValue = command.longValueOfParameterNamed(parentIdParamName);
            actualChanges.put(parentIdParamName, newValue);
        }

        final String openingDateParamName = "openingDate";
        if (command.isChangeInLocalDateParameterNamed(openingDateParamName, office.getOpeningLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(openingDateParamName);
            actualChanges.put(openingDateParamName, valueAsInput);
            actualChanges.put("dateFormat", dateFormatAsInput);
            actualChanges.put("locale", localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(openingDateParamName);
            office.setOpeningDate(newValue.toDate());
        }

        final String nameParamName = "name";
        if (command.isChangeInStringParameterNamed(nameParamName, office.getName())) {
            final String newValue = command.stringValueOfParameterNamed(nameParamName);
            actualChanges.put(nameParamName, newValue);
            office.setName(newValue);
        }

        final String externalIdParamName = "externalId";
        if (command.isChangeInStringParameterNamed(externalIdParamName, office.getExternalId())) {
            final String newValue = command.stringValueOfParameterNamed(externalIdParamName);
            actualChanges.put(externalIdParamName, newValue);
            office.setExternalId(StringUtils.defaultIfEmpty(newValue, null));
        }

        return actualChanges;
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleOfficeDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("externalid_org")) {
            final String externalId = command.stringValueOfParameterNamed("externalId");
            throw new PlatformDataIntegrityException("error.msg.office.duplicate.externalId", "Office with externalId `" + externalId
                    + "` already exists", "externalId", externalId);
        } else if (realCause.getMessage().contains("name_org")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.office.duplicate.name", "Office with name `" + name + "` already exists",
                    "name", name);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.office.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    /*
     * used to restrict modifying operations to office that are either the users
     * office or lower (child) in the office hierarchy
     */
    private Office validateUserPriviledgeOnOfficeAndRetrieve(final AppUser currentUser, final Long officeId) {

        final Long userOfficeId = currentUser.getOffice().getId();
        final Office userOffice = this.officeRepositoryWrapper.findOfficeHierarchy(userOfficeId);
        if (userOffice.doesNotHaveAnOfficeInHierarchyWithId(officeId)) { throw new NoAuthorizationException(
                "User does not have sufficient priviledges to act on the provided office."); }

        Office officeToReturn = userOffice;
        if (!userOffice.getId().equals(officeId)) {
            officeToReturn = this.officeRepositoryWrapper.findOfficeHierarchy(officeId);
        }

        return officeToReturn;
    }

    public PlatformSecurityContext getContext() {
        return this.context;
    }
}
