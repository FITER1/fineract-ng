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
package org.apache.fineract.infrastructure.jobs.service;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.jobs.data.JobDetailDataValidator;
import org.apache.fineract.infrastructure.jobs.domain.*;
import org.apache.fineract.infrastructure.jobs.exception.JobNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchedularWritePlatformServiceJpaRepositoryImpl implements SchedularWritePlatformService {

    private final ScheduledJobDetailRepository scheduledJobDetailsRepository;

    private final ScheduledJobRunHistoryRepository scheduledJobRunHistoryRepository;

    private final SchedulerDetailRepository schedulerDetailRepository;

    private final JobDetailDataValidator dataValidator;

    @Override
    public List<ScheduledJobDetail> retrieveAllJobs() {
        return this.scheduledJobDetailsRepository.findAll();
    }

    @Override
    public ScheduledJobDetail findByJobKey(final String jobKey) {
        return this.scheduledJobDetailsRepository.findByJobKey(jobKey);
    }

    @Transactional
    @Override
    public void saveOrUpdate(final ScheduledJobDetail scheduledJobDetails) {
        this.scheduledJobDetailsRepository.save(scheduledJobDetails);
    }

    @Transactional
    @Override
    public void saveOrUpdate(final ScheduledJobDetail scheduledJobDetails, final ScheduledJobRunHistory scheduledJobRunHistory) {
        this.scheduledJobDetailsRepository.save(scheduledJobDetails);
        this.scheduledJobRunHistoryRepository.save(scheduledJobRunHistory);
    }

    @Override
    public Long fetchMaxVersionBy(final String jobKey) {
        Long version = 0L;
        final Long versionFromDB = this.scheduledJobRunHistoryRepository.findMaxVersionByJobKey(jobKey);
        if (versionFromDB != null) {
            version = versionFromDB;
        }
        return version;
    }

    @Override
    public ScheduledJobDetail findByJobId(final Long jobId) {
        return this.scheduledJobDetailsRepository.findByJobId(jobId);
    }

    @Override
    @Transactional
    public void updateSchedulerDetail(final SchedulerDetail schedulerDetail) {
        this.schedulerDetailRepository.save(schedulerDetail);
    }

    @Override
    public SchedulerDetail retriveSchedulerDetail() {
        SchedulerDetail schedulerDetail = null;
        final List<SchedulerDetail> schedulerDetailList = this.schedulerDetailRepository.findAll();
        if (schedulerDetailList != null) {
            schedulerDetail = schedulerDetailList.get(0);
        }
        return schedulerDetail;
    }

    @Transactional
    @Override
    public CommandProcessingResult updateJobDetail(final Long jobId, final JsonCommand command) {
        this.dataValidator.validateForUpdate(command.json());
        final ScheduledJobDetail scheduledJobDetail = findByJobId(jobId);
        if (scheduledJobDetail == null) { throw new JobNotFoundException(String.valueOf(jobId)); }
        final Map<String, Object> changes = scheduledJobDetail.update(command);
        if (!changes.isEmpty()) {
            this.scheduledJobDetailsRepository.saveAndFlush(scheduledJobDetail);
        }
        return CommandProcessingResult.builder() //
                .commandId(command.commandId()) //
                .resourceId(jobId) //
                .changes(changes) //
                .build();

    }

    @Transactional
    @Override
    public boolean processJobDetailForExecution(final String jobKey, final String triggerType) {
        boolean isStopExecution = false;
        final ScheduledJobDetail scheduledJobDetail = this.scheduledJobDetailsRepository.findByJobKeyWithLock(jobKey);
        if (scheduledJobDetail.isCurrentlyRunning()
                || (triggerType.equals(SchedulerServiceConstants.TRIGGER_TYPE_CRON) && (scheduledJobDetail.getNextRunTime().after(new Date())))) {
            isStopExecution = true;
        }
        final SchedulerDetail schedulerDetail = retriveSchedulerDetail();
        if (triggerType.equals(SchedulerServiceConstants.TRIGGER_TYPE_CRON) && schedulerDetail.isSuspended()) {
            scheduledJobDetail.setTriggerMisfired(true);
            isStopExecution = true;
        } else if (!isStopExecution) {
            scheduledJobDetail.setCurrentlyRunning(true);
        }
        this.scheduledJobDetailsRepository.save(scheduledJobDetail);
        return isStopExecution;
    }

}
