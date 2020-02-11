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
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.boot.FineractProperties;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerTriggerListener implements TriggerListener {

    private final String name = "Global trigger Listner";

    private final SchedularWritePlatformService schedularService;

    private final FineractProperties fineractProperties;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void triggerFired(@SuppressWarnings("unused") final Trigger trigger,
            @SuppressWarnings("unused") final JobExecutionContext context) {

    }

    @Override
    public boolean vetoJobExecution(final Trigger trigger, final JobExecutionContext context) {

        final JobKey key = trigger.getJobKey();
        final String jobKey = key.getName() + SchedulerServiceConstants.JOB_KEY_SEPERATOR + key.getGroup();
        String triggerType = SchedulerServiceConstants.TRIGGER_TYPE_CRON;
        if (context.getMergedJobDataMap().containsKey(SchedulerServiceConstants.TRIGGER_TYPE_REFERENCE)) {
            triggerType = context.getMergedJobDataMap().getString(SchedulerServiceConstants.TRIGGER_TYPE_REFERENCE);
        }
        Integer maxNumberOfRetries = fineractProperties.getConnection().getMaxRetriesOnDeadlock();
        Integer maxIntervalBetweenRetries = fineractProperties.getConnection().getMaxIntervalBetweenRetries();
        Integer numberOfRetries = 0;
        boolean proceedJob = false;
        while (numberOfRetries <= maxNumberOfRetries) {
            try {
                proceedJob = this.schedularService.processJobDetailForExecution(jobKey, triggerType);
                numberOfRetries = maxNumberOfRetries + 1;
            } catch (Exception exception) { //Adding generic exception as it depends on JPA provider
                log.debug("Not able to acquire the lock to update job running status for JobKey: " + jobKey);
                try {
                    Random random = new Random();
                    int randomNum = random.nextInt(maxIntervalBetweenRetries + 1);
                    Thread.sleep(1000 + (randomNum * 1000));
                    numberOfRetries = numberOfRetries + 1;
                } catch (InterruptedException e) {

                }
            }
        }
        return proceedJob;
    }

    @Override
    public void triggerMisfired(@SuppressWarnings("unused") final Trigger trigger) {

    }

    @Override
    public void triggerComplete(@SuppressWarnings("unused") final Trigger trigger,
            @SuppressWarnings("unused") final JobExecutionContext context,
            @SuppressWarnings("unused") final CompletedExecutionInstruction triggerInstructionCode) {

    }

}
