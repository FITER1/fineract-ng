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
package org.apache.fineract.portfolio.interestratechart.domain;

import lombok.*;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.portfolio.common.domain.ConditionType;
import org.apache.fineract.portfolio.interestratechart.incentive.InterestIncentiveAttributeName;
import org.apache.fineract.portfolio.interestratechart.incentive.InterestIncentiveEntityType;
import org.apache.fineract.portfolio.interestratechart.incentive.InterestIncentiveType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.math.BigDecimal;

import static org.apache.fineract.portfolio.interestratechart.InterestIncentiveApiConstants.attributeValueParamName;
import static org.apache.fineract.portfolio.interestratechart.InterestIncentiveApiConstants.conditionTypeParamName;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class InterestIncentivesFields {

    @Column(name = "entiry_type", nullable = false)
    private Integer entityType;

    @Column(name = "attribute_name", nullable = false)
    private Integer attributeName;

    @Column(name = "condition_type", nullable = false)
    private Integer conditionType;

    @Column(name = "attribute_value", nullable = false)
    private String attributeValue;

    @Column(name = "incentive_type", nullable = false)
    private Integer incentiveType;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    public InterestIncentiveAttributeName getAttributeNameEnum() {
        return InterestIncentiveAttributeName.fromInt(this.attributeName);
    }

    public ConditionType getConditionTypeEnum() {
        return ConditionType.fromInt(this.conditionType);
    }

    public InterestIncentiveType getIncentiveTypeEnum() {
        return InterestIncentiveType.fromInt(this.incentiveType);
    }

    public InterestIncentiveEntityType getEntiryTypeEnum() {
        return InterestIncentiveEntityType.fromInt(this.entityType);
    }

    // TODO: @Aleks stuff like this should be in a separate validator class
    public void validateIncentiveData(final DataValidatorBuilder baseDataValidator) {

        switch (getAttributeNameEnum()) {
            case GENDER:
            case CLIENT_CLASSIFICATION:
            case CLIENT_TYPE:
                baseDataValidator.reset().parameter(attributeValueParamName).value(this.attributeValue).longGreaterThanZero();
                baseDataValidator.reset().parameter(conditionTypeParamName).value(this.conditionType)
                        .isOneOfTheseValues(ConditionType.EQUAL.getValue(), ConditionType.NOT_EQUAL.getValue());
                break;
            case AGE:
                baseDataValidator.reset().parameter(attributeValueParamName).value(this.attributeValue).longGreaterThanZero();
                break;
            default:
            break;
        }
    }
}
