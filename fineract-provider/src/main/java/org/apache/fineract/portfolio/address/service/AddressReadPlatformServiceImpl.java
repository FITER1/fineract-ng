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

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.address.data.AddressData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressReadPlatformServiceImpl implements AddressReadPlatformService {

	private final JdbcTemplate jdbcTemplate;
	private final PlatformSecurityContext context;
	private final CodeValueReadPlatformService readService;

	private static final class AddFieldsMapper implements RowMapper<AddressData> {
		public String schema() {
			return "addr.id as id,client.id as client_id,addr.street as street,addr.address_line_1 as address_line_1,addr.address_line_2 as address_line_2,"
					+ "addr.address_line_3 as address_line_3,addr.town_village as town_village, addr.city as city,addr.county_district as county_district,"
					+ "addr.state_province_id as state_province_id, addr.country_id as country_id,addr.postal_code as postal_code,addr.latitude as latitude,"
					+ "addr.longitude as longitude,addr.created_by as created_by,addr.created_on as created_on,addr.updated_by as updated_by,"
					+ "addr.updated_on as updated_on from m_address as addr,m_client client";
		}

		@Override
		public AddressData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final long addressId = rs.getLong("id");

			final long clientId = rs.getLong("client_id");

			final String street = rs.getString("street");

			final String addressLine1 = rs.getString("address_line_1");

			final String addressLine2 = rs.getString("address_line_2");

			final String addressLine3 = rs.getString("address_line_3");

			final String townVillage = rs.getString("town_village");

			final String city = rs.getString("city");

			final String countyDistrict = rs.getString("county_district");

			final long stateProvinceId = rs.getLong("state_province_id");

			final long countryId = rs.getLong("country_id");

			final String postalCode = rs.getString("postal_code");

			final BigDecimal latitude = rs.getBigDecimal("latitude");

			final BigDecimal longitude = rs.getBigDecimal("longitude");

			final String createdBy = rs.getString("created_by");

			final Date createdOn = rs.getDate("created_on");

			final String updatedBy = rs.getString("updated_by");

			final Date updatedOn = rs.getDate("updated_on");

			return AddressData.builder()
				.addressId(addressId)
				.street(street)
				.addressLine1(addressLine1)
				.addressLine2(addressLine2)
				.addressLine3(addressLine3)
				.townVillage(townVillage)
				.city(city)
				.countyDistrict(countyDistrict)
				.stateProvinceId(stateProvinceId)
				.countryId(countryId)
				.postalCode(postalCode)
				.latitude(latitude)
				.longitude(longitude)
				.createdBy(createdBy)
				.createdOn(createdOn)
				.updatedBy(updatedBy)
				.updatedOn(updatedOn)
				.build();
		}
	}

	private static final class AddMapper implements RowMapper<AddressData> {
		public String schema() {
			return "cv2.code_value as addressType,ca.client_id as client_id,addr.id as id,ca.address_type_id as addresstyp,ca.is_active as is_active,addr.street as street,addr.address_line_1 as address_line_1,addr.address_line_2 as address_line_2,"
					+ "addr.address_line_3 as address_line_3,addr.town_village as town_village, addr.city as city,addr.county_district as county_district,"
					+ "addr.state_province_id as state_province_id,cv.code_value as state_name, addr.country_id as country_id,c.code_value as country_name,addr.postal_code as postal_code,addr.latitude as latitude,"
					+ "addr.longitude as longitude,addr.created_by as created_by,addr.created_on as created_on,addr.updated_by as updated_by,"
					+ "addr.updated_on as updated_on"
					+ " from m_address addr left join m_code_value cv on addr.state_province_id=cv.id"
					+ " left join  m_code_value c on addr.country_id=c.id"
					+ " join m_client_address ca on addr.id= ca.address_id"
					+ " join m_code_value cv2 on ca.address_type_id=cv2.id";

		}

		@Override
		public AddressData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final String addressType = rs.getString("addressType");
			final long addressId = rs.getLong("id");

			final long clientId = rs.getLong("client_id");

			final String street = rs.getString("street");

			final long addressTypeId = rs.getLong("addresstyp");

			final boolean isActive = rs.getBoolean("is_active");

			final String addressLine1 = rs.getString("address_line_1");

			final String addressLine2 = rs.getString("address_line_2");

			final String addressLine3 = rs.getString("address_line_3");

			final String townVillage = rs.getString("town_village");

			final String city = rs.getString("city");

			final String countyDistrict = rs.getString("county_district");

			final long stateProvinceId = rs.getLong("state_province_id");

			final long countryId = rs.getLong("country_id");

			final String countryName = rs.getString("country_name");

			final String stateName = rs.getString("state_name");

			final String postalCode = rs.getString("postal_code");

			final BigDecimal latitude = rs.getBigDecimal("latitude");

			final BigDecimal longitude = rs.getBigDecimal("longitude");

			final String createdBy = rs.getString("created_by");

			final Date createdOn = rs.getDate("created_on");

			final String updatedBy = rs.getString("updated_by");

			final Date updatedOn = rs.getDate("updated_on");

			return AddressData.builder()
				.addressType(addressType)
				.clientId(clientId)
				.addressId(addressId)
				.addressTypeId(addressTypeId)
				.active(isActive)
				.street(street)
				.addressLine1(addressLine1)
				.addressLine2(addressLine2)
				.addressLine3(addressLine3)
				.townVillage(townVillage)
				.city(city)
				.countyDistrict(countyDistrict)
				.stateProvinceId(stateProvinceId)
				.countryId(countryId)
				.stateName(stateName)
				.countryName(countryName)
				.postalCode(postalCode)
				.latitude(latitude)
				.longitude(longitude)
				.createdBy(createdBy)
				.createdOn(createdOn)
				.updatedBy(updatedBy)
				.updatedOn(updatedOn)
				.build();
		}
	}

	@Override
	public Collection<AddressData> retrieveAddressFields(final long clientid) {
		this.context.authenticatedUser();

		final AddFieldsMapper rm = new AddFieldsMapper();
		final String sql = "select " + rm.schema() + " where client.id=?";

		return this.jdbcTemplate.query(sql, rm, new Object[] { clientid });
	}

	@Override
	public Collection<AddressData> retrieveAllClientAddress(final long clientid) {
		this.context.authenticatedUser();
		final AddMapper rm = new AddMapper();
		final String sql = "select " + rm.schema() + " and ca.client_id=?";
		return this.jdbcTemplate.query(sql, rm, new Object[] { clientid });
	}

	@Override
	public Collection<AddressData> retrieveAddressbyType(final long clientid, final long typeid) {
		this.context.authenticatedUser();

		final AddMapper rm = new AddMapper();
		final String sql = "select " + rm.schema() + " and ca.client_id=? and ca.address_type_id=?";

		return this.jdbcTemplate.query(sql, rm, new Object[] { clientid, typeid });
	}

	@Override
	public Collection<AddressData> retrieveAddressbyTypeAndStatus(final long clientid, final long typeid,
			final String status) {
		this.context.authenticatedUser();
		Boolean temp = false;
		temp = Boolean.parseBoolean(status);

		final AddMapper rm = new AddMapper();
		final String sql = "select " + rm.schema() + " and ca.client_id=? and ca.address_type_id=? and ca.is_active=?";

		return this.jdbcTemplate.query(sql, rm, new Object[] { clientid, typeid, temp });
	}

	@Override
	public Collection<AddressData> retrieveAddressbyStatus(final long clientid, final String status) {
		this.context.authenticatedUser();
		Boolean temp = false;
		temp = Boolean.parseBoolean(status);

		final AddMapper rm = new AddMapper();
		final String sql = "select " + rm.schema() + " and ca.client_id=? and ca.is_active=?";

		return this.jdbcTemplate.query(sql, rm, new Object[] { clientid, temp });
	}

	@Override
	public AddressData retrieveTemplate() {
		final List<CodeValueData> countryoptions = new ArrayList<>(
				this.readService.retrieveCodeValuesByCode("COUNTRY"));

		final List<CodeValueData> stateOptions = new ArrayList<>(this.readService.retrieveCodeValuesByCode("STATE"));

		final List<CodeValueData> addressTypeOptions = new ArrayList<>(
				this.readService.retrieveCodeValuesByCode("ADDRESS_TYPE"));

		return AddressData.builder()
			.countryIdOptions(countryoptions)
			.stateProvinceIdOptions(stateOptions)
			.addressTypeIdOptions(addressTypeOptions)
			.build();
	}
}
