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
package org.apache.fineract.infrastructure.documentmanagement.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.documentmanagement.command.DocumentCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "m_document")
public class Document extends AbstractPersistableCustom<Long> {

    @Column(name = "parent_entity_type", length = 50)
    private String parentEntityType;

    @Column(name = "parent_entity_id", length = 1000)
    private Long parentEntityId;

    @Column(name = "name", length = 250)
    private String name;

    @Column(name = "file_name", length = 250)
    private String fileName;

    @Column(name = "size")
    private Long size;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "storage_type_enum")
    private Integer storageType;

    public static Document createNew(final String parentEntityType, final Long parentEntityId, final String name, final String fileName,
            final Long size, final String type, final String description, final String location, final StorageType storageType) {
        return Document.builder()
            .parentEntityId(parentEntityId)
            .parentEntityType(parentEntityType)
            .name(name)
            .fileName(fileName)
            .size(size)
            .type(type)
            .description(description)
            .location(location)
            .storageType(storageType.getValue())
            .build();
    }

    public void update(final DocumentCommand command) {
        if (command.isDescriptionChanged()) {
            this.description = command.getDescription();
        }
        if (command.isFileNameChanged()) {
            this.fileName = command.getFileName();
        }
        if (command.isFileTypeChanged()) {
            this.type = command.getType();
        }
        if (command.isLocationChanged()) {
            this.location = command.getLocation();
        }
        if (command.isNameChanged()) {
            this.name = command.getName();
        }
        if (command.isSizeChanged()) {
            this.size = command.getSize();
        }
    }

    public StorageType storageType() {
        return StorageType.fromInt(this.storageType);
    }
}