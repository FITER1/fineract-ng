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
package org.apache.fineract.infrastructure.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.AbstractAuditable;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

// TODO: @aleks fix this (check for default implementations with Temporal/Instant etc.); replace Joda with java.time.*

/**
 * A custom copy of {@link AbstractAuditable} to override the column names used
 * on database.
 * 
 * Abstract base class for auditable entities. Stores the audition values in
 * persistent fields.
 * 
 * @param <U>
 *            the auditing type. Typically some kind of user.
 * @param <PK>
 *            the type of the auditing type's identifier
 */
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractAuditableCustom<U, PK extends Serializable> extends AbstractPersistableCustom<PK> implements Auditable<AppUser, Long, Instant> {

    private static final long serialVersionUID = 141481953116476081L;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "createdby_id")
    @CreatedBy
    private AppUser createdBy;

    @Column(name = "created_date")
    // @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    private Date createdDate;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "lastmodifiedby_id")
    @LastModifiedBy
    private AppUser lastModifiedBy;

    @Column(name = "lastmodified_date")
    // @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    private Date lastModifiedDate;

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.domain.Auditable#getCreatedBy()
     */
    @Override
    public Optional<AppUser> getCreatedBy() {
        return Optional.ofNullable(this.createdBy);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.domain.Auditable#getCreatedDate()
     */
    @Override
    public Optional<Instant> getCreatedDate() {
        return null == this.createdDate ? Optional.empty() : Optional.of(this.createdDate.toInstant());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.data.domain.Auditable#setCreatedDate(T)
     */
    @Override
    public void setCreatedDate(final Instant createdDate) {
        this.createdDate = null == createdDate ? null : Date.from(createdDate);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.domain.Auditable#getLastModifiedBy()
     */
    @Override
    public Optional<AppUser> getLastModifiedBy() {
        return Optional.ofNullable(this.lastModifiedBy);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.domain.Auditable#getLastModifiedDate()
     */
    @Override
    public Optional<Instant> getLastModifiedDate() {
        return null == this.lastModifiedDate ? Optional.empty() : Optional.of(this.lastModifiedDate.toInstant());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.data.domain.Auditable#setLastModifiedDate(T)
     */
    @Override
    public void setLastModifiedDate(final Instant lastModifiedDate) {
        this.lastModifiedDate = null == lastModifiedDate ? null : Date.from(lastModifiedDate);
    }
}
