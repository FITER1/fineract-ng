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
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.office.domain.OrganisationCurrency;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.joda.time.LocalDate;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_client_charge")
public class ClientCharge extends AbstractPersistableCustom<Long> {

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "charge_id", referencedColumnName = "id", nullable = false)
    private Charge charge;

    @Column(name = "charge_time_enum", nullable = false)
    private Integer chargeTime;

    // @Temporal(TemporalType.DATE)
    @Column(name = "charge_due_date")
    private Date dueDate;

    @Column(name = "charge_calculation_enum")
    private Integer chargeCalculation;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_paid_derived", scale = 6, precision = 19)
    private BigDecimal amountPaid;

    @Column(name = "amount_waived_derived", scale = 6, precision = 19)
    private BigDecimal amountWaived;

    @Column(name = "amount_writtenoff_derived", scale = 6, precision = 19)
    private BigDecimal amountWrittenOff;

    @Column(name = "amount_outstanding_derived", scale = 6, precision = 19, nullable = false)
    private BigDecimal amountOutstanding;

    @Column(name = "is_penalty", nullable = false)
    private boolean penaltyCharge;

    @Column(name = "is_paid_derived", nullable = false)
    private boolean paid;

    @Column(name = "waived", nullable = false)
    private boolean waived;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean status = true;

    // @Temporal(TemporalType.DATE)
    @Column(name = "inactivated_on_date")
    private Date inactivationDate;

    @Transient
    private OrganisationCurrency currency;

    public Money pay(final Money amountPaid) {
        Money amountPaidToDate = Money.of(this.getCurrency(), this.amountPaid);
        Money amountOutstanding = Money.of(this.getCurrency(), this.amountOutstanding);
        amountPaidToDate = amountPaidToDate.plus(amountPaid);
        amountOutstanding = amountOutstanding.minus(amountPaid);
        this.amountPaid = amountPaidToDate.getAmount();
        this.amountOutstanding = amountOutstanding.getAmount();
        this.paid = BigDecimal.ZERO.equals(calculateOutstanding());
        return Money.of(this.getCurrency(), this.amountOutstanding);
    }

    public void undoPayment(final Money transactionAmount) {
        Money amountPaid = getAmountPaid();
        amountPaid = amountPaid.minus(transactionAmount);
        this.amountPaid = amountPaid.getAmount();
        this.amountOutstanding = calculateOutstanding();
        this.paid = false;
        this.status = true;
    }

    public Money waive() {
        Money amountWaivedToDate = getAmountWaived();
        Money amountOutstanding = getAmountOutstanding();
        Money totalAmountWaived = amountWaivedToDate.plus(amountOutstanding);
        this.amountWaived = totalAmountWaived.getAmount();
        this.amountOutstanding = BigDecimal.ZERO;
        this.waived = true;
        return totalAmountWaived;
    }

    public void undoWaiver(final Money transactionAmount) {
        Money amountWaived = getAmountWaived();
        amountWaived = amountWaived.minus(transactionAmount);
        this.amountWaived = amountWaived.getAmount();
        this.amountOutstanding = calculateOutstanding();
        this.waived = false;
        this.status = true;
    }

    public BigDecimal calculateOutstanding() {
        BigDecimal amountPaidLocal = BigDecimal.ZERO;
        if (this.amountPaid != null) {
            amountPaidLocal = this.amountPaid;
        }

        BigDecimal amountWaivedLocal = BigDecimal.ZERO;
        if (this.amountWaived != null) {
            amountWaivedLocal = this.amountWaived;
        }

        BigDecimal amountWrittenOffLocal = BigDecimal.ZERO;
        if (this.amountWrittenOff != null) {
            amountWrittenOffLocal = this.amountWrittenOff;
        }

        final BigDecimal totalAccountedFor = amountPaidLocal.add(amountWaivedLocal).add(amountWrittenOffLocal);

        return this.amount.subtract(totalAccountedFor);
    }

    public LocalDate getDueLocalDate() {
        LocalDate dueDate = null;
        if (this.dueDate != null) {
            dueDate = new LocalDate(this.dueDate);
        }
        return dueDate;
    }

    public Long getClientId() {
        return client.getId();
    }

    public Long getOfficeId() {
        return this.client.getOffice().getId();
    }

    public MonetaryCurrency getCurrency() {
        return MonetaryCurrency.builder()
            .code(this.currency.getCode())
            .digitsAfterDecimal(this.currency.getDecimalPlaces())
            .inMultiplesOf(this.currency.getInMultiplesOf())
            .build();
    }

    public Money getAmount() {
        return Money.of(getCurrency(), this.amount);
    }

    public Money getAmountPaid() {
        return Money.of(getCurrency(), this.amountPaid);
    }

    public Money getAmountWaived() {
        return Money.of(getCurrency(), this.amountWaived);
    }

    public Money getAmountWrittenOff() {
        return Money.of(getCurrency(), this.amountWrittenOff);
    }

    public Money getAmountOutstanding() {
        return Money.of(getCurrency(), this.amountOutstanding);
    }
}
