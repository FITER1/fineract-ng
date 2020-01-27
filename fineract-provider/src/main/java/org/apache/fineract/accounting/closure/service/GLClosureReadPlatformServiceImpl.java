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
package org.apache.fineract.accounting.closure.service;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.accounting.closure.data.GLClosureData;
import org.apache.fineract.accounting.closure.domain.GLClosure;
import org.apache.fineract.accounting.closure.domain.GLClosureRepository;
import org.apache.fineract.accounting.closure.exception.GLClosureNotFoundException;
import org.joda.time.LocalDate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GLClosureReadPlatformServiceImpl implements GLClosureReadPlatformService {

    private final GLClosureRepository glClosureRepository;

    @Override
    public List<GLClosureData> retrieveAllGLClosures(final Long officeId) {
        List<GLClosure> glClosures;

        if(officeId!=null && officeId>0) {
            glClosures = glClosureRepository.findAllByOfficeIdAndDeleted(officeId, false, Sort.by(Sort.Direction.ASC, "closingDate"));
        } else {
            glClosures = glClosureRepository.findAllByDeleted(false, Sort.by(Sort.Direction.ASC, "closingDate"));
        }

        return glClosures.stream().map(glClosure -> new GLClosureData(
            glClosure.getId(),
            glClosure.getOffice().getId(),
            glClosure.getOffice().getName(),
            new LocalDate(glClosure.getClosingDate().getTime()),
            glClosure.isDeleted(),
            glClosure.getCreatedDate().isPresent() ? new LocalDate(glClosure.getCreatedDate().get().toEpochMilli()) : null,
            glClosure.getLastModifiedDate().isPresent() ? new LocalDate(glClosure.getLastModifiedDate().get().toEpochMilli()) : null,
            glClosure.getCreatedBy().isPresent() ? glClosure.getCreatedBy().get().getId() : null,
            glClosure.getCreatedBy().isPresent() ? glClosure.getCreatedBy().get().getUsername() : null,
            glClosure.getLastModifiedBy().isPresent() ? glClosure.getLastModifiedBy().get().getId() : null,
            glClosure.getLastModifiedBy().get().getUsername(),
            glClosure.getComments())).collect(Collectors.toList());
    }

    @Override
    public GLClosureData retrieveGLClosureById(final long glClosureId) {
        try {
            return glClosureRepository.findById(glClosureId).map(glClosure -> new GLClosureData(
                glClosure.getId(),
                glClosure.getOffice().getId(),
                glClosure.getOffice().getName(),
                new LocalDate(glClosure.getClosingDate().getTime()),
                glClosure.isDeleted(),
                glClosure.getCreatedDate().isPresent() ? new LocalDate(glClosure.getCreatedDate().get().toEpochMilli()) : null,
                glClosure.getLastModifiedDate().isPresent() ? new LocalDate(glClosure.getLastModifiedDate().get().toEpochMilli()) : null,
                glClosure.getCreatedBy().isPresent() ? glClosure.getCreatedBy().get().getId() : null,
                glClosure.getCreatedBy().isPresent() ? glClosure.getCreatedBy().get().getUsername() : null,
                glClosure.getLastModifiedBy().isPresent() ? glClosure.getLastModifiedBy().get().getId() : null,
                glClosure.getLastModifiedBy().isPresent() ? glClosure.getLastModifiedBy().get().getUsername() : null,
                glClosure.getComments())).orElse(null);
        } catch (final EmptyResultDataAccessException e) {
            throw new GLClosureNotFoundException(glClosureId);
        }
    }
}
