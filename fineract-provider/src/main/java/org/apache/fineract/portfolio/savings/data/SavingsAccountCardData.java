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

package org.apache.fineract.portfolio.savings.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountCard;
import org.joda.time.LocalDate;

@Data
@NoArgsConstructor
public class SavingsAccountCardData {

    public SavingsAccountCardData(SavingsAccountCard card) {
        this.applicationId = card.getApplicationId();
        this.applicationFlowId = card.getApplicationFlowId();
        this.applicationStatus = card.getApplicationStatus();
        this.cardNumber = card.getCardNumber();
        this.cardholderName = card.getCardholderName();
        this.cardType = card.getCardType();
        this.dateCreated = LocalDate.fromDateFields(card.getDateCreated());
        if (card.getExpiryDate() != null) {
            this.expiryDate = LocalDate.fromDateFields(card.getExpiryDate());
        }
        if (card.getLastUpdated() != null) {
            this.lastUpdated = LocalDate.fromDateFields(card.getLastUpdated());
        }
    }
    private String applicationId;
    private Long applicationFlowId;
    private String applicationStatus;
    private String cardNumber;
    private String cardholderName;
    private String cardType;
    private LocalDate expiryDate;
    private LocalDate dateCreated;
    private LocalDate lastUpdated;
}
