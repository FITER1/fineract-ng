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
package org.apache.fineract.portfolio.floatingrates.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.floatingrates.data.FloatingRateDTO;
import org.apache.fineract.portfolio.floatingrates.data.FloatingRatePeriodData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@SuperBuilder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_floating_rates_periods")
public class FloatingRatePeriod extends AbstractPersistableCustom<Long> {

	@ManyToOne
	@JoinColumn(name = "floating_rates_id", nullable = false)
	private FloatingRate floatingRate;

	@Column(name = "from_date", nullable = false)
	private Date fromDate;

	@Column(name = "interest_rate", scale = 6, precision = 19, nullable = false)
	private BigDecimal interestRate;

	@Column(name = "is_differential_to_base_lending_rate", nullable = false)
	private boolean differentialToBaseLendingRate;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@ManyToOne(optional = true, fetch=FetchType.LAZY)
	@JoinColumn(name = "createdby_id", nullable = false)
	private AppUser createdBy;

	@ManyToOne(optional = true, fetch=FetchType.LAZY)
	@JoinColumn(name = "lastmodifiedby_id", nullable = false)
	private AppUser modifiedBy;

	@Column(name = "created_date", nullable = false)
	private Date createdOn;

	@Column(name = "lastmodified_date", nullable = false)
	private Date modifiedOn;

	public FloatingRatePeriodData toData(final FloatingRateDTO floatingRateDTO) {
		BigDecimal interest = this.getInterestRate().add(floatingRateDTO.getInterestRateDiff());

		if (this.isDifferentialToBaseLendingRate()) {
			interest = interest.add(floatingRateDTO.getBaseRate(LocalDate.fromDateFields(getFromDate())));
		}
		
		return FloatingRatePeriodData.builder()
			.id(this.getId())
			.fromDate(LocalDate.fromDateFields(this.getFromDate()))
			.createdOn(LocalDate.fromDateFields(this.getCreatedOn()))
			.modifiedOn(LocalDate.fromDateFields(this.getModifiedOn()))
			.differentialToBaseLendingRate(this.isDifferentialToBaseLendingRate())
			.active(this.isActive())
			.createdBy(this.getCreatedBy().getUsername())
			.modifiedBy(this.getModifiedBy().getUsername())
			.interestRate(interest)
			.build();
	}

}
