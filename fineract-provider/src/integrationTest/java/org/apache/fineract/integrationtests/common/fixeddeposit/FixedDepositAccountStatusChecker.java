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
package org.apache.fineract.integrationtests.common.fixeddeposit;

import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integrationtests.common.Utils;
import org.junit.Assert;

import java.util.HashMap;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("rawtypes")
@Slf4j
public class FixedDepositAccountStatusChecker {

    private static final String FIXED_DEPOSIT_ACCOUNT_URL = "/fixeddepositaccounts";

    public static void verifyFixedDepositIsApproved(final HashMap fixedDepositStatusHashMap) {
        log.info("-------------------- VERIFYING FIXED DEPOSIT APPLICATION IS APPROVED --------------------");
        assertTrue("Error in Approving Fixed deposit application", getStatus(fixedDepositStatusHashMap, "approved"));
        log.info("Approving Fixed deposit application: {}", fixedDepositStatusHashMap);
    }

    public static void verifyFixedDepositIsPending(final HashMap fixedDepositStatusHashMap) {
        log.info("-------------------- VERIFYING FIXED DEPOSIT APPLICATION IS PENDING --------------------");
        assertTrue("FIXED DEPOSIT ACCOUNT IS NOT IN PENDING STATE", getStatus(fixedDepositStatusHashMap, "submittedAndPendingApproval"));
        log.info("Approving Fixed deposit application: {}", fixedDepositStatusHashMap);
    }

    public static void verifyFixedDepositIsActive(final HashMap fixedDepositStatusHashMap) {
        log.info("----------------- VERIFYING FIXED DEPOSIT APPLICATION IS ACTIVE -----------------");
        assertTrue("ERROR IN ACTIVATING THE FIXED DEPOSIT APPLICATION", getStatus(fixedDepositStatusHashMap, "active"));
        log.info("Approving Fixed deposit application: {}", fixedDepositStatusHashMap);
    }

    public static void verifyFixedDepositIsRejected(final HashMap fixedDepositStatusHashMap) {
        log.info("-------------- VERIFYING FIXED DEPOSIT APPLICATION IS REJECTED ----------------");
        assertTrue("ERROR IN REJECTING THE FIXED DEPOSIT APPLICATION", getStatus(fixedDepositStatusHashMap, "rejected"));
        log.info("Approving Fixed deposit application: {}", fixedDepositStatusHashMap);
    }

    public static void verifyFixedDepositIsWithdrawn(final HashMap fixedDepositStatusHashMap) {
        log.info("---------------- VERIFYING FIXED DEPOSIT APPLICATION IS WITHDRAWN ----------------");
        assertTrue("ERROR IN WITHDRAW  THE FIXED DEPOSIT APPLICATION", getStatus(fixedDepositStatusHashMap, "withdrawnByApplicant"));
        log.info("Approving Fixed deposit application: {}", fixedDepositStatusHashMap);
    }

    public static void verifyFixedDepositAccountIsClosed(final HashMap fixedDepositStatusHashMap) {
        log.info("--------------------- VERIFYING FIXED DEPOSIT APPLICATION IS CLOSED ---------------------");
        assertTrue("ERROR IN CLOSING THE FIXED DEPOSIT APPLICATION", getStatus(fixedDepositStatusHashMap, "closed"));
        log.info("Approving Fixed deposit application: {}", fixedDepositStatusHashMap);
    }

    public static void verifyFixedDepositAccountIsNotActive(final HashMap fixedDepositStatusHashMap) {
        log.info("------------------ VERIFYING FIXED DEPOSIT APPLICATION IS INACTIVE --------------------");
        Assert.assertFalse(getStatus(fixedDepositStatusHashMap, "active"));
        log.info("Approving Fixed deposit application: {}", fixedDepositStatusHashMap);
    }

    public static HashMap getStatusOfFixedDepositAccount(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String fixedDepositAccountID) {
        final String GET_STATUS_OF_FIXED_DEPOSIT_ACCOUNT_URL = FIXED_DEPOSIT_ACCOUNT_URL + "/" + fixedDepositAccountID + "?"
                + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, GET_STATUS_OF_FIXED_DEPOSIT_ACCOUNT_URL, "status");
    }

    public static void verifyFixedDepositAccountIsPrematureClosed(HashMap fixedDepositStatusHashMap) {
        log.info("--------------------- VERIFYING FIXED DEPOSIT APPLICATION IS CLOSED ---------------------");
        assertTrue("ERROR IN PREMATURELY CLOSING THE FIXED DEPOSIT ACCOUNT", getStatus(fixedDepositStatusHashMap, "prematureClosed"));
        log.info("Approving Fixed deposit application: {}", fixedDepositStatusHashMap);
    }
    
    public static void verifyFixedDepositAccountIsMatured(HashMap fixedDepositStatusHashMap) {
        log.info("--------------------- VERIFYING FIXED DEPOSIT APPLICATION IS MATURED ---------------------");
        assertTrue("ERROR IN MATURITY JOB OF THE FIXED DEPOSIT ACCOUNT", getStatus(fixedDepositStatusHashMap, "matured"));
        log.info("Approving Fixed deposit application: {}", fixedDepositStatusHashMap);
    }

    private static boolean getStatus(final HashMap fixedDepositStatusMap, final String fixedDepositStatusString) {
        return (Boolean) fixedDepositStatusMap.get(fixedDepositStatusString);
    }
}