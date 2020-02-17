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
package org.apache.fineract.infrastructure.gcm.domain;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Result of a GCM message request that returned HTTP status code 200.
 *
 * <p>
 * If the message is successfully created, the {@link #getMessageId()} returns
 * the message id and {@link #getErrorCode()} returns {@literal null};
 * otherwise, {@link #getMessageId()} returns {@literal null} and
 * {@link #getErrorCode()} returns the code of the error.
 *
 * <p>
 * There are cases when a request is accept and the message successfully
 * created, but GCM has a canonical registration id for that device. In this
 * case, the server should update the registration id to avoid rejected requests
 * in the future.
 * 
 * <p>
 * In a nutshell, the workflow to handle a result is:
 * 
 * <pre>
 *   - Call {@link #getMessageId()}:
 *     - {@literal null} means error, call {@link #getErrorCode()}
 *     - non-{@literal null} means the message was created:
 *       - Call {@link #getCanonicalRegistrationId()}
 *         - if it returns {@literal null}, do nothing.
 *         - otherwise, update the server datastore with the new id.
 * </pre>
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class Result implements Serializable {
	private String messageId;
	private String canonicalRegistrationId;
	private String errorCode;
	private Integer success;
	private Integer failure;
	private List<String> failedRegistrationIds;
	private int status;
}
