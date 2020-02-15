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
package org.apache.fineract.commands.domain;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_portfolio_command_source")
public class CommandSource extends AbstractPersistableCustom<Long> {

    @Column(name = "action_name", nullable = true, length = 100)
    private String actionName;

    @Column(name = "entity_name", nullable = true, length = 100)
    private String entityName;

    @Column(name = "office_id")
    private Long officeId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "savings_account_id")
    private Long savingsId;

    @Column(name = "api_get_url", length = 100)
    private String resourceGetUrl;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "subresource_id")
    private Long subresourceId;

    @Column(name = "command_as_json", length = 1000)
    private String commandAsJson;

    @ManyToOne
    @JoinColumn(name = "maker_id", nullable = false)
    private AppUser maker;

    @Column(name = "made_on_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date madeOnDate;

    @ManyToOne
    @JoinColumn(name = "checker_id", nullable = true)
    private AppUser checker;

    @Column(name = "checked_on_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date checkedOnDate;

    @Column(name = "processing_result_enum", nullable = false)
    private Integer processingResult;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "creditbureau_id")
    private Long creditBureauId;

    @Column(name = "organisation_creditbureau_id")
    private Long organisationCreditBureauId;

    public static CommandSource fullEntryFrom(final CommandWrapper wrapper, final JsonCommand command, final AppUser maker) {
        return CommandSource.builder()
            .actionName(wrapper.getActionName())
            .entityName(wrapper.getEntityName())
            .resourceGetUrl(wrapper.getHref())
            .resourceId(wrapper.getEntityId())
            .subresourceId(command.getSubresourceId())
            .commandAsJson(command.json())
            .maker(maker)
            .madeOnDate(DateTime.now().toDate())
            .build();
    }

    public void markAsChecked(final AppUser checker, final DateTime checkedOnDate) {
        this.checker = checker;
        this.checkedOnDate = checkedOnDate.toDate();
        this.processingResult = CommandProcessingResultType.PROCESSED.getValue();
    }

    public void markAsRejected(final AppUser checker, final DateTime checkedOnDate) {
        this.checker = checker;
        this.checkedOnDate = checkedOnDate.toDate();
        this.processingResult = CommandProcessingResultType.REJECTED.getValue();
    }

    public boolean hasJson() {
        return StringUtils.isNotBlank(this.commandAsJson);
    }

    public String getPermissionCode() {
        return this.actionName + "_" + this.entityName;
    }

    public void markAsAwaitingApproval() {
        this.processingResult = CommandProcessingResultType.AWAITING_APPROVAL.getValue();
    }

    public boolean isMarkedAsAwaitingApproval() {
        return this.processingResult.equals(CommandProcessingResultType.AWAITING_APPROVAL.getValue());
    }

    public void updateForAudit(final Long officeId, final Long groupId, final Long clientId, final Long loanId, final Long savingsId, final Long productId, final String transactionId) {
        this.officeId = officeId;
        this.groupId = groupId;
        this.clientId = clientId;
        this.loanId = loanId;
        this.savingsId = savingsId;
        this.productId = productId;
        this.transactionId = transactionId;
    }
}