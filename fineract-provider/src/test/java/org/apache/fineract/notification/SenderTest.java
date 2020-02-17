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
package org.apache.fineract.notification;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.TestWithJmsConfiguration;
import org.apache.fineract.notification.data.NotificationData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestWithJmsConfiguration.class)
public class SenderTest {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    public void notificationCreation() {

        String objectType = "CLIENT";
        Long objectIdentifier = 1L;
        String action = "created";
        Long actorId = 1L;
        String notificationContent = "A client was created";

        NotificationData notificationData = NotificationData.builder()
            .objectType(objectType)
            .objectId(objectIdentifier)
            .action(action)
            .actorId(actorId)
            .content(notificationContent)
            .read(false)
            .systemGenerated(false)
            .build();
        );

        jmsTemplate.send(session -> {
            log.info("Message send successfully");
            return session.createObjectMessage(notificationData);
        });
    }
}
