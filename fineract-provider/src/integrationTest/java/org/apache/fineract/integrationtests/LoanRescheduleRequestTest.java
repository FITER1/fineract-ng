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
package org.apache.fineract.integrationtests;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.specification.ResponseSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.LoanRescheduleRequestHelper;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanRescheduleRequestTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 
 * Test the creation, approval and rejection of a loan reschedule request 
 **/
@SuppressWarnings({ "rawtypes" })
@Slf4j
public class LoanRescheduleRequestTest extends BaseIntegrationTest {
	private ResponseSpecification generalResponseSpec;
    private LoanTransactionHelper loanTransactionHelper;
    private LoanRescheduleRequestHelper loanRescheduleRequestHelper;
    private Integer clientId;
    private Integer loanProductId;
    private Integer loanId;
    private Integer loanRescheduleRequestId;
    private String loanPrincipalAmount = "100000.00";
    private String numberOfRepayments = "12";
    private String interestRatePerPeriod = "18";
    private String dateString = "4 September 2014";
    
    @Before
    public void initialize() {
    	super.setup();

        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        this.loanRescheduleRequestHelper = new LoanRescheduleRequestHelper(this.requestSpec, this.responseSpec);

        this.generalResponseSpec = new ResponseSpecBuilder().build();
        
        // create all required entities
        this.createRequiredEntities();
    }
    
    /** 
     * Creates the client, loan product, and loan entities 
     **/
    private void createRequiredEntities() {
    	this.createClientEntity();
    	this.createLoanProductEntity();
    	this.createLoanEntity();
    }
    
    /** 
     * create a new client 
     **/ 
    private void createClientEntity() {
    	this.clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
    	
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, this.clientId);
    }
    
    /** 
     * create a new loan product 
     **/
    private void createLoanProductEntity() {
    	log.info("---------------------------------CREATING LOAN PRODUCT------------------------------------------");
    	
    	final String loanProductJSON = new LoanProductTestBuilder()
    			.withPrincipal(loanPrincipalAmount)
    			.withNumberOfRepayments(numberOfRepayments)
    			.withinterestRatePerPeriod(interestRatePerPeriod)
    			.withInterestRateFrequencyTypeAsYear()
    			.build(null);
    	
    	this.loanProductId = this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    	log.info("Successfully created loan product  (ID: " + this.loanProductId + ")");
    }
    
    /** 
     * submit a new loan application, approve and disburse the loan 
     **/
    private void createLoanEntity() {
    	log.info("---------------------------------NEW LOAN APPLICATION------------------------------------------");
    	
    	final String loanApplicationJSON = new LoanApplicationTestBuilder()
    			.withPrincipal(loanPrincipalAmount)
    			.withLoanTermFrequency(numberOfRepayments)
                .withLoanTermFrequencyAsMonths()
                .withNumberOfRepayments(numberOfRepayments)
                .withRepaymentEveryAfter("1")
                .withRepaymentFrequencyTypeAsMonths()
                .withAmortizationTypeAsEqualInstallments()
                .withInterestCalculationPeriodTypeAsDays()
                .withInterestRatePerPeriod(interestRatePerPeriod)
                .withLoanTermFrequencyAsMonths()
                .withSubmittedOnDate(dateString)
                .withExpectedDisbursementDate(dateString)
                .withPrincipalGrace("2")
                .withInterestGrace("2")
    			.build(this.clientId.toString(), this.loanProductId.toString(), null);
    	
    	this.loanId = this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    	
    	log.info("Sucessfully created loan (ID: " + this.loanId + ")");
    	
    	this.approveLoanApplication();
    	this.disburseLoan();
    }
    
    /** 
     * approve the loan application 
     **/
    private void approveLoanApplication() {
    	
    	if(this.loanId != null) {
    		this.loanTransactionHelper.approveLoan(this.dateString, this.loanId);
    		log.info("Successfully approved loan (ID: " + this.loanId + ")");
    	}
    }
    
    /** 
     * disburse the newly created loan 
     **/
    private void disburseLoan() {
    	
    	if(this.loanId != null) {
    		this.loanTransactionHelper.disburseLoan(this.dateString, this.loanId);
    		log.info("Successfully disbursed loan (ID: " + this.loanId + ")");
    	}
    }
    
    /** 
     * create new loan reschedule request 
     **/
    private void createLoanRescheduleRequest() {
    	log.info("---------------------------------CREATING LOAN RESCHEDULE REQUEST------------------------------------------");
    	
    	final String requestJSON = new LoanRescheduleRequestTestBuilder().build(this.loanId.toString());
    	
    	this.loanRescheduleRequestId = this.loanRescheduleRequestHelper.createLoanRescheduleRequest(requestJSON);
    	this.loanRescheduleRequestHelper.verifyCreationOfLoanRescheduleRequest(this.loanRescheduleRequestId);
    	
    	log.info("Successfully created loan reschedule request (ID: " + this.loanRescheduleRequestId + ")");
    }
    
    @Test
    public void testCreateLoanRescheduleRequest() {
    	this.createLoanRescheduleRequest();
    }
    
    @Test
    public void testRejectLoanRescheduleRequest() {
    	this.createLoanRescheduleRequest();
    	
    	log.info("-----------------------------REJECTING LOAN RESCHEDULE REQUEST--------------------------");
    	
    	final String requestJSON = new LoanRescheduleRequestTestBuilder().getRejectLoanRescheduleRequestJSON();
    	this.loanRescheduleRequestHelper.rejectLoanRescheduleRequest(this.loanRescheduleRequestId, requestJSON);
    	
    	final HashMap response = (HashMap) this.loanRescheduleRequestHelper.getLoanRescheduleRequest(loanRescheduleRequestId, "statusEnum");
    	assertTrue((Boolean)response.get("rejected"));
    	
    	log.info("Successfully rejected loan reschedule request (ID: " + this.loanRescheduleRequestId + ")");
    }
    
    @Test
    public void testApproveLoanRescheduleRequest() {
    	this.createLoanRescheduleRequest();
    	
    	log.info("-----------------------------APPROVING LOAN RESCHEDULE REQUEST--------------------------");
    	
    	final String requestJSON = new LoanRescheduleRequestTestBuilder().getApproveLoanRescheduleRequestJSON();
    	this.loanRescheduleRequestHelper.approveLoanRescheduleRequest(this.loanRescheduleRequestId, requestJSON);
    	
    	final HashMap response = (HashMap) this.loanRescheduleRequestHelper.getLoanRescheduleRequest(loanRescheduleRequestId, "statusEnum");
    	assertTrue((Boolean)response.get("approved"));
    	
    	final Integer numberOfRepayments = (Integer) this.loanTransactionHelper.getLoanDetail(requestSpec, generalResponseSpec, loanId, "numberOfRepayments");
    	final HashMap loanSummary = this.loanTransactionHelper.getLoanSummary(requestSpec, generalResponseSpec, loanId);
    	final Float totalExpectedRepayment = (Float) loanSummary.get("totalExpectedRepayment");
    	
    	assertEquals("NUMBER OF REPAYMENTS SHOULD BE 16, NOT 12", "12", numberOfRepayments.toString());
    	assertEquals("TOTAL EXPECTED REPAYMENT MUST BE EQUAL TO 118000.0", "118000.0", totalExpectedRepayment.toString());
    	
    	log.info("Successfully approved loan reschedule request (ID: " + this.loanRescheduleRequestId + ")");
    }
}
