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
package org.apache.fineract.portfolio.address.domain;

import com.google.gson.JsonObject;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.domain.ClientAddress;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_address")
public class Address extends AbstractPersistableCustom<Long> {

	/*
	 * @OneToMany(mappedBy = "address", cascade = CascadeType.ALL) private
	 * List<ClientAddress> clientaddress = new ArrayList<>();
	 */

	@OneToMany(mappedBy = "address", cascade = CascadeType.ALL)
	private Set<ClientAddress> clientaddress;

	@Column(name = "street")
	private String street;

	@Column(name = "address_line_1")
	private String addressLine1;

	@Column(name = "address_line_2")
	private String addressLine2;

	@Column(name = "address_line_3")
	private String addressLine3;

	@Column(name = "town_village")
	private String townVillage;

	@Column(name = "city")
	private String city;

	@Column(name = "county_district")
	private String countyDistrict;

	@ManyToOne
	@JoinColumn(name = "state_province_id")
	private CodeValue stateProvince;

	@ManyToOne
	@JoinColumn(name = "country_id")
	private CodeValue country;

	@Column(name = "postal_code")
	private String postalCode;

	@Column(name = "latitude")
	private BigDecimal latitude;

	@Column(name = "longitude")
	private BigDecimal longitude;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "created_on")
	private Date createdOn;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "updated_on")
	private Date updatedOn;

	public static Address fromJson(final JsonCommand command, final CodeValue stateProvince, final CodeValue country) {

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

	public static Address fromJsonObject(final JsonObject jsonObject, final CodeValue stateProvince,
			final CodeValue country) {
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
		Locale locale = Locale.ENGLISH;
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
}
