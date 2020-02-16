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
package org.apache.fineract.infrastructure.core.data;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PaginationParameters {
    public static final Integer DEFAULT_CHECKED_LIMIT = 200;

    private boolean paged;
    private Integer offset;
    private Integer limit;
    private String orderBy;
    private String sortOrder;

    public boolean isLimited() {
        return this.limit != null && this.limit > 0;
    }

    public String orderBySql() {
        final StringBuffer sql = new StringBuffer();

        if (StringUtils.isNotBlank(this.getOrderBy())) {
            sql.append(" order by ").append(this.getOrderBy());
            if (StringUtils.isNotBlank(this.getSortOrder())) {
                sql.append(' ').append(this.getSortOrder());
            }
        }
        return sql.toString();
    }

    public String limitSql() {
        final StringBuffer sql = new StringBuffer();
        if (this.isLimited()) {
            sql.append(" limit ").append(this.getLimit());
            if (this.offset != null) {
                sql.append(" offset ").append(this.getOffset());
            }
        }
        return sql.toString();
    }
    
    public String paginationSql(){
        final StringBuilder sqlBuilder = new StringBuilder(50); 
        if (StringUtils.isNotBlank(this.getOrderBy())) {
            sqlBuilder.append(' ').append(this.orderBySql());
        }        
        if (this.isLimited()) {
            sqlBuilder.append(' ').append(this.limitSql());
        }
        
        return sqlBuilder.toString();
    }
}