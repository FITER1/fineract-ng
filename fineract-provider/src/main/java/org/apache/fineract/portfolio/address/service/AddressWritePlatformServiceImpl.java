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
package org.apache.fineract.portfolio.address.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.address.domain.Address;
import org.apache.fineract.portfolio.address.domain.AddressRepository;
import org.apache.fineract.portfolio.address.serialization.AddressCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.client.domain.*;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AddressWritePlatformServiceImpl implements AddressWritePlatformService {
	private final PlatformSecurityContext context;
	private final CodeValueRepository codeValueRepository;
	private final ClientAddressRepository clientAddressRepository;
	private final ClientRepositoryWrapper clientRepositoryWrapper;
	private final AddressRepository addressRepository;
	private final ClientAddressRepositoryWrapper clientAddressRepositoryWrapper;
	private final AddressCommandFromApiJsonDeserializer fromApiJsonDeserializer;

	@Override
	public CommandProcessingResult addClientAddress(final Long clientId, final Long addressTypeId,
			final JsonCommand command) {
		CodeValue stateIdobj = null;
		CodeValue countryIdObj = null;
		long stateId;
		long countryId;

		this.context.authenticatedUser();
		this.fromApiJsonDeserializer.validateForCreate(command.json(), true);

		if (command.longValueOfParameterNamed("stateProvinceId") != null) {
			stateId = command.longValueOfParameterNamed("stateProvinceId");
			stateIdobj = this.codeValueRepository.getOne(stateId);
		}

		if (command.longValueOfParameterNamed("countryId") != null) {
			countryId = command.longValueOfParameterNamed("countryId");
			countryIdObj = this.codeValueRepository.getOne(countryId);
		}

		final CodeValue addressTypeIdObj = this.codeValueRepository.getOne(addressTypeId);

		final Address add = fromJson(command, stateIdobj, countryIdObj);
		this.addressRepository.save(add);
		final Long addressid = add.getId();
		final Address addobj = this.addressRepository.getOne(addressid);

		final Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
		final boolean isActive = command.booleanPrimitiveValueOfParameterNamed("isActive");

		final ClientAddress clientAddressobj = ClientAddress.fromJson(isActive, client, addobj, addressTypeIdObj);
		this.clientAddressRepository.save(clientAddressobj);

		return CommandProcessingResult.builder().commandId(command.commandId())
				.resourceId(clientAddressobj.getId()).build();
	}

	// following method is used for adding multiple addresses while creating new
	// client

	@Override
	public CommandProcessingResult addNewClientAddress(final Client client, final JsonCommand command) {
		CodeValue stateIdobj = null;
		CodeValue countryIdObj = null;
		long stateId;
		long countryId;
		ClientAddress clientAddressobj = new ClientAddress();
		final JsonArray addressArray = command.arrayOfParameterNamed("address");
		
		if(addressArray != null){
			for (int i = 0; i < addressArray.size(); i++) {
				final JsonObject jsonObject = addressArray.get(i).getAsJsonObject();

				// validate every address
				this.fromApiJsonDeserializer.validateForCreate(jsonObject.toString(), true);

				if (jsonObject.get("stateProvinceId") != null) {
					stateId = jsonObject.get("stateProvinceId").getAsLong();
					stateIdobj = this.codeValueRepository.getOne(stateId);
				}

				if (jsonObject.get("countryId") != null) {
					countryId = jsonObject.get("countryId").getAsLong();
					countryIdObj = this.codeValueRepository.getOne(countryId);
				}

				final long addressTypeId = jsonObject.get("addressTypeId").getAsLong();
				final CodeValue addressTypeIdObj = this.codeValueRepository.getOne(addressTypeId);

				final Address add = fromJsonObject(jsonObject, stateIdobj, countryIdObj);
				this.addressRepository.save(add);
				final Long addressid = add.getId();
				final Address addobj = this.addressRepository.getOne(addressid);

				//final boolean isActive = jsonObject.get("isActive").getAsBoolean();
				boolean isActive=false;
				if(jsonObject.get("isActive")!= null)
				{
					isActive= jsonObject.get("isActive").getAsBoolean();
				}
				

				clientAddressobj = ClientAddress.fromJson(isActive, client, addobj, addressTypeIdObj);
				this.clientAddressRepository.save(clientAddressobj);

			}
		}

		return CommandProcessingResult.builder().commandId(command.commandId())
				.resourceId(clientAddressobj.getId()).build();
	}

	@Override
	public CommandProcessingResult updateClientAddress(final Long clientId, final JsonCommand command) {
		this.context.authenticatedUser();

		long stateId;

		long countryId;

		CodeValue stateIdobj;

		CodeValue countryIdObj;

		boolean is_address_update = false;

		this.fromApiJsonDeserializer.validateForUpdate(command.json());

		final long addressId = command.longValueOfParameterNamed("addressId");

		final ClientAddress clientAddressObj = this.clientAddressRepositoryWrapper
				.findOneByClientIdAndAddressId(clientId, addressId);

		final Address addobj = this.addressRepository.getOne(addressId);

		if (!(command.stringValueOfParameterNamed("street").isEmpty())) {

			is_address_update = true;
			final String street = command.stringValueOfParameterNamed("street");
			addobj.setStreet(street);
		}

		if (!(command.stringValueOfParameterNamed("addressLine1").isEmpty())) {

			is_address_update = true;
			final String addressLine1 = command.stringValueOfParameterNamed("addressLine1");
			addobj.setAddressLine1(addressLine1);

		}

		if (!(command.stringValueOfParameterNamed("addressLine2").isEmpty())) {

			is_address_update = true;
			final String addressLine2 = command.stringValueOfParameterNamed("addressLine2");
			addobj.setAddressLine2(addressLine2);

		}

		if (!(command.stringValueOfParameterNamed("addressLine3").isEmpty())) {
			is_address_update = true;
			final String addressLine3 = command.stringValueOfParameterNamed("addressLine3");
			addobj.setAddressLine3(addressLine3);

		}

		if (!(command.stringValueOfParameterNamed("townVillage").isEmpty())) {

			is_address_update = true;
			final String townVillage = command.stringValueOfParameterNamed("townVillage");
			addobj.setTownVillage(townVillage);
		}

		if (!(command.stringValueOfParameterNamed("city").isEmpty())) {
			is_address_update = true;
			final String city = command.stringValueOfParameterNamed("city");
			addobj.setCity(city);
		}

		if (!(command.stringValueOfParameterNamed("countyDistrict").isEmpty())) {
			is_address_update = true;
			final String countyDistrict = command.stringValueOfParameterNamed("countyDistrict");
			addobj.setCountyDistrict(countyDistrict);
		}

		if ((command.longValueOfParameterNamed("stateProvinceId") != null)) {
			if ((command.longValueOfParameterNamed("stateProvinceId") != 0)) {
				is_address_update = true;
				stateId = command.longValueOfParameterNamed("stateProvinceId");
				stateIdobj = this.codeValueRepository.getOne(stateId);
				addobj.setStateProvince(stateIdobj);
			}

		}
		if ((command.longValueOfParameterNamed("countryId") != null)) {
			if ((command.longValueOfParameterNamed("countryId") != 0)) {
				is_address_update = true;
				countryId = command.longValueOfParameterNamed("countryId");
				countryIdObj = this.codeValueRepository.getOne(countryId);
				addobj.setCountry(countryIdObj);
			}

		}

		if (!(command.stringValueOfParameterNamed("postalCode").isEmpty())) {
			is_address_update = true;
			final String postalCode = command.stringValueOfParameterNamed("postalCode");
			addobj.setPostalCode(postalCode);
		}

		if (command.bigDecimalValueOfParameterNamed("latitude") != null) {

			is_address_update = true;
			final BigDecimal latitude = command.bigDecimalValueOfParameterNamed("latitude");

			addobj.setLatitude(latitude);
		}
		if (command.bigDecimalValueOfParameterNamed("longitude") != null) {
			is_address_update = true;
			final BigDecimal longitude = command.bigDecimalValueOfParameterNamed("longitude");
			addobj.setLongitude(longitude);

		}

		if (is_address_update) {

			this.addressRepository.save(addobj);

		}

		final Boolean testActive = command.booleanPrimitiveValueOfParameterNamed("isActive");
		if (testActive != null) {

			final boolean active = command.booleanPrimitiveValueOfParameterNamed("isActive");
			clientAddressObj.setActive(active);

		}

		return CommandProcessingResult.builder().commandId(command.commandId())
				.resourceId(clientAddressObj.getId()).build();
	}


	private Address fromJsonObject(final JsonObject jsonObject, final CodeValue stateProvince, final CodeValue country) {
		String street = "";
		String addressLine1 = "";
		String addressLine2 = "";
		String addressLine3 = "";
		String townVillage = "";
		String city = "";
		String countyDistrict = "";
		String postalCode = "";
		BigDecimal latitude = BigDecimal.ZERO;
		BigDecimal longitude = BigDecimal.ZERO;
		String createdBy = "";
		String updatedBy = "";
		LocalDate updatedOnDate = null;
		LocalDate createdOnDate = null;

		if (jsonObject.has("street")) {
			street = jsonObject.get("street").getAsString();
		}

		if (jsonObject.has("addressLine1")) {
			addressLine1 = jsonObject.get("addressLine1").getAsString();
		}
		if (jsonObject.has("addressLine2")) {

			addressLine2 = jsonObject.get("addressLine2").getAsString();
		}
		if (jsonObject.has("addressLine3")) {
			addressLine3 = jsonObject.get("addressLine3").getAsString();
		}
		if (jsonObject.has("townVillage")) {
			townVillage = jsonObject.get("townVillage").getAsString();
		}
		if (jsonObject.has("city")) {
			city = jsonObject.get("city").getAsString();
		}
		if (jsonObject.has("countyDistrict")) {
			countyDistrict = jsonObject.get("countyDistrict").getAsString();
		}
		if (jsonObject.has("postalCode")) {
			postalCode = jsonObject.get("postalCode").getAsString();
		}
		if (jsonObject.has("latitude")) {

			latitude = jsonObject.get("latitude").getAsBigDecimal();
		}
		if (jsonObject.has("longitude")) {
			longitude = jsonObject.get("longitude").getAsBigDecimal();
		}

		if (jsonObject.has("createdBy")) {
			createdBy = jsonObject.get("createdBy").getAsString();
		}
		if (jsonObject.has("createdOn")) {
			String createdOn = jsonObject.get("createdOn").getAsString();
			DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
			createdOnDate = LocalDate.parse(createdOn, formatter);

		}
		if (jsonObject.has("updatedBy")) {
			updatedBy = jsonObject.get("updatedBy").getAsString();
		}
		if (jsonObject.has("updatedOn")) {
			String updatedOn = jsonObject.get("updatedOn").getAsString();
			DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
			updatedOnDate = LocalDate.parse(updatedOn, formatter);
		}

		return Address.builder()
			.street(street)
			.addressLine1(addressLine1)
			.addressLine2(addressLine2)
			.addressLine3(addressLine3)
			.townVillage(townVillage)
			.city(city)
			.countyDistrict(countyDistrict)
			.stateProvince(stateProvince)
			.country(country)
			.postalCode(postalCode)
			.latitude(latitude)
			.longitude(longitude)
			.createdBy(createdBy)
			.createdOn(createdOnDate==null ? null : createdOnDate.toDate())
			.updatedBy(updatedBy)
			.updatedOn(updatedOnDate==null ? null : updatedOnDate.toDate())
			.build();
	}

	private Address fromJson(final JsonCommand command, final CodeValue stateProvince, final CodeValue country) {

		final String street = command.stringValueOfParameterNamed("street");
		final String addressLine1 = command.stringValueOfParameterNamed("addressLine1");
		final String addressLine2 = command.stringValueOfParameterNamed("addressLine2");
		final String addressLine3 = command.stringValueOfParameterNamed("addressLine3");
		final String townVillage = command.stringValueOfParameterNamed("townVillage");
		final String city = command.stringValueOfParameterNamed("city");
		final String countyDistrict = command.stringValueOfParameterNamed("countyDistrict");
		final String postalCode = command.stringValueOfParameterNamed("postalCode");
		final BigDecimal latitude = command.bigDecimalValueOfParameterNamed("latitude");
		final BigDecimal longitude = command.bigDecimalValueOfParameterNamed("longitude");
		final String createdBy = command.stringValueOfParameterNamed("createdBy");
		final LocalDate createdOn = command.localDateValueOfParameterNamed("createdOn");
		final String updatedBy = command.stringValueOfParameterNamed("updatedBy");
		final LocalDate updatedOn = command.localDateValueOfParameterNamed("updatedOn");

		return Address.builder()
			.street(street)
			.addressLine1(addressLine1)
			.addressLine2(addressLine2)
			.addressLine3(addressLine3)
			.townVillage(townVillage)
			.city(city)
			.countyDistrict(countyDistrict)
			.stateProvince(stateProvince)
			.country(country)
			.postalCode(postalCode)
			.latitude(latitude)
			.longitude(longitude)
			.createdBy(createdBy)
			.createdOn(createdOn==null ? null : createdOn.toDate())
			.updatedBy(updatedBy)
			.updatedOn(updatedOn==null ? null : updatedOn.toDate())
			.build();
	}
}
