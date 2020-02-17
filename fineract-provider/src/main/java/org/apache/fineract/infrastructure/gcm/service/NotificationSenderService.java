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
package org.apache.fineract.infrastructure.gcm.service;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.gcm.GcmConstants;
import org.apache.fineract.infrastructure.gcm.domain.*;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageStatusType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationSenderService {

	private final DeviceRegistrationRepositoryWrapper deviceRegistrationRepositoryWrapper;
	private final SmsMessageRepository smsMessageRepository;
	private ExternalServicesPropertiesReadPlatformService propertiesReadPlatformService;

	public void sendNotification(List<SmsMessage> smsMessages) {
		Map<Long, List<SmsMessage>> notificationByEachClient = getNotificationListByClient(smsMessages);
		for (Map.Entry<Long, List<SmsMessage>> entry : notificationByEachClient
				.entrySet()) {
			this.sendNotifiaction(entry.getKey(), entry.getValue());
		}
	}

	public Map<Long, List<SmsMessage>> getNotificationListByClient(
			List<SmsMessage> smsMessages) {
		Map<Long, List<SmsMessage>> notificationByEachClient = new HashMap<>();
		for (SmsMessage smsMessage : smsMessages) {
			if (smsMessage.getClient() != null) {
				Long clientId = smsMessage.getClient().getId();
				if (notificationByEachClient.containsKey(clientId)) {
					notificationByEachClient.get(clientId).add(smsMessage);
				} else {
					List<SmsMessage> msgList = new ArrayList<>(
							Arrays.asList(smsMessage));
					notificationByEachClient.put(clientId, msgList);
				}

			}
		}
		return notificationByEachClient;
	}

	public void sendNotifiaction(Long clientId, List<SmsMessage> smsList) {

		DeviceRegistration deviceRegistration = this.deviceRegistrationRepositoryWrapper
				.findDeviceRegistrationByClientId(clientId);
		NotificationConfigurationData notificationConfigurationData = this.propertiesReadPlatformService.getNotificationConfiguration();
		String registrationId = null;
		if (deviceRegistration != null) {
			registrationId = deviceRegistration.getRegistrationId();
		}
		for (SmsMessage smsMessage : smsList) {
			try {
				Notification notification = Notification.builder()
					.icon(GcmConstants.defaultIcon)
					.title(GcmConstants.title)
					.body(smsMessage.getMessage())
					.build();
				Message msg = Message.builder()
					.notification(notification)
					.dryRun(false)
					.contentAvailable(true)
					.timeToLive(GcmConstants.TIME_TO_LIVE)
					.priority(GcmConstants.MESSAGE_PRIORITY_NORMAL)
					.delayWhileIdle(true)
					.build();
				Sender s = new Sender(notificationConfigurationData.getServerKey(),notificationConfigurationData.getFcmEndPoint());
				Result res;

				res = s.send(msg, registrationId, 3);
				if (res.getSuccess() != null && res.getSuccess()>0) {
					smsMessage.setStatusType(SmsMessageStatusType.SENT
							.getValue());
					smsMessage.setDeliveredOnDate(DateUtils.getLocalDateOfTenant().toDate());
				} else if (res.getFailure() != null && res.getFailure()>0) {
					smsMessage.setStatusType(SmsMessageStatusType.FAILED
							.getValue());
				}
			} catch (IOException e) {
				smsMessage
						.setStatusType(SmsMessageStatusType.FAILED.getValue());
			}
		}

		this.smsMessageRepository.saveAll(smsList);

	}

}
