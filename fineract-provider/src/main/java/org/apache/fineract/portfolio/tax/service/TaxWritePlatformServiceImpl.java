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
package org.apache.fineract.portfolio.tax.service;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.tax.domain.*;
import org.apache.fineract.portfolio.tax.serialization.TaxValidator;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaxWritePlatformServiceImpl implements TaxWritePlatformService {

    private final TaxValidator validator;
    private final TaxAssembler taxAssembler;
    private final TaxComponentRepository taxComponentRepository;
    private final TaxComponentRepositoryWrapper taxComponentRepositoryWrapper;
    private final TaxGroupRepository taxGroupRepository;
    private final TaxGroupRepositoryWrapper taxGroupRepositoryWrapper;

    @Override
    public CommandProcessingResult createTaxComponent(final JsonCommand command) {
        this.validator.validateForTaxComponentCreate(command.json());
        TaxComponent taxComponent = this.taxAssembler.assembleTaxComponentFrom(command);
        this.taxComponentRepository.save(taxComponent);
        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(taxComponent.getId()) //
                .build();
    }

    @Override
    public CommandProcessingResult updateTaxComponent(final Long id, final JsonCommand command) {
        this.validator.validateForTaxComponentUpdate(command.json());
        final TaxComponent taxComponent = this.taxComponentRepositoryWrapper.findOneWithNotFoundDetection(id);
        this.validator.validateStartDate(taxComponent.startDate(), command);
        Map<String, Object> changes = taxComponent.update(command);
        this.validator.validateTaxComponentForUpdate(taxComponent);
        this.taxComponentRepository.save(taxComponent);
        return CommandProcessingResult.builder() //
                .resourceId(id) //
                .changes(changes).build();
    }

    @Override
    public CommandProcessingResult createTaxGroup(final JsonCommand command) {
        this.validator.validateForTaxGroupCreate(command.json());
        final TaxGroup taxGroup = this.taxAssembler.assembleTaxGroupFrom(command);
        this.validator.validateTaxGroup(taxGroup);
        this.taxGroupRepository.save(taxGroup);
        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(taxGroup.getId()) //
                .build();
    }

    @Override
    public CommandProcessingResult updateTaxGroup(final Long id, final JsonCommand command) {
        this.validator.validateForTaxGroupUpdate(command.json());
        final TaxGroup taxGroup = this.taxGroupRepositoryWrapper.findOneWithNotFoundDetection(id);
        final boolean isUpdate = true;
        Set<TaxGroupMappings> groupMappings = this.taxAssembler.assembleTaxGroupMappingsFrom(command, isUpdate);
        this.validator.validateTaxGroupEndDateAndTaxComponent(taxGroup, groupMappings);
        Map<String, Object> changes = taxGroup.update(command, groupMappings);
        this.validator.validateTaxGroup(taxGroup);
        this.taxGroupRepository.save(taxGroup);
        return CommandProcessingResult.builder() //
                .resourceId(id) //
                .changes(changes).build();
    }

}
