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
package org.apache.fineract.notification.eventandlistener;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.notification.data.NotificationData;
import org.apache.fineract.notification.service.NotificationWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationEventListener implements SessionAwareMessageListener {

    private final NotificationWritePlatformService notificationWritePlatformService;

    private final AppUserRepository appUserRepository;

    @Override
    public void onMessage(Message message, Session session) throws JMSException {
        if (message instanceof ObjectMessage) {
            NotificationData notificationData = (NotificationData) ((ObjectMessage) message).getObject();

            Long appUserId = notificationData.getActorId();

            List<Long> userIds = notificationData.getUserIds();

            if (notificationData.getOfficeId() != null) {
                List<Long> tempUserIds = new ArrayList<>(userIds);
                for (Long userId : tempUserIds) {
                    AppUser appUser = appUserRepository.findById(userId).get();
                    if (!Objects.equals(appUser.getOffice().getId(), notificationData.getOfficeId())) {
                        userIds.remove(userId);
                    }
                }
            }

            if (userIds.contains(appUserId)) {
                userIds.remove(appUserId);
            }

            notificationWritePlatformService.notify(
                    userIds,
                    notificationData.getObjectType(),
                    notificationData.getObjectId(),
                    notificationData.getAction(),
                    notificationData.getActorId(),
                    notificationData.getContent(),
                    notificationData.isSystemGenerated()
            );
        }
    }
}
