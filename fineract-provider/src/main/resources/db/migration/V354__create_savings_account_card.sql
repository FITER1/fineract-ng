--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

CREATE TABLE savings_account_card(
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    savings_account_id BIGINT(20) NOT NULL,
    application_id VARCHAR(100),
    application_flow_id BIGINT(20) NOT NULL,
    application_status VARCHAR(100) NOT NULL,
    card_number VARCHAR(100),
    cardholder_name VARCHAR(100),
    card_type VARCHAR(100),
    expiry_date DATE,
    createdon_date DATE NOT NULL,
    last_updatedon_date DATE,
    PRIMARY KEY (id),
    CONSTRAINT FK_card_savings_account FOREIGN KEY (`savings_account_id`) REFERENCES `m_savings_account` (`id`) ON DELETE cascade
);

INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`) VALUES ('portfolio', 'CREATE_SAVINGSACCOUNTCARD', 'SAVINGSACCOUNTCARD', 'CREATE');
INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`) VALUES ('portfolio', 'UPDATE_SAVINGSACCOUNTCARD', 'SAVINGSACCOUNTCARD', 'UPDATE');
INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`) VALUES ('portfolio', 'READ_SAVINGSACCOUNTCARD', 'SAVINGSACCOUNTCARD', 'READ');
