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
package org.apache.fineract.portfolio.floatingrates.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.floatingrates.domain.FloatingRate;
import org.apache.fineract.portfolio.floatingrates.domain.FloatingRatePeriod;
import org.apache.fineract.portfolio.floatingrates.domain.FloatingRateRepositoryWrapper;
import org.apache.fineract.portfolio.floatingrates.serialization.FloatingRateDataValidator;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FloatingRateWritePlatformServiceImpl implements FloatingRateWritePlatformService {

	private final PlatformSecurityContext context;
	private final FloatingRateDataValidator fromApiJsonDeserializer;
	private final FloatingRateRepositoryWrapper floatingRateRepository;

	@Transactional
	@Override
	public CommandProcessingResult createFloatingRate(final JsonCommand command) {
		try {
			this.fromApiJsonDeserializer.validateForCreate(command.json());
			final AppUser currentUser = this.context.authenticatedUser();
			final FloatingRate newFloatingRate = createNew(currentUser, command);
			this.floatingRateRepository.save(newFloatingRate);
			return CommandProcessingResult.builder() //
					.commandId(command.commandId()) //
					.resourceId(newFloatingRate.getId()) //
					.build();
		} catch (final DataIntegrityViolationException dve) {
			handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
			return new CommandProcessingResult();
		}catch (final PersistenceException dve) {
			Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            handleDataIntegrityIssues(command, throwable, dve);
         	return new CommandProcessingResult();
		}
	}

	@Transactional
	@Override
	public CommandProcessingResult updateFloatingRate(final JsonCommand command) {
		try {
			final FloatingRate floatingRateForUpdate = this.floatingRateRepository.findOneWithNotFoundDetection(command.entityId());
			this.fromApiJsonDeserializer.validateForUpdate(command.json(), floatingRateForUpdate);
			final AppUser currentUser = this.context.authenticatedUser();
			final Map<String, Object> changes = update(floatingRateForUpdate, command, currentUser);

			if (!changes.isEmpty()) {
				this.floatingRateRepository.save(floatingRateForUpdate);
			}

			return CommandProcessingResult.builder() //
					.commandId(command.commandId()) //
					.resourceId(command.entityId()) //
					.changes(changes) //
					.build();
		} catch (final DataIntegrityViolationException dve) {
			handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
			return new CommandProcessingResult();
		}catch (final PersistenceException dve) {
			Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
            handleDataIntegrityIssues(command, throwable, dve);
         	return new CommandProcessingResult();
		}
	}

	private FloatingRate createNew(AppUser currentUser, JsonCommand command) {
		final String name = command.stringValueOfParameterNamed("name");
		final boolean isBaseLendingRate = command.parameterExists("isBaseLendingRate") && command.booleanPrimitiveValueOfParameterNamed("isBaseLendingRate");
		final boolean isActive = !command.parameterExists("isActive") || command.booleanPrimitiveValueOfParameterNamed("isActive");
		final List<FloatingRatePeriod> floatingRatePeriods = getRatePeriods(currentUser, command);
		final LocalDate currentDate = DateUtils.getLocalDateOfTenant();

		return FloatingRate.builder()
			.name(name)
			.baseLendingRate(isBaseLendingRate)
			.active(isActive)
			.floatingRatePeriods(floatingRatePeriods)
			.createdBy(currentUser)
			.modifiedBy(currentUser)
			.createdOn(currentDate.toDate())
			.modifiedOn(currentDate.toDate())
			.build();
	}

	private List<FloatingRatePeriod> getRatePeriods(final AppUser currentUser, final JsonCommand command) {
		if (!command.parameterExists("ratePeriods")) {
			return null;
		}
		List<FloatingRatePeriod> ratePeriods = new ArrayList<>();
		JsonArray arrayOfParameterNamed = command.arrayOfParameterNamed("ratePeriods");
		for (final JsonElement ratePeriod : arrayOfParameterNamed) {
			final JsonObject ratePeriodObject = ratePeriod.getAsJsonObject();
			final JsonParserHelper helper = new JsonParserHelper();
			final Date fromDate = helper.extractLocalDateNamed("fromDate", ratePeriod, new HashSet<String>()).toDate();
			final BigDecimal interestRate = ratePeriodObject.get("interestRate").getAsBigDecimal();
			final boolean isDifferentialToBaseLendingRate = helper.parameterExists("isDifferentialToBaseLendingRate", ratePeriod) && ratePeriodObject.get("isDifferentialToBaseLendingRate").getAsBoolean();
			final boolean isActive = true;
			final Date currentDate = DateUtils.getDateOfTenant();
			ratePeriods.add(FloatingRatePeriod.builder()
				.fromDate(fromDate)
				.interestRate(interestRate)
				.differentialToBaseLendingRate(isDifferentialToBaseLendingRate)
				.active(isActive)
				.createdBy(currentUser)
				.modifiedBy(currentUser)
				.createdOn(currentDate)
				.modifiedOn(currentDate)
				.build());
		}

		return ratePeriods;
	}

	private Map<String, Object> update(final FloatingRate floatingRate, final JsonCommand command, final AppUser appUser) {

		final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

		if (command.isChangeInStringParameterNamed("name", floatingRate.getName())) {
			final String newValue = command.stringValueOfParameterNamed("name");
			actualChanges.put("name", newValue);
			floatingRate.setName(newValue);
		}

		if (command.isChangeInBooleanParameterNamed("isBaseLendingRate", floatingRate.isBaseLendingRate())) {
			final boolean newValue = command.booleanPrimitiveValueOfParameterNamed("isBaseLendingRate");
			actualChanges.put("isBaseLendingRate", newValue);
			floatingRate.setBaseLendingRate(newValue);
		}

		if (command.isChangeInBooleanParameterNamed("isActive", floatingRate.isActive())) {
			final boolean newValue = command.booleanPrimitiveValueOfParameterNamed("isActive");
			actualChanges.put("isActive", newValue);
			floatingRate.setActive(newValue);
		}

		final List<FloatingRatePeriod> newRatePeriods = getRatePeriods(appUser, command);
		if (newRatePeriods != null && !newRatePeriods.isEmpty()) {
			updateRatePeriods(floatingRate, newRatePeriods, appUser);
			actualChanges.put("ratePeriods", command.jsonFragment("ratePeriods"));
		}

		return actualChanges;
	}

	private void updateRatePeriods(final FloatingRate floatingRate, final List<FloatingRatePeriod> newRatePeriods, final AppUser appUser) {
		final LocalDate today = DateUtils.getLocalDateOfTenant();
		if (floatingRate.getFloatingRatePeriods() != null) {
			for (FloatingRatePeriod ratePeriod : floatingRate.getFloatingRatePeriods()) {
				LocalDate fromDate = LocalDate.fromDateFields(ratePeriod.getFromDate());
				if (fromDate.isAfter(today)) {
					ratePeriod.setActive(false);
					ratePeriod.setModifiedBy(appUser);
					ratePeriod.setModifiedOn(today.toDate());
				}
			}
		}
		for (FloatingRatePeriod newRatePeriod : newRatePeriods) {
			newRatePeriod.setFloatingRate(floatingRate);
			floatingRate.getFloatingRatePeriods().add(newRatePeriod);
		}
	}

	private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause,
			final Exception dve) {

		if (realCause.getMessage().contains("unq_name")) {

			final String name = command.stringValueOfParameterNamed("name");
			throw new PlatformDataIntegrityException(
					"error.msg.floatingrates.duplicate.name",
					"Floating Rate with name `" + name + "` already exists",
					"name", name);
		}

		if (realCause.getMessage().contains("unq_rate_period")) {
			throw new PlatformDataIntegrityException(
					"error.msg.floatingrates.duplicate.active.fromdate",
					"Attempt to add multiple floating rate periods with same fromdate",
					"fromdate", "");
		}

		log.error(dve.getMessage(), dve);
		throw new PlatformDataIntegrityException(
				"error.msg.floatingrates.unknown.data.integrity.issue",
				"Unknown data integrity issue with resource.");
	}
}
