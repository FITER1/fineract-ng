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
package org.apache.fineract.infrastructure.hooks.data;

import lombok.*;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HookData implements Serializable {
    private Long id;
    private String name;
    private String displayName;
    private Boolean active;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private Long templateId;
    private String templateName;
    // associations
    private List<Event> events;
    private List<Field> config;
    // template data
    private List<HookTemplateData> templates;
    private List<Grouping> groupings;
}
