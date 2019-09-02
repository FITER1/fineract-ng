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

import org.apache.fineract.useradministration.domain.AppUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.AbstractAuditable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

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
@MappedSuperclass
public abstract class AbstractAuditableCustom<U, PK extends Serializable> extends AbstractPersistableCustom<PK> /* implements Auditable<AppUser, Long, Instant> */ {

    private static final long serialVersionUID = 141481953116476081L;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "createdby_id")
    @CreatedBy
    private AppUser createdBy;

    @Column(name = "created_date")
    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    private Date createdDate;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "lastmodifiedby_id")
    @LastModifiedBy
    private AppUser lastModifiedBy;

    @Column(name = "lastmodified_date")
    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    private Date lastModifiedDate;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.domain.Auditable#getCreatedBy()
     */
    // @Override
    public AppUser getCreatedBy() {

        return this.createdBy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.domain.Auditable#setCreatedBy(java.lang.Object)
     */
    // @Override
    public void setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.domain.Auditable#getCreatedDate()
     */
    @NotNull
    // @Override
    public Date getCreatedDate() {
        return this.createdDate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.domain.Auditable#setCreatedDate(org.joda.time
     * .DateTime)
     */
    // @Override
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.domain.Auditable#getLastModifiedBy()
     */
    // @Override
    public AppUser getLastModifiedBy() {

        return this.lastModifiedBy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.domain.Auditable#setLastModifiedBy(java.lang
     * .Object)
     */
    // @Override
    public void setLastModifiedBy(final AppUser lastModifiedBy) {

        this.lastModifiedBy = lastModifiedBy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.domain.Auditable#getLastModifiedDate()
     */
    // @Override
    public Date getLastModifiedDate() {

        return this.lastModifiedDate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.domain.Auditable#setLastModifiedDate(org.joda
     * .time.DateTime)
     */
    // @Override
    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
