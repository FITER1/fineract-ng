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
package org.apache.fineract.adhocquery.service;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.adhocquery.data.AdHocData;
import org.apache.fineract.adhocquery.exception.AdHocNotFoundException;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.joda.time.DateTime;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class AdHocReadPlatformServiceImpl implements AdHocReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final AdHocMapper adHocRowMapper = new AdHocMapper();

    @Override
    public Collection<AdHocData> retrieveAllAdHocQuery() {
        final String sql = "select " + this.adHocRowMapper.schema() + " order by r.id";

        return this.jdbcTemplate.query(sql, this.adHocRowMapper);
    }

    @Override
    public Collection<AdHocData> retrieveAllActiveAdHocQuery() {
        final String sql = "select " + this.adHocRowMapper.schema() + " where r.IsActive = 1 order by r.id";

        return this.jdbcTemplate.query(sql, this.adHocRowMapper);
    }

    @Override
    public AdHocData retrieveOne(final Long id) {

        try {
            final String sql = "select " + this.adHocRowMapper.schema() + " where r.id=?";

            return this.jdbcTemplate.queryForObject(sql, this.adHocRowMapper, new Object[] { id });
        } catch (final EmptyResultDataAccessException e) {
            throw new AdHocNotFoundException(id);
        }
    }

    protected static final class AdHocMapper implements RowMapper<AdHocData> {

        @Override
        public AdHocData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = JdbcSupport.getLong(rs, "id");
            final String name = rs.getString("name");
            final String query = rs.getString("query");
            final String tableName=rs.getString("tableName");
            final String tableFields=rs.getString("tableField");
            final Boolean isActive = rs.getBoolean("isActive");
            final DateTime createdDate = JdbcSupport.getDateTime(rs, "createdDate");
            final Long createdById = JdbcSupport.getLong(rs, "createdById");
            final Long updatedById=JdbcSupport.getLong(rs, "updatedById");
            final DateTime updatedOn=JdbcSupport.getDateTime(rs, "updatedOn");
            final String createdByUsername=rs.getString("createdBy");
            final String email=rs.getString("email");
            final Long reportRunFrequency=JdbcSupport.getLong(rs, "report_run_frequency_code");
            final Long reportRunEvery=JdbcSupport.getLong(rs, "report_run_every");
            final DateTime lastRun = JdbcSupport.getDateTime(rs, "last_run");

            return AdHocData.builder()
                .id(id)
                .name(name)
                .query(query)
                .tableName(tableName)
                .tableFields(tableFields)
                .active(isActive)
                .createdOn(createdDate)
                .createdById(createdById)
                .updatedById(updatedById)
                .updatedOn(updatedOn)
                .createdBy(createdByUsername)
                .email(email)
                .reportRunFrequencies(AdHocData.createReportRunFrequencies())
                .reportRunFrequency(reportRunFrequency)
                .reportRunEvery(reportRunEvery)
                .lastRun(lastRun)
                .build();
        }

        public String schema() {
            return " r.id as id, r.name as name, r.query as query, r.table_name as tableName,r.table_fields as tableField ,r.IsActive as isActive ,r.email as email ,"
                    + " r.report_run_frequency_code, r.report_run_every, r.last_run, "
            		+ " r.created_date as createdDate, r.createdby_id as createdById,cb.username as createdBy,r.lastmodifiedby_id as updatedById ,r.lastmodified_date as updatedOn "
                    + " from m_adhoc r left join m_appuser cb on cb.id=r.createdby_id left join m_appuser mb on mb.id=r.lastmodifiedby_id";
            		        
        }
    }

	@Override
	public AdHocData retrieveNewAdHocDetails() {
        return AdHocData.builder()
            .reportRunFrequencies(AdHocData.createReportRunFrequencies())
            .build();
	}
}