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
package org.apache.fineract.portfolio.interestratechart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.interestratechart.data.InterestRateChartRepositoryWrapper;
import org.apache.fineract.portfolio.interestratechart.data.InterestRateChartSlabDataValidator;
import org.apache.fineract.portfolio.interestratechart.data.InterestRateChartSlabRepository;
import org.apache.fineract.portfolio.interestratechart.domain.InterestRateChartSlab;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestRateChartSlabWritePlatformServiceJpaRepositoryImpl implements InterestRateChartSlabWritePlatformService {

    private final PlatformSecurityContext context;
    private final InterestRateChartSlabDataValidator interestRateChartSlabDataValidator;
    private final InterestRateChartAssembler interestRateChartAssembler;
    private final InterestRateChartSlabAssembler interestRateChartSlabAssembler;
    private final InterestRateChartRepositoryWrapper interestRateChartRepository;
    private final InterestRateChartSlabRepository chartSlabRepository;
    private final SavingsProductRepository savingsProductRepository;

    @Override
    @Transactional
    public CommandProcessingResult create(JsonCommand command) {
        this.interestRateChartSlabDataValidator.validateCreate(command.json());

        final InterestRateChartSlab interestRateChartSlab = this.interestRateChartSlabAssembler.assembleFrom(command);

        this.chartSlabRepository.save(interestRateChartSlab);

        final Long interestRateChartId = interestRateChartSlab.getId();

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(interestRateChartId) //
                .build();
    }

    @Override
    @Transactional
    public CommandProcessingResult update(Long chartSlabId, Long interestRateChartId, JsonCommand command) {
        this.interestRateChartSlabDataValidator.validateUpdate(command.json());
        final Map<String, Object> changes = new LinkedHashMap<>(20);
        final InterestRateChartSlab updateChartSlabs = this.interestRateChartSlabAssembler.assembleFrom(chartSlabId,
                interestRateChartId);
        final Locale locale = command.extractLocale();
        updateChartSlabs.update(command, changes,locale);

        this.chartSlabRepository.saveAndFlush(updateChartSlabs);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(interestRateChartId) //
                .changes(changes).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult deleteChartSlab(Long chartSlabId, Long interestRateChartId) {
        final InterestRateChartSlab deleteChartSlabs = this.interestRateChartSlabAssembler.assembleFrom(chartSlabId,
                interestRateChartId);
        this.chartSlabRepository.delete(deleteChartSlabs);
        return CommandProcessingResult.builder() //
                .resourceId(chartSlabId) //
                .build();
    }

}