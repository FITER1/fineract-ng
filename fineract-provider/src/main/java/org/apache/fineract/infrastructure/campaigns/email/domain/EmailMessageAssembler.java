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

import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.campaigns.email.EmailApiConstants;
import org.apache.fineract.infrastructure.campaigns.email.exception.EmailNotFoundException;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepositoryWrapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailMessageAssembler {

    private final EmailMessageRepository emailMessageRepository;
    private final GroupRepositoryWrapper groupRepository;
    private final ClientRepositoryWrapper clientRepository;
    private final StaffRepositoryWrapper staffRepository;
    private final FromJsonHelper fromApiJsonHelper;

    public EmailMessage assembleFromJson(final JsonCommand command) {

        final JsonElement element = command.parsedJson();

        String emailAddress = null;

        Group group = null;
        if (this.fromApiJsonHelper.parameterExists(EmailApiConstants.groupIdParamName, element)) {
            final Long groupId = this.fromApiJsonHelper.extractLongNamed(EmailApiConstants.groupIdParamName, element);
            group = this.groupRepository.findOneWithNotFoundDetection(groupId);
        }

        Client client = null;
        if (this.fromApiJsonHelper.parameterExists(EmailApiConstants.clientIdParamName, element)) {
            final Long clientId = this.fromApiJsonHelper.extractLongNamed(EmailApiConstants.clientIdParamName, element);
            client = this.clientRepository.findOneWithNotFoundDetection(clientId);
            emailAddress = client.getEmailAddress();
        }

        Staff staff = null;
        if (this.fromApiJsonHelper.parameterExists(EmailApiConstants.staffIdParamName, element)) {
            final Long staffId = this.fromApiJsonHelper.extractLongNamed(EmailApiConstants.staffIdParamName, element);
            staff = this.staffRepository.findOneWithNotFoundDetection(staffId);
            emailAddress = staff.getEmailAddress();
        }

        final String message = this.fromApiJsonHelper.extractStringNamed(EmailApiConstants.messageParamName, element);
        final String emailSubject = this.fromApiJsonHelper.extractStringNamed(EmailApiConstants.subjectParamName, element);

        return EmailMessage.builder()
            .statusType(EmailMessageStatusType.PENDING.getValue())
            .group(group)
            .client(client)
            .staff(staff)
            .emailSubject(emailSubject)
            .message(message)
            .emailAddress(emailAddress)
            .build();
    }

    public EmailMessage assembleFromResourceId(final Long resourceId) {
        return this.emailMessageRepository.findById(resourceId)
                .orElseThrow(() -> new EmailNotFoundException(resourceId));
    }
}