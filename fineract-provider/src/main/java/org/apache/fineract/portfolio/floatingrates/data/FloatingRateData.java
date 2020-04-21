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
package org.apache.fineract.portfolio.floatingrates.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FloatingRateData implements Serializable {
	private Long id;
	private String name;
	@JsonProperty("is_base_lending_rate")
	private boolean baseLendingRate;
	@JsonProperty("is_active")
	private boolean active;
	private String createdBy;
	private LocalDate createdOn;
	private String modifiedBy;
	private LocalDate modifiedOn;
	private List<FloatingRatePeriodData> ratePeriods;
	private List<EnumOptionData> interestRateFrequencyTypeOptions;
}
