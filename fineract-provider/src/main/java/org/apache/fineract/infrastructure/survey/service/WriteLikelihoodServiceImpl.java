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
package org.apache.fineract.infrastructure.survey.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.survey.data.LikelihoodDataValidator;
import org.apache.fineract.infrastructure.survey.data.LikelihoodStatus;
import org.apache.fineract.infrastructure.survey.domain.Likelihood;
import org.apache.fineract.infrastructure.survey.domain.LikelihoodRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Cieyou on 3/12/14.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WriteLikelihoodServiceImpl implements WriteLikelihoodService {

    private final PlatformSecurityContext context;
    private final LikelihoodDataValidator likelihoodDataValidator;
    private final LikelihoodRepository repository;

    @Override
    public CommandProcessingResult update(Long likelihoodId, JsonCommand command) {

        this.context.authenticatedUser();

        try {

            this.likelihoodDataValidator.validateForUpdate(command);

            final Likelihood likelihood = this.repository.findById(likelihoodId).orElse(null);

            if (!likelihood.update(command).isEmpty()) {
                this.repository.save(likelihood);

                if (likelihood.isActivateCommand(command)) {
                    List<Likelihood> likelihoods = this.repository
                            .findByPpiNameAndLikeliHoodId(likelihood.getPpiName(), likelihood.getId());

                    for (Likelihood aLikelihood : likelihoods) {
                        aLikelihood.setEnabled(LikelihoodStatus.DISABLED);
                    }
                    this.repository.saveAll(likelihoods);
                }

            }

            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(likelihood.getId()).build();

        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(dve);
            return new CommandProcessingResult();
        }

    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final DataIntegrityViolationException dve) {

        final Throwable realCause = dve.getMostSpecificCause();
        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.likelihood.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}
