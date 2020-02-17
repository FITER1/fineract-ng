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
import java.util.Collections;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class Notification implements Serializable {
	private String title;
	private String body;
	private String icon;
	private String sound;
	private Integer badge;
	private String tag;
	private String color;
	private String clickAction;
	private String bodyLocKey;
	private List<String> bodyLocArgs;
	private String titleLocKey;
	private List<String> titleLocArgs;
}
