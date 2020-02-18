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

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Persistable;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;


@SuperBuilder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@MappedSuperclass
public abstract class AbstractPersistableCustom<PK extends Serializable> implements Persistable<Long> {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        protected Long id;

        /*
         * (non-Javadoc)
         * 
         * @see org.springframework.data.domain.Persistable#isNew()
         */
        @Override
        public boolean isNew() {

                return null == this.id;
        }
}
