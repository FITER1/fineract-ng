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
package org.apache.fineract.infrastructure.core.data;

import lombok.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApiGlobalErrorResponse {

    /**
     * A developer friendly plain English description of why the HTTP error
     * response was returned from the API.
     */
    private String developerMessage;

    /**
     * The HTTP status code of the response.
     */
    private String httpStatusCode;

    /**
     * A user friendly plain English description of why the HTTP error response
     * was returned from the API that can be presented to end users.
     */
    private String defaultUserMessage;

    /**
     * A code that can be used for globalisation support by client applications
     * of the API.
     */
    private String userMessageGlobalisationCode;

    /**
     * A list of zero or more of the actual reasons for the HTTP error should
     * they be needed. Typically used to express data validation errors with
     * parameters passed to API.
     */
    @XmlElementWrapper(name = "errors")
    @XmlElement(name = "errorResponse")
    private List<ApiParameterError> errors = new ArrayList<>();

    public static ApiGlobalErrorResponse unAuthenticated() {
        return ApiGlobalErrorResponse.builder()
            .httpStatusCode("401")
            .developerMessage("Invalid authentication details were passed in api request.")
            .userMessageGlobalisationCode("error.msg.not.authenticated")
            .defaultUserMessage("Unauthenticated. Please login.")
            .build();
    }

    public static ApiGlobalErrorResponse invalidTenantIdentifier() {
        return ApiGlobalErrorResponse.builder()
            .httpStatusCode("401")
            .developerMessage("Invalid tenant details were passed in api request.")
            .userMessageGlobalisationCode("error.msg.invalid.tenant.identifier")
            .defaultUserMessage("Invalide tenant identifier provided with request.")
            .build();
    }

    public static ApiGlobalErrorResponse unAuthorized(final String defaultUserMessage) {
        return ApiGlobalErrorResponse.builder()
            .httpStatusCode("403")
            .developerMessage("The user associated with credentials passed on this request does not have sufficient privileges to perform this action.")
            .userMessageGlobalisationCode("error.msg.not.authorized")
            .defaultUserMessage("Insufficient privileges to perform this action.")
            .errors(Collections.singletonList(ApiParameterError.generalError("error.msg.not.authorized", defaultUserMessage)))
            .build();
    }

    public static ApiGlobalErrorResponse domainRuleViolation(final String globalisationMessageCode, final String defaultUserMessage, final Object... defaultUserMessageArgs) {
        return ApiGlobalErrorResponse.builder()
            .httpStatusCode("403")
            .developerMessage("Request was understood but caused a domain rule violation.")
            .userMessageGlobalisationCode("validation.msg.domain.rule.violation")
            .defaultUserMessage("Errors contain reason for domain rule violation.")
            .errors(Collections.singletonList(ApiParameterError.generalError(globalisationMessageCode, defaultUserMessage, defaultUserMessageArgs)))
            .build();
    }

    public static ApiGlobalErrorResponse notFound(final String globalisationMessageCode, final String defaultUserMessage, final Object... defaultUserMessageArgs) {
        return ApiGlobalErrorResponse.builder()
            .httpStatusCode("404")
            .developerMessage("The requested resource is not available.")
            .userMessageGlobalisationCode("error.msg.resource.not.found")
            .defaultUserMessage("The requested resource is not available.")
            .errors(Collections.singletonList(ApiParameterError.generalError(globalisationMessageCode, defaultUserMessage, defaultUserMessageArgs)))
            .build();
    }

    public static ApiGlobalErrorResponse dataIntegrityError(final String globalisationMessageCode, final String defaultUserMessage, final String parameterName, final Object... defaultUserMessageArgs) {
        return ApiGlobalErrorResponse.builder()
            .httpStatusCode("403")
            .developerMessage("The request caused a data integrity issue to be fired by the database.")
            .userMessageGlobalisationCode(globalisationMessageCode)
            .defaultUserMessage(defaultUserMessage)
            .errors(Collections.singletonList(ApiParameterError.parameterError(globalisationMessageCode, defaultUserMessage, parameterName, defaultUserMessageArgs)))
            .build();
    }

    public static ApiGlobalErrorResponse badClientRequest(final String globalisationMessageCode, final String defaultUserMessage, final List<ApiParameterError> errors) {
        return ApiGlobalErrorResponse.builder()
            .httpStatusCode("400")
            .developerMessage("The request was invalid. This typically will happen due to validation errors which are provided.")
            .userMessageGlobalisationCode(globalisationMessageCode)
            .defaultUserMessage(defaultUserMessage)
            .errors(errors)
            .build();
    }

    public static ApiGlobalErrorResponse serverSideError(final String globalisationMessageCode, final String defaultUserMessage, final Object... defaultUserMessageArgs) {
        return ApiGlobalErrorResponse.builder()
            .httpStatusCode("500")
            .developerMessage("An unexpected error occured on the platform server.")
            .userMessageGlobalisationCode("error.msg.platform.server.side.error")
            .defaultUserMessage("An unexpected error occured on the platform server.")
            .errors(Collections.singletonList(ApiParameterError.generalError(globalisationMessageCode, defaultUserMessage, defaultUserMessageArgs)))
            .build();
    }

    public static ApiGlobalErrorResponse serviceUnavailable(final String globalisationMessageCode, final String defaultUserMessage, final Object... defaultUserMessageArgs) {
        return ApiGlobalErrorResponse.builder()
            .httpStatusCode("503")
            .developerMessage("The server is currently unable to handle the request , please try after some time.")
            .userMessageGlobalisationCode("error.msg.platform.service.unavailable")
            .defaultUserMessage("The server is currently unable to handle the request , please try after some time.")
            .errors(Collections.singletonList(ApiParameterError.generalError(globalisationMessageCode, defaultUserMessage, defaultUserMessageArgs)))
            .build();
    }
}