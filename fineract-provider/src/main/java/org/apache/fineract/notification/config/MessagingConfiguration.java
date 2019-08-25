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
package org.apache.fineract.notification.config;

import org.apache.fineract.infrastructure.core.boot.FineractSettings;
import org.apache.fineract.notification.eventandlistener.NotificationEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.jms.ConnectionFactory;

@Configuration
public class MessagingConfiguration {

	private Logger logger = LoggerFactory.getLogger(MessagingConfiguration.class);

	@Autowired
	private FineractSettings settings;
	
	@Autowired
	private NotificationEventListener notificationEventListener;

    @Bean
    public DefaultMessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory) {

    	DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();
    	messageListenerContainer.setConnectionFactory(connectionFactory); // TODO: @aleks do we really have to configure this explicitly?
    	messageListenerContainer.setDestinationName("NotificationQueue");
    	messageListenerContainer.setMessageListener(notificationEventListener);
    	messageListenerContainer.setExceptionListener(jmse -> {
			logger.error("Network Error: ActiveMQ Broker Unavailable.");
			messageListenerContainer.shutdown();
		});
    	return messageListenerContainer;
    }
    
}
