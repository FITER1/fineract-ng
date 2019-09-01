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
package org.apache.fineract.integrationtests.common.savings;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integrationtests.common.Utils;

import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

@SuppressWarnings("rawtypes")
@Slf4j
public class SavingsStatusChecker {
    
    private static final String SAVINGS_ACCOUNT_URL = "/savingsaccounts";

    public static void verifySavingsIsApproved(final HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS APPROVED ------------------------------------");
        assertTrue("ERROR IN APPROVING SAVINGS APPLICATION", getStatus(savingsStatusHashMap, "approved"));
        log.info("Savings Application Status:" + savingsStatusHashMap + "\n");
    }

    public static void verifySavingsIsPending(final HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS PENDING ------------------------------------");
        assertTrue("SAVINGS ACCOUNT IS NOT IN PENDING STATE", getStatus(savingsStatusHashMap, "submittedAndPendingApproval"));
        log.info("Savings Application Status:" + savingsStatusHashMap + "\n");
    }

    public static void verifySavingsIsActive(final HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS ACTIVE ------------------------------------");
        assertTrue("ERROR IN ACTIVATING THE SAVINGS APPLICATION", getStatus(savingsStatusHashMap, "active"));
        log.info("Savings Application Status:" + savingsStatusHashMap + "\n");
    }
    
    public static void verifySavingsIsRejected(final HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS REJECTED ------------------------------------");
        assertTrue("ERROR IN REJECTING THE SAVINGS APPLICATION", getStatus(savingsStatusHashMap, "rejected"));
        log.info("Savings Application Status:" + savingsStatusHashMap + "\n");
    }
    
    public static void verifySavingsIsWithdrawn(final HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS WITHDRAWN ------------------------------------");
        assertTrue("ERROR IN WITHDRAW  THE SAVINGS APPLICATION", getStatus(savingsStatusHashMap, "withdrawnByApplicant"));
        log.info("Savings Application Status:" + savingsStatusHashMap + "\n");
    }
    
    public static void verifySavingsAccountIsClosed(final HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS CLOSED ------------------------------------");
        assertTrue("ERROR IN CLOSING THE SAVINGS APPLICATION", getStatus(savingsStatusHashMap, "closed"));
        log.info("Savings Application Status:" + savingsStatusHashMap + "\n");
    }

    public static void verifySavingsAccountIsNotActive(final HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS INACTIVE ------------------------------------");
        assertTrue(getStatus(savingsStatusHashMap, "active"));
        log.info("Savings Application Status:" + savingsStatusHashMap + "\n");
    }

    public static HashMap getStatusOfSavings(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer savingsID) {
        final String url = SAVINGS_ACCOUNT_URL+"/" + savingsID + "?"+Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, url, "status");
    }

    public static HashMap getSubStatusOfSavings(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer savingsID) {
        final String url = SAVINGS_ACCOUNT_URL+"/" + savingsID + "?"+Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, url, "subStatus");
    }

   private static boolean getStatus(final HashMap savingsStatusMap, final String nameOfSavingsStatusString) {
        return (Boolean) savingsStatusMap.get(nameOfSavingsStatusString);
    }

	public static void verifySavingsSubStatusInactive(HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS ACTIVE ------------------------------------");
        assertTrue("UNEXPECTED SAVINGS ACCOUNT SUB STATUS", getStatus(savingsStatusHashMap, "inactive"));
        log.info("Savings Application Sub Status:" + savingsStatusHashMap + "\n");
	}
	
	public static void verifySavingsSubStatusDormant(HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS ACTIVE ------------------------------------");
        assertTrue("UNEXPECTED SAVINGS ACCOUNT SUB STATUS", getStatus(savingsStatusHashMap, "dormant"));
        log.info("Savings Application Sub Status:" + savingsStatusHashMap + "\n");
	}
	
	public static void verifySavingsSubStatusEscheat(HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS ACTIVE ------------------------------------");
        assertTrue("UNEXPECTED SAVINGS ACCOUNT SUB STATUS", getStatus(savingsStatusHashMap, "escheat"));
        log.info("Savings Application Sub Status:" + savingsStatusHashMap + "\n");
	}

	public static void verifySavingsSubStatusNone(HashMap savingsStatusHashMap) {
        log.info("\n-------------------------------------- VERIFYING SAVINGS APPLICATION IS ACTIVE ------------------------------------");
        assertTrue("UNEXPECTED SAVINGS ACCOUNT SUB STATUS", getStatus(savingsStatusHashMap, "none"));
        log.info("Savings Application Sub Status:" + savingsStatusHashMap + "\n");
	}
	
	public static void verifySavingsSubStatusblock(HashMap savingsStatusHashMap) {
        log.info(
                "\n-------------------------------------- VERIFYING SAVINGS ACCOUNT IS BLOCKED ------------------------------------");
        assertTrue("block", getStatus(savingsStatusHashMap, "block"));
        log.info("Savings Application Sub Status:" + savingsStatusHashMap + "\n");
    }

	public static void verifySavingsSubStatusIsNone(HashMap savingsStatusHashMap) {
        log.info("\n------------------------- VERIFYING SAVINGS ACCOUNT IS NOT BLOCKED FOR ANY TYPE OF TRANSACTIONS ---------------------------");
        assertTrue("none", getStatus(savingsStatusHashMap, "none"));
        log.info("Savings Application Sub Status:" + savingsStatusHashMap + "\n");
    }

    public static void verifySavingsSubStatusIsDebitBlocked(HashMap savingsStatusHashMap) {
        log.info("\n--------------------- VERIFYING SAVINGS APPLICATION IS BLOCKED FOR DEBIT TRANSACTIONS ---------------------");
        assertTrue("status is blockDebit", getStatus(savingsStatusHashMap, "blockDebit"));
        log.info("Savings Application Sub Status:" + savingsStatusHashMap + "\n");
    }

    public static void verifySavingsSubStatusIsCreditBlocked(HashMap savingsStatusHashMap) {
        log.info("\n---------------------- VERIFYING SAVINGS APPLICATION IS BLOCKED FOR CREDIT TRANSACTIONS ---------------");
        assertTrue("blockCredit ", getStatus(savingsStatusHashMap, "blockCredit"));
        log.info("Savings Application Sub Status:" + savingsStatusHashMap + "\n");
    }

}