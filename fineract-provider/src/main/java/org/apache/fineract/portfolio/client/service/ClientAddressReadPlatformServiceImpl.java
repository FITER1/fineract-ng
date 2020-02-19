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
package org.apache.fineract.portfolio.client.service;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.address.data.ClientAddressData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class ClientAddressReadPlatformServiceImpl implements ClientAddressReadPlatformService {

	private final JdbcTemplate jdbcTemplate;
	private final PlatformSecurityContext context;

	private static final class ClientAddrMapper implements RowMapper<ClientAddressData> {
		public String schema() {
			return "fld.id as fieldConfigurationId,fld.entity as entity,fld.table as entitytable,fld.field as field,fld.is_enabled as is_enabled,"
					+ "fld.is_mandatory as is_mandatory,fld.validation_regex as validation_regex from m_field_configuration fld";
		}

		@Override
		public ClientAddressData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
			final Long clientAddressId = rs.getLong("clientAddressId");
			final Long clientId = rs.getLong("client_id");
			final Long addressId = rs.getLong("address_id");
			final Long addressTypeId = rs.getLong("address_type_id");
			final boolean isActive = rs.getBoolean("is_active");

			return ClientAddressData.builder()
				.clientAddressId(clientAddressId)
				.clientId(clientId)
				.addressId(addressId)
				.addressTypeId(addressTypeId)
				.active(isActive)
				.build();

		}
	}

	@Override
	public Collection<ClientAddressData> retrieveClientAddrConfiguration(final String entity) {
		this.context.authenticatedUser();

		final ClientAddrMapper rm = new ClientAddrMapper();
		final String sql = "select " + rm.schema() + " where fld.entity=?";

		return this.jdbcTemplate.query(sql, rm, new Object[] { entity });
	}

}
