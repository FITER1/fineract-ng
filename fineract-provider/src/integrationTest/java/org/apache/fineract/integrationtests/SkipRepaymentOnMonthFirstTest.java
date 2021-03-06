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

import junit.framework.Assert;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integrationtests.common.CalendarHelper;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.GlobalConfigurationHelper;
import org.apache.fineract.integrationtests.common.GroupHelper;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({ "static-access", "rawtypes", "unchecked", "deprecation" })
@Slf4j
public class SkipRepaymentOnMonthFirstTest extends BaseIntegrationTest {

	private GlobalConfigurationHelper globalConfigurationHelper;
	private LoanTransactionHelper loanTransactionHelper;
	private CalendarHelper calendarHelper;

	@Test
	public void testSkippingRepaymentOnFirstDayOfMonth() {
		this.globalConfigurationHelper = new GlobalConfigurationHelper(this.requestSpec, this.responseSpec);

		// Retrieving All Global Configuration details
		final ArrayList<HashMap> globalConfig = this.globalConfigurationHelper
				.getAllGlobalConfigurations(this.requestSpec, this.responseSpec);
		Assert.assertNotNull(globalConfig);

		String configName = "skip-repayment-on-first-day-of-month";
		boolean newBooleanValue = true;

		for (Integer configIndex = 0; configIndex < (globalConfig.size()); configIndex++) {
			if (globalConfig.get(configIndex).get("name").equals(configName)) {
				String configId = (globalConfig.get(configIndex).get("id")).toString();
				Integer updateConfigId = this.globalConfigurationHelper.updateEnabledFlagForGlobalConfiguration(
						this.requestSpec, this.responseSpec, configId.toString(), newBooleanValue);
				Assert.assertNotNull(updateConfigId);
				break;
			}
		}

	}

	@Test
	public void checkRepaymentSkipOnFirstDayOfMonth() {
		this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);

		final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec);
		Integer groupID = GroupHelper.createGroup(this.requestSpec, this.responseSpec, true);
		groupID = GroupHelper.associateClient(this.requestSpec, this.responseSpec, groupID.toString(),
				clientID.toString());
		final String startDate = "15 September 2011";
		final String frequency = "3"; // Monthly
		final String interval = "1"; //Every One Moth
		Integer calendarID = calendarHelper.createMeetingForGroup(requestSpec, responseSpec, groupID, startDate, frequency,
				interval, null);
		log.info("caladerId --------------------" + calendarID);
		final Integer loanProductID = createLoanProduct();
		final Integer loanID = applyForLoanApplication(groupID, loanProductID, calendarID, clientID);
		log.info("loanID----" + loanID);
		final ArrayList<HashMap> loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec,
				this.responseSpec, loanID);
		verifyLoanRepaymentSchedule(loanSchedule);

	}

	private Integer createLoanProduct() {
		log.info(
				"------------------------------CREATING NEW LOAN PRODUCT ---------------------------------------");
		final String loanProductJSON = new LoanProductTestBuilder() //
				.withPrincipal("12,000.00") //
				.withNumberOfRepayments("4") //
				.withRepaymentAfterEvery("1") //
				.withRepaymentTypeAsMonth() //
				.withinterestRatePerPeriod("1") //
				.withInterestRateFrequencyTypeAsMonths() //
				.withAmortizationTypeAsEqualInstallments() //
				.withInterestTypeAsDecliningBalance() //
				.build(null);
		return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
	}

	private Integer applyForLoanApplication(final Integer groupID, final Integer loanProductID, Integer calendarID,
			Integer clientID) {
		log.info(
				"--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
		final String loanApplicationJSON = new LoanApplicationTestBuilder() //
				.withPrincipal("12,000.00") //
				.withLoanTermFrequency("4") //
				.withLoanTermFrequencyAsMonths() //
				.withNumberOfRepayments("4") //
				.withRepaymentEveryAfter("1") //
				.withRepaymentFrequencyTypeAsMonths() //
				.withInterestRatePerPeriod("2") //
				.withAmortizationTypeAsEqualInstallments() //
				.withInterestTypeAsDecliningBalance() //
				.withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
				.withExpectedDisbursementDate("01 October 2011") //
				.withCalendarID(calendarID.toString()) //
				.withSubmittedOnDate("01 October 2011") //
				.withLoanType("jlg").build(clientID.toString(), groupID.toString(), loanProductID.toString(), null);
		log.info(loanApplicationJSON);
		return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
	}

	private void verifyLoanRepaymentSchedule(final ArrayList<HashMap> loanSchedule) {
		log.info("--------------------VERIFYING THE REPAYMENT DATE--------------------------");
		assertEquals("Checking for Repayment Date for 1st Month", new ArrayList<>(Arrays.asList(2011, 10, 15)),
				loanSchedule.get(1).get("dueDate"));
		log.info("Repayment Date for 1st Month--" + loanSchedule.get(1).get("dueDate"));
		
		assertEquals("Checking for Repayment Date for 2nd Month", new ArrayList<>(Arrays.asList(2011, 11, 15)),
				loanSchedule.get(2).get("dueDate"));
		log.info("Repayment Date for 2nd Month--" + loanSchedule.get(2).get("dueDate"));
		
		assertEquals("Checking for  Repayment Date for 3rd Month", new ArrayList<>(Arrays.asList(2011, 12, 15)),
				loanSchedule.get(3).get("dueDate"));
		log.info("Repayment Date for 3rd Month--" + loanSchedule.get(3).get("dueDate"));
		
		assertEquals("Checking for  Repayment Date for 4th Month", new ArrayList<>(Arrays.asList(2012, 1, 15)),
				loanSchedule.get(4).get("dueDate"));
		log.info("Repayment Date for 4th Month--" + loanSchedule.get(4).get("dueDate"));
	}

}
