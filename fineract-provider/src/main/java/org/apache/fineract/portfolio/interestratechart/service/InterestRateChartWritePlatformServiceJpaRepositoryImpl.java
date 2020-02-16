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
import org.apache.fineract.portfolio.interestratechart.data.InterestRateChartDataValidator;
import org.apache.fineract.portfolio.interestratechart.data.InterestRateChartRepositoryWrapper;
import org.apache.fineract.portfolio.interestratechart.domain.InterestRateChart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestRateChartWritePlatformServiceJpaRepositoryImpl implements InterestRateChartWritePlatformService {

    private final PlatformSecurityContext context;
    private final InterestRateChartDataValidator interestRateChartDataValidator;
    private final InterestRateChartAssembler interestRateChartAssembler;
    private final InterestRateChartRepositoryWrapper interestRateChartRepository;

    @Override
    @Transactional
    public CommandProcessingResult create(JsonCommand command) {
        this.interestRateChartDataValidator.validateForCreate(command.json());

        final InterestRateChart interestRateChart = this.interestRateChartAssembler.assembleFrom(command);

        this.interestRateChartRepository.saveAndFlush(interestRateChart);

        final Long interestRateChartId = interestRateChart.getId();

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(interestRateChartId) //
                .build();
    }

    @Override
    @Transactional
    public CommandProcessingResult update(Long interestRateChartId, JsonCommand command) {
        this.interestRateChartDataValidator.validateUpdate(command.json());
        final Map<String, Object> changes = new LinkedHashMap<>(20);
        final InterestRateChart interestRateChart = this.interestRateChartAssembler.assembleFrom(interestRateChartId);

        interestRateChart.update(command, changes);

        this.interestRateChartRepository.saveAndFlush(interestRateChart);

        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(interestRateChartId) //
                .changes(changes).build();
    }

    @Override
    @Transactional
    public CommandProcessingResult deleteChart(Long chartId) {
        final InterestRateChart chart = this.interestRateChartRepository.findOneWithNotFoundDetection(chartId);
        // validate if chart is associated with any accounts

        this.interestRateChartRepository.delete(chart);
        return CommandProcessingResult.builder() //
                .resourceId(chartId) //
                .build();
    }

}