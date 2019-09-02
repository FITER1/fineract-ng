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
package org.apache.fineract.batch.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: @aleks fix this
/**
 * Provides methods to create dependency map among the various batchRequests. It
 * also provides method that takes care of dependency resolution among related
 * requests.
 * 
 * @author Rishabh Shukla
 * @see BatchApiServiceImpl
 */
@Component
@Slf4j
public class ResolutionHelper {

    /**
     * Provides a Node like object for the request tree.
     * 
     * @author Rishabh shukla
     * 
     */
    public class BatchRequestNode {

        private BatchRequest request;
        private final List<BatchRequestNode> childRequests = new ArrayList<>();

        public BatchRequestNode() {
            super();
        }

        public BatchRequest getRequest() {
            return this.request;
        }

        public void setRequest(BatchRequest request) {
            this.request = request;
        }

        public List<BatchRequestNode> getChildRequests() {
            return this.childRequests;
        }

        public void addChildRequest(final BatchRequestNode batchRequest) {
            this.childRequests.add(batchRequest);
        }

    }

    private FromJsonHelper fromJsonHelper;

    @Autowired
    public ResolutionHelper(final FromJsonHelper fromJsonHelper) {
        this.fromJsonHelper = fromJsonHelper;
    }

    /**
     * Returns a map containing requests that are divided in accordance of
     * dependency relations among them. Each different list is identified with a
     * "Key" which is the "requestId" of the request at topmost level in
     * dependency hierarchy of that particular list.
     * 
     * @param batchRequests
     * @return List&lt;ArrayList&lt;BatchRequestNode&gt;&gt;
     */
    public List<BatchRequestNode> getDependingRequests(final List<BatchRequest> batchRequests) {
        final List<BatchRequestNode> rootRequests = new ArrayList<>();

        for (BatchRequest batchRequest : batchRequests) {
            if (batchRequest.getReference() == null) {
                final BatchRequestNode node = new BatchRequestNode();
                node.setRequest(batchRequest);
                rootRequests.add(node);
            } else {
                this.addDependingRequest(batchRequest, rootRequests);
            }
        }

        return rootRequests;
    }

    private void addDependingRequest(final BatchRequest batchRequest, final List<BatchRequestNode> parentRequests) {
        for (BatchRequestNode batchRequestNode : parentRequests) {
            if (batchRequestNode.getRequest().getRequestId().equals(batchRequest.getReference())) {
                final BatchRequestNode dependingRequest = new BatchRequestNode();
                dependingRequest.setRequest(batchRequest);
                batchRequestNode.addChildRequest(dependingRequest);
            } else {
                addDependingRequest(batchRequest, batchRequestNode.getChildRequests());
            }
        }
    }

    /**
     * Returns a BatchRequest after dependency resolution. It takes a request
     * and the response of the request it is dependent upon as its arguments and
     * change the body or relativeUrl of the request according to parent
     * Request.
     * 
     * @param request
     * @param parentResponse
     * @return BatchRequest
     */
    public BatchRequest resoluteRequest(final BatchRequest request, final BatchResponse parentResponse) {

        // Create a duplicate request
        // TODO: Java is by reference... br and request are one and the same
        final BatchRequest br = request;

        final JsonObject responseJsonModel = this.fromJsonHelper.parse(parentResponse.getBody()).getAsJsonObject();

        // Also check the relativeUrl for any dependency resolution
        String relativeUrl = request.getRelativeUrl();

        if (relativeUrl.contains("$.")) {

            List<String> parameters = parseParameters(relativeUrl);

            for (String parameter : parameters) {
                if (parameter.contains("$.")) {
                    // TODO: add null checks to make this a bit more robust
                    String key = parameter.substring(2);
                    final String resParamValue = responseJsonModel.get(key).getAsString();

                    String regex = "\\$\\." + key;
                    relativeUrl = relativeUrl.replaceAll(regex, resParamValue);
                }
            }

            br.setRelativeUrl(relativeUrl);
        }

        String body = request.getBody();

        if (!StringUtils.isEmpty(body) && body.contains("$.")) {
            List<String> parameters = parseParameters(body);

            for (String parameter : parameters) {
                if (parameter.contains("$.")) {
                    // TODO: add null checks to make this a bit more robust
                    String key = parameter.substring(2);
                    final String resParamValue = responseJsonModel.get(key).getAsString();

                    String regex = "\\$\\." + key;
                    if(NumberUtils.isCreatable(resParamValue) || "true".equalsIgnoreCase(resParamValue) || "false".equalsIgnoreCase(resParamValue)) {
                        regex = "\"" + regex + "\""; // NOTE: remove quotes if it's a number or a boolean
                    }
                    body = body.replaceAll(regex, resParamValue);
                }
            }

            br.setBody(body);
        }

        return br;
    }

    private List<String> parseParameters(String s) {
        List<String> result = new ArrayList<>();

        try {
            Pattern p = Pattern.compile("(\\$\\.[\\w]+)");
            Matcher m = p.matcher(s);

            while (m.find()) {
                result.add(m.group());
            }

        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return result;
    }

    private JsonElement resolveDependentVariables(final Entry<String, JsonElement> entryElement/*, final JsonModel responseJsonModel*/) {
        JsonElement value = null;

        final JsonElement element = entryElement.getValue();

        if (element.isJsonObject()) {
            final JsonObject jsObject = element.getAsJsonObject();
            value = processJsonObject(jsObject/*, responseJsonModel*/);
        } else if (element.isJsonArray()) {
            final JsonArray jsElementArray = element.getAsJsonArray();
            value = processJsonArray(jsElementArray/*, responseJsonModel*/);
        } else {
            value = resolveDependentVariable(element/*, responseJsonModel*/);
        }
        return value;
    }

    private JsonElement processJsonObject(final JsonObject jsObject/*, final JsonModel responseJsonModel*/) {
        JsonObject valueObj = new JsonObject();
        for (Entry<String, JsonElement> element : jsObject.entrySet()) {
            final String key = element.getKey();
            final JsonElement value = resolveDependentVariable(element.getValue()/*, responseJsonModel*/);
            valueObj.add(key, value);
        }
        return valueObj;
    }

    private JsonArray processJsonArray(final JsonArray elementArray/*, final JsonModel responseJsonModel*/) {

        JsonArray valueArr = new JsonArray();

        for (JsonElement element : elementArray) {
            if (element.isJsonObject()) {
                final JsonObject jsObject = element.getAsJsonObject();
                valueArr.add(processJsonObject(jsObject/*, responseJsonModel*/));
            }
        }

        return valueArr;
    }

    private JsonElement resolveDependentVariable(final JsonElement element /*, final JsonModel responseJsonModel*/) {
        JsonElement value = element;
        String paramVal = element.getAsString();
        if (paramVal.contains("$.")) {
            // Get the value of the parameter from parent response
            final String resParamValue = ""; /*responseJsonModel.get(paramVal).toString();*/
            value = this.fromJsonHelper.parse(resParamValue);
        }
        return value;
    }

}
