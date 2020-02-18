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
package org.apache.fineract.infrastructure.campaigns.email.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.campaigns.email.EmailApiConstants;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.group.domain.Group;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "scheduled_email_messages_outbound")
public class EmailMessage extends AbstractPersistableCustom<Long> {

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "email_campaign_id")
    private EmailCampaign emailCampaign;

    @Column(name = "status_enum", nullable = false)
    private Integer statusType;

    @Column(name = "email_address", nullable = false, length = 50)
    private String emailAddress;

    @Column(name = "email_subject", nullable = false, length = 50)
    private String emailSubject;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "campaign_name")
    private String campaignName;

    @Column(name = "submittedon_date")
    @Temporal(TemporalType.DATE)
    private Date submittedOnDate;

    @Column(name = "error_message")
    private String errorMessage;

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(1);

        if (command.isChangeInStringParameterNamed(EmailApiConstants.messageParamName, this.message)) {
            final String newValue = command.stringValueOfParameterNamed(EmailApiConstants.messageParamName);
            actualChanges.put(EmailApiConstants.messageParamName, newValue);
            this.message = StringUtils.defaultIfEmpty(newValue, null);
        }

        return actualChanges;
    }

    public boolean isPending() {
        return EmailMessageStatusType.fromInt(this.statusType).isPending();
    }

    public boolean isSent() {
        return EmailMessageStatusType.fromInt(this.statusType).isSent();
    }
}