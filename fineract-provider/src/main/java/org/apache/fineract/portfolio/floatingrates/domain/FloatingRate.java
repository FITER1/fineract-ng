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

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@SuperBuilder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_floating_rates", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }, name = "unq_name") })
public class FloatingRate extends AbstractPersistableCustom<Long> {

	@Column(name = "name", length = 200, unique = true, nullable = false)
	private String name;

	@Column(name = "is_base_lending_rate", nullable = false)
	private boolean baseLendingRate;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@OrderBy(value = "fromDate,id")
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "floatingRate", orphanRemoval = true, fetch=FetchType.EAGER)
	private List<FloatingRatePeriod> floatingRatePeriods;

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

	@JsonIgnore
	public Collection<FloatingRatePeriodData> getInterestRates(final FloatingRateDTO floatingRateDTO) {
		Collection<FloatingRatePeriodData> applicableRates = new ArrayList<>();
		FloatingRatePeriod previousPeriod = null;
		boolean addPeriodData = false;
		for (FloatingRatePeriod floatingRatePeriod : this.floatingRatePeriods) {
			if (floatingRatePeriod.isActive()) {
				// will enter
				if (applicableRates.isEmpty() && floatingRateDTO.getStartDate().isBefore(LocalDate.fromDateFields(floatingRatePeriod.getFromDate()))) {
					if (floatingRateDTO.isFloatingInterestRate()) {
						addPeriodData = true;
					}
					if (previousPeriod != null) {
						applicableRates.add(previousPeriod
								.toData(floatingRateDTO));
					} else if (!addPeriodData) {
						applicableRates.add(floatingRatePeriod.toData(floatingRateDTO));
					}
				}
				if (addPeriodData) {
					applicableRates.add(floatingRatePeriod.toData(floatingRateDTO));
				}
				previousPeriod = floatingRatePeriod;
			}
		}
		if (applicableRates.isEmpty() && previousPeriod != null) {
			applicableRates.add(previousPeriod.toData(floatingRateDTO));
		}
		return applicableRates;
	}
}
