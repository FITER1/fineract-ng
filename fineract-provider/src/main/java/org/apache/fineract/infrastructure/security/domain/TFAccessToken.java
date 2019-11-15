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
package org.apache.fineract.infrastructure.security.domain;

import lombok.*;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.data.AccessTokenData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import javax.persistence.*;
import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "twofactor_access_token",
        uniqueConstraints = {@UniqueConstraint(columnNames = { "token", "appuser_id" }, name = "token_appuser_UNIQUE")})
public class TFAccessToken extends AbstractPersistableCustom<Long> {

    @Column(name = "token", nullable = false, length = 32)
    private String token;

    @ManyToOne
    @JoinColumn(name = "appuser_id", nullable = false)
    private AppUser user;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "valid_from", nullable = false)
    private Date validFrom;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "valid_to", nullable = false)
    private Date validTo;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public static TFAccessToken create(String token, AppUser user, int tokenLiveTimeInSec) {
        DateTime validFrom = DateUtils.getLocalDateTimeOfTenant().toDateTime();
        DateTime validTo = validFrom.plusSeconds(tokenLiveTimeInSec);

        return TFAccessToken.builder()
            .token(token)
            .user(user)
            .validFrom(validFrom.toDate())
            .validTo(validTo.toDate())
            .enabled(true)
            .build();
    }

    public boolean isValid() {
        return this.enabled && isDateInTheFuture(getValidToDate()) && isDateInThePast(getValidFromDate());
    }

    public AccessTokenData toTokenData() {
        return new AccessTokenData(this.token, getValidFromDate().toDateTime(), getValidToDate().toDateTime());
    }

    public LocalDateTime getValidFromDate() {
        return new LocalDateTime(validFrom);
    }

    public LocalDateTime getValidToDate() {
        return new LocalDateTime(validTo);
    }

    private boolean isDateInTheFuture(LocalDateTime dateTime) {
        return dateTime.isAfter(DateUtils.getLocalDateTimeOfTenant());
    }

    private boolean isDateInThePast(LocalDateTime dateTime) {
        return dateTime.isBefore(DateUtils.getLocalDateTimeOfTenant());
    }
}
