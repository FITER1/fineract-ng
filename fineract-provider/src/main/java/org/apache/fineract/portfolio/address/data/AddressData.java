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
package org.apache.fineract.portfolio.address.data;

import lombok.*;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AddressData {
	private Long clientId;
	private String addressType;
	private Long addressId;
	private Long addressTypeId;
	private boolean active;
	private String street;
	private String addressLine1;
	private String addressLine2;
	private String addressLine3;
	private String townVillage;
	private String city;
	private String countyDistrict;
	private Long stateProvinceId;
	private String countryName;
	private String stateName;
	private Long countryId;
	private String postalCode;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private String createdBy;
	private Date createdOn;
	private String updatedBy;
	private Date updatedOn;
	// template holder
	private Collection<CodeValueData> countryIdOptions;
	private Collection<CodeValueData> stateProvinceIdOptions;
	private Collection<CodeValueData> addressTypeIdOptions;
}
