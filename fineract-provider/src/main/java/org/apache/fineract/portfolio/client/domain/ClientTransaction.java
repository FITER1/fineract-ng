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
package org.apache.fineract.portfolio.client.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OrganisationCurrency;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_client_transaction", uniqueConstraints = { @UniqueConstraint(columnNames = { "external_id" }, name = "external_id") })
public class ClientTransaction extends AbstractPersistableCustom<Long> {

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @ManyToOne(optional = true)
    @JoinColumn(name = "payment_detail_id")
    private PaymentDetail paymentDetail;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "transaction_type_enum", nullable = false)
    private Integer typeOf;

    @Temporal(TemporalType.DATE)
    @Column(name = "transaction_date", nullable = false)
    private Date dateOf;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    @Column(name = "external_id", length = 100, unique = true)
    private String externalId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @ManyToOne
    @JoinColumn(name = "appuser_id")
    private AppUser appUser;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "clientTransaction", orphanRemoval = true, fetch=FetchType.EAGER)
    private Set<ClientChargePaidBy> clientChargePaidByCollection = new HashSet<>();

    @Transient
    private OrganisationCurrency currency;

    public static ClientTransaction payCharge(final Client client, final Office office, PaymentDetail paymentDetail, final LocalDate date,
            final Money amount, final String currencyCode, final AppUser appUser) {
        final boolean isReversed = false;
        final String externalId = null;

        return ClientTransaction.builder()
            .client(client)
            .office(office)
            .paymentDetail(paymentDetail)
            .typeOf(ClientTransactionType.PAY_CHARGE.getValue())
            .dateOf(date.toDate())
            .amount(amount.getAmount())
            .reversed(isReversed)
            .externalId(externalId)
            .createdDate(DateUtils.getDateOfTenant())
            .currencyCode(currencyCode)
            .appUser(appUser)
            .build();
    }

    public static ClientTransaction waiver(final Client client, final Office office, final LocalDate date, final Money amount,
            final String currencyCode, final AppUser appUser) {
        final boolean isReversed = false;
        final String externalId = null;
        final PaymentDetail paymentDetail = null;


        return ClientTransaction.builder()
            .client(client)
            .office(office)
            .paymentDetail(paymentDetail)
            .typeOf(ClientTransactionType.WAIVE_CHARGE.getValue())
            .dateOf(date.toDate())
            .amount(amount.getAmount())
            .reversed(isReversed)
            .externalId(externalId)
            .createdDate(DateUtils.getDateOfTenant())
            .currencyCode(currencyCode)
            .appUser(appUser)
            .build();
    }

    /**
     * Converts the content of this Client Transaction to a map which can be
     * passed to the accounting module
     */
    public Map<String, Object> toMapData() {
        final Map<String, Object> thisTransactionData = new LinkedHashMap<>();

        final EnumOptionData transactionType = ClientEnumerations.clientTransactionType(this.typeOf);
        Boolean accountingEnabledForAtleastOneCharge = false;

        thisTransactionData.put("id", getId());
        thisTransactionData.put("clientId", getClient().getId());
        thisTransactionData.put("officeId", this.office.getId());
        thisTransactionData.put("type", transactionType);
        thisTransactionData.put("reversed", Boolean.valueOf(this.reversed));
        thisTransactionData.put("date", getTransactionDate());
        thisTransactionData.put("currencyCode", this.currencyCode);
        thisTransactionData.put("amount", this.amount);

        if (this.paymentDetail != null) {
            thisTransactionData.put("paymentTypeId", this.paymentDetail.getPaymentType().getId());
        }

        if (!this.clientChargePaidByCollection.isEmpty()) {
            final List<Map<String, Object>> clientChargesPaidData = new ArrayList<>();
            for (final ClientChargePaidBy clientChargePaidBy : this.clientChargePaidByCollection) {
                final Map<String, Object> clientChargePaidData = new LinkedHashMap<>();
                clientChargePaidData.put("chargeId", clientChargePaidBy.getClientCharge().getCharge().getId());
                clientChargePaidData.put("isPenalty", clientChargePaidBy.getClientCharge().getCharge().isPenalty());
                clientChargePaidData.put("clientChargeId", clientChargePaidBy.getClientCharge().getId());
                clientChargePaidData.put("amount", clientChargePaidBy.getAmount());
                GLAccount glAccount = clientChargePaidBy.getClientCharge().getCharge().getAccount();
                if (glAccount != null) {
                    accountingEnabledForAtleastOneCharge = true;
                    clientChargePaidData.put("incomeAccountId", glAccount.getId());
                }
                clientChargesPaidData.add(clientChargePaidData);
            }
            thisTransactionData.put("clientChargesPaid", clientChargesPaidData);
        }

        thisTransactionData.put("accountingEnabled", accountingEnabledForAtleastOneCharge);

        return thisTransactionData;
    }

    public boolean isPayChargeTransaction() {
        return ClientTransactionType.PAY_CHARGE.getValue().equals(this.typeOf);
    }

    public boolean isWaiveChargeTransaction() {
        return ClientTransactionType.WAIVE_CHARGE.getValue().equals(this.typeOf);
    }

    public Money getAmount() {
        return Money.of(MonetaryCurrency.builder()
            .code(this.currency.getCode())
            .digitsAfterDecimal(this.currency.getDecimalPlaces())
            .inMultiplesOf(this.currency.getInMultiplesOf())
            .build(), this.amount);
    }

    public LocalDate getTransactionDate() {
        return new LocalDate(this.dateOf);
    }
}
