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
package org.apache.fineract.infrastructure.core.domain;

import lombok.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.Days;
import org.joda.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LocalDateInterval {
    private LocalDate startDate;
    private LocalDate endDate;

    public Integer daysInPeriodInclusiveOfEndDate() {
        return Days.daysBetween(this.startDate, this.endDate).getDays() + 1;
    }

    public boolean containsPortionOf(final LocalDateInterval interval) {
        return contains(interval.startDate) || contains(interval.endDate);
    }

    public boolean contains(final LocalDateInterval interval) {
        return contains(interval.startDate) && contains(interval.endDate);
    }

    public boolean contains(final LocalDate target) {
        return isBetweenInclusive(this.startDate, this.endDate, target);
    }

    private boolean isBetweenInclusive(final LocalDate start, final LocalDate end, final LocalDate target) {
        return !target.isBefore(start) && !target.isAfter(end);
    }

    public boolean fallsBefore(final LocalDate dateToCheck) {
        return this.endDate.isBefore(dateToCheck);
    }
}