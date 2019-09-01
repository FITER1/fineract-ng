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
package org.apache.fineract.integrationtests.common;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
public class CenterHelper {

    private static final String CENTERS_URL = "/centers";

    public static final String CREATED_DATE = "29 December 2014";
    private static final String CREATE_CENTER_URL = "/centers?" + Utils.TENANT_IDENTIFIER;

    public static CenterDomain retrieveByID(int id, final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        final String GET_CENTER_BY_ID_URL = CENTERS_URL + "/" + id + "?associations=groupMembers&" + Utils.TENANT_IDENTIFIER;
        log.info("------------------------ RETRIEVING CENTER AT " + id + "-------------------------");
        Object get = Utils.performServerGet(requestSpec, responseSpec, GET_CENTER_BY_ID_URL, "");
		final String jsonData = new Gson().toJson(get);
        return new Gson().fromJson(jsonData, new TypeToken<CenterDomain>() {}.getType());
    }

    public static ArrayList<CenterDomain> paginatedListCenters(final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec) {
        final String GET_CENTER = CENTERS_URL + "?paged=true&limit=-1&" + Utils.TENANT_IDENTIFIER;
        log.info("------------------------ RETRIEVING CENTERS-------------------------");
        Object get = Utils.performServerGet(requestSpec, responseSpec, GET_CENTER, "pageItems");
        final String jsonData = new Gson().toJson(get);
        return new Gson().fromJson(jsonData, new TypeToken<ArrayList<CenterDomain>>() {}.getType());
    }

    public static ArrayList<CenterDomain> listCenters(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        final String GET_CENTER = CENTERS_URL + "?limit=-1&" + Utils.TENANT_IDENTIFIER;
        log.info("------------------------ RETRIEVING CENTERS-------------------------");
        Object get = Utils.performServerGet(requestSpec, responseSpec, GET_CENTER, "");
        final String jsonData = new Gson().toJson(get);
        return new Gson().fromJson(jsonData, new TypeToken<ArrayList<CenterDomain>>() {}.getType());
    }

    public static int createCenter(final String name, final int officeId, final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec) {
        return createCenter(name, officeId, null, -1, null, null, requestSpec, responseSpec);
    }

    public static int createCenter(final String name, final int officeId, final String activationDate,
            final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        return createCenter(name, officeId, null, -1, null, activationDate, requestSpec, responseSpec);
    }

    public static int createCenter(final String name, final int officeId, final String externalId, final int staffId,
            final int[] groupMembers, final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        return createCenter(name, officeId, externalId, staffId, groupMembers, null, requestSpec, responseSpec);
    }

    public static int createCenter(final String name, final int officeId, final String externalId, final int staffId,
            final int[] groupMembers, final String activationDate, final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec) {
        final String CREATE_CENTER_URL = CENTERS_URL + "?" + Utils.TENANT_IDENTIFIER;
        HashMap hm = new HashMap();
        hm.put("name", name);
        hm.put("officeId", officeId);
        hm.put("active", false);

        if (externalId != null) hm.put("externalId", externalId);
        if (staffId != -1) hm.put("staffId", staffId);
        if (groupMembers != null) hm.put("groupMembers", groupMembers);
        if (activationDate != null) {
            hm.put("active", true);
            hm.put("locale", "en");
            hm.put("dateFormat", "dd MMM yyyy");
            hm.put("activationDate", activationDate);
        }
        
        log.info("------------------------CREATING CENTER-------------------------");
        return Utils.performServerPost(requestSpec, responseSpec, CREATE_CENTER_URL, new Gson().toJson(hm), "resourceId");
    }

    public static HashMap<String, String> updateCenter(final int id, HashMap request, final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec) {
        final String UPDATE_CENTER_URL = CENTERS_URL + "/" + id + "?" + Utils.TENANT_IDENTIFIER;
        log.info("---------------------------------UPDATE CENTER AT " + id + "---------------------------------------------");
        HashMap<String, String> hash = Utils.performServerPut(requestSpec, responseSpec, UPDATE_CENTER_URL, new Gson().toJson(request),
                "changes");
        return hash;
    }

    public static int[] associateGroups(final int id, final int[] groupMembers, final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec) {
        final String ASSOCIATE_GROUP_CENTER_URL = CENTERS_URL + "/" + id + "?command=associateGroups&" + Utils.TENANT_IDENTIFIER;
        HashMap groupMemberHashMap = new HashMap();
        groupMemberHashMap.put("groupMembers", groupMembers);
        log.info("---------------------------------ASSOCIATING GROUPS AT " + id + "--------------------------------------------");
        HashMap hash = Utils.performServerPost(requestSpec, responseSpec, ASSOCIATE_GROUP_CENTER_URL,
                new Gson().toJson(groupMemberHashMap), "changes");
        log.info(hash);
        ArrayList<String> arr = (ArrayList<String>) hash.get("groupMembers");
        int[] ret = new int[arr.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = Integer.parseInt(arr.get(i));
        }
        return ret;
    }

    public static void deleteCenter(final int id, final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        final String DELETE_CENTER_URL = CENTERS_URL + "/" + id + "?" + Utils.TENANT_IDENTIFIER;
        log.info("---------------------------------DELETING CENTER AT " + id + "--------------------------------------------");
        Utils.performServerDelete(requestSpec, responseSpec, DELETE_CENTER_URL, "");
    }

    public static Integer createCenter(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            @SuppressWarnings("unused") final boolean active) {
        log.info("---------------------------------CREATING A CENTER---------------------------------------------");
        return createCenter(requestSpec, responseSpec, "CREATED_DATE");
    }

    public static Integer createCenter(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String activationDate) {
        log.info("---------------------------------CREATING A CENTER---------------------------------------------");
        return Utils.performServerPost(requestSpec, responseSpec, CREATE_CENTER_URL, getTestCenterAsJSON(true, activationDate), "groupId");
    }

    public static Integer createCenter(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        log.info("---------------------------------CREATING A CENTER---------------------------------------------");
        return Utils.performServerPost(requestSpec, responseSpec, CREATE_CENTER_URL, getTestCenterAsJSON(true, CenterHelper.CREATED_DATE),
                "groupId");
    }

    public static int createCenterWithStaffId(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer staffId) {
        log.info("---------------------------------CREATING A CENTER---------------------------------------------");
        return Utils.performServerPost(requestSpec, responseSpec, CREATE_CENTER_URL,
                getTestCenterWithStaffAsJSON(true, CenterHelper.CREATED_DATE, staffId), "groupId");
    }

    public static void verifyCenterCreatedOnServer(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer generatedCenterID) {
        log.info("------------------------------CHECK CENTER DETAILS------------------------------------\n");
        final String CENTER_URL = "/centers/" + generatedCenterID + "?" + Utils.TENANT_IDENTIFIER;
        final Integer responseCenterID = Utils.performServerGet(requestSpec, responseSpec, CENTER_URL, "id");
        assertEquals("ERROR IN CREATING THE CENTER", generatedCenterID, responseCenterID);
    }

    public static void verifyCenterActivatedOnServer(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer generatedCenterID, final boolean generatedCenterStatus) {
        log.info("------------------------------CHECK CENTER STATUS------------------------------------\n");
        final String CENTER_URL = "/centers/" + generatedCenterID + "?" + Utils.TENANT_IDENTIFIER;
        final Boolean responseCenterStatus = Utils.performServerGet(requestSpec, responseSpec, CENTER_URL, "active");
        assertEquals("ERROR IN ACTIVATING THE CENTER", generatedCenterStatus, responseCenterStatus);
    }

    public static Integer activateCenter(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String centerId) {
        final String CENTER_ASSOCIATE_URL = "/centers/" + centerId + "?command=activate&" + Utils.TENANT_IDENTIFIER;
        log.info("---------------------------------ACTIVATE A CENTER---------------------------------------------");
        return Utils.performServerPost(requestSpec, responseSpec, CENTER_ASSOCIATE_URL, activateCenterAsJSON(""), "groupId");
    }

    public static String getTestCenterWithStaffAsJSON(final boolean active, final String activationDate, final Integer staffId) {
       
        Integer id = null;
        Integer statusid = null;
        String statuscode = null;
        String statusvalue = null;
        String name = null;
        String externalId = null;
        Integer officeID = null;
        String officeName = null;
        String hierarchy = null;
        int[] groupMembers = null;
        String submittedDate = null;

        return CenterDomain.jsonRequestToCreateCenter(id, statusid, statuscode, statusvalue, active, activationDate,submittedDate,name,
                externalId, staffId, officeID, officeName, hierarchy, groupMembers);
    }

    public static String getTestCenterAsJSON(final boolean active, final String activationDate) {
      
        Integer id = null;
        Integer statusid = null;
        String statuscode = null;
        String statusvalue = null;
        String name = null;
        String externalId = null;
        Integer officeID = null;
        String officeName = null;
        Integer staffId = null;
        String hierarchy = null;
        final int[] groupMembers = null;
        String submittedDate = null;

        return CenterDomain.jsonRequestToCreateCenter(id, statusid, statuscode, statusvalue, active, activationDate,submittedDate,name,
                externalId, staffId, officeID, officeName, hierarchy, groupMembers);
        
    }

    public static String assignStaffAsJSON(final Long staffId) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("staffId", staffId);
        log.info("map : " + map);
        return new Gson().toJson(map);
    }

    public static String unassignStaffAsJSON(final Long staffId) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("staffId", staffId);
        log.info("map : " + map);
        return new Gson().toJson(map);
    }

    public static String activateCenterAsJSON(final String activationDate) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("locale", "en");
        if (StringUtils.isNotEmpty(activationDate)) {
            map.put("activationDate", activationDate);
        } else {
            map.put("activationDate", "CREATED_DATE");
            log.info("defaulting to fixed date: CREATED_DATE");
        }
        log.info("map : " + map);
        return new Gson().toJson(map);
    }

    public static String randomNameGenerator(final String prefix, final int lenOfRandomSuffix) {
        return Utils.randomStringGenerator(prefix, lenOfRandomSuffix);
    }

    private static String randomIDGenerator(final String prefix, final int lenOfRandomSuffix) {
        return Utils.randomStringGenerator(prefix, lenOfRandomSuffix, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    public static Object assignStaff(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String groupId, final Long staffId) {
        final String GROUP_ASSIGN_STAFF_URL = "/groups/" + groupId + "?" + Utils.TENANT_IDENTIFIER
                + "&command=assignStaff";
        log.info("---------------------------------Assign Staff---------------------------------------------");
        return Utils.performServerPost(requestSpec, responseSpec, GROUP_ASSIGN_STAFF_URL, assignStaffAsJSON(staffId), "changes");
    }

    public static Object unassignStaff(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String groupId, final Long staffId) {
        final String GROUP_ASSIGN_STAFF_URL = "/groups/" + groupId + "?" + Utils.TENANT_IDENTIFIER
                + "&command=unassignStaff";
        log.info("---------------------------------Unassign Staff---------------------------------------------");
        return Utils.performServerPost(requestSpec, responseSpec, GROUP_ASSIGN_STAFF_URL, unassignStaffAsJSON(staffId), "changes");
    }

}
