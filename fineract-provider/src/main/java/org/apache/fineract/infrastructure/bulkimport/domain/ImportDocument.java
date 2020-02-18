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
package org.apache.fineract.infrastructure.bulkimport.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.documentmanagement.domain.Document;
import org.apache.fineract.useradministration.domain.AppUser;

import javax.persistence.*;
import java.util.Date;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_import_document")
public class ImportDocument extends AbstractPersistableCustom<Long>{

    @OneToOne
    @JoinColumn(name = "document_id")
    private Document document;

    // @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "import_time")
    private Date importTime;

    // @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_time")
    private Date endTime;

    @Column(name = "completed", nullable = false)
    private Boolean completed;

    @Column(name = "entity_type")
    private Integer entityType;

    @ManyToOne
    @JoinColumn(name = "createdby_id")
    private AppUser createdBy;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "failure_count")
    private Integer failureCount;
}