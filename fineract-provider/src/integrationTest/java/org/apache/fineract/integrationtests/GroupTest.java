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

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.GroupHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.apache.fineract.integrationtests.common.organisation.StaffHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Group Test for checking Group: Creation, Activation, Client Association,
 * Updating & Deletion
 */
@Slf4j
public class GroupTest extends BaseIntegrationTest {

    private LoanTransactionHelper loanTransactionHelper;
    private final String principal = "10000.00";
    private final String accountingRule = "1";
    private final String numberOfRepayments = "5";
    private final String interestRatePerPeriod = "18";

    @Before
    public void setup() {
        super.setup();

        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
    }

    @Test
    public void checkGroupFunctions() {
        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Integer groupID = GroupHelper.createGroup(this.requestSpec, this.responseSpec);
        GroupHelper.verifyGroupCreatedOnServer(this.requestSpec, this.responseSpec, groupID);

        groupID = GroupHelper.activateGroup(this.requestSpec, this.responseSpec, groupID.toString());
        GroupHelper.verifyGroupActivatedOnServer(this.requestSpec, this.responseSpec, groupID, true);

        groupID = GroupHelper.associateClient(this.requestSpec, this.responseSpec, groupID.toString(), clientID.toString());
        GroupHelper.verifyGroupMembers(this.requestSpec, this.responseSpec, groupID, clientID);

        groupID = GroupHelper.disAssociateClient(this.requestSpec, this.responseSpec, groupID.toString(), clientID.toString());
        GroupHelper.verifyEmptyGroupMembers(this.requestSpec, this.responseSpec, groupID);

        final String updatedGroupName = GroupHelper.randomNameGenerator("Group-", 5);
        groupID = GroupHelper.updateGroup(this.requestSpec, this.responseSpec, updatedGroupName, groupID.toString());
        GroupHelper.verifyGroupDetails(this.requestSpec, this.responseSpec, groupID, "name", updatedGroupName);

        // NOTE: removed as consistently provides false positive result on
        // cloudbees server.
        // groupID = GroupHelper.createGroup(this.requestSpec,
        // this.responseSpec);
        // GroupHelper.deleteGroup(this.requestSpec, this.responseSpec,
        // groupID.toString());
        // GroupHelper.verifyGroupDeleted(this.requestSpec, this.responseSpec,
        // groupID);
    }

    @Test
    public void assignStaffToGroup() {
        Integer groupID = GroupHelper.createGroup(this.requestSpec, this.responseSpec);
        GroupHelper.verifyGroupCreatedOnServer(this.requestSpec, this.responseSpec, groupID);

        final String updateGroupName = Utils.randomNameGenerator("Savings Group Help_", 5);
        groupID = GroupHelper.activateGroup(this.requestSpec, this.responseSpec, groupID.toString());
        Integer updateGroupId = GroupHelper.updateGroup(this.requestSpec, this.responseSpec, updateGroupName, groupID.toString());

        // create client and add client to group
        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        groupID = GroupHelper.associateClient(this.requestSpec, this.responseSpec, groupID.toString(), clientID.toString());
        GroupHelper.verifyGroupMembers(this.requestSpec, this.responseSpec, groupID, clientID);

        // create staff
        Integer createStaffId1 = StaffHelper.createStaff(this.requestSpec, this.responseSpec);
        log.info("--------------creating first staff with id-------------" + createStaffId1);
        Assert.assertNotNull(createStaffId1);

        Integer createStaffId2 = StaffHelper.createStaff(this.requestSpec, this.responseSpec);
        log.info("--------------creating second staff with id-------------" + createStaffId2);
        Assert.assertNotNull(createStaffId2);

        // assign staff "createStaffId1" to group
        HashMap assignStaffGroupId = (HashMap) GroupHelper.assignStaff(this.requestSpec, this.responseSpec, groupID.toString(),
                createStaffId1.longValue());
        assertEquals("Verify assigned staff id is the same as id sent", assignStaffGroupId.get("staffId"), createStaffId1);

        // assign staff "createStaffId2" to client
        final HashMap assignStaffToClientChanges = (HashMap) ClientHelper.assignStaffToClient(this.requestSpec, this.responseSpec,
                clientID.toString(), createStaffId2.toString());
        assertEquals("Verify assigned staff id is the same as id sent", assignStaffToClientChanges.get("staffId"), createStaffId2);

        final Integer loanProductId = this.createLoanProduct();

        final Integer loanId = this.applyForLoanApplication(clientID, loanProductId, this.principal);

        this.loanTransactionHelper.approveLoan("20 September 2014", loanId);
        this.loanTransactionHelper.disburseLoan("20 September 2014", loanId);

        final HashMap assignStaffAndInheritStaffForClientAccounts = (HashMap) GroupHelper.assignStaffInheritStaffForClientAccounts(
                this.requestSpec, this.responseSpec, groupID.toString(), createStaffId1.toString());
        final Integer getClientStaffId = ClientHelper.getClientsStaffId(this.requestSpec, this.responseSpec, clientID.toString());

        // assert if client staff officer has change Note client was assigned
        // staff with createStaffId2
        assertNotEquals("Verify if client stuff has changed", assignStaffAndInheritStaffForClientAccounts.get("staffId"), createStaffId2);
        assertEquals("Verify if client inherited staff assigned above", assignStaffAndInheritStaffForClientAccounts.get("staffId"),
                getClientStaffId);

        // assert if clients loan officer has changed
        final Integer loanOfficerId = this.loanTransactionHelper.getLoanOfficerId(loanId.toString());
        assertEquals("Verify if client loan inherited staff", assignStaffAndInheritStaffForClientAccounts.get("staffId"), loanOfficerId);

    }

    private Integer createLoanProduct() {
        final String loanProductJSON = new LoanProductTestBuilder().withPrincipal(this.principal)
                .withNumberOfRepayments(this.numberOfRepayments).withinterestRatePerPeriod(this.interestRatePerPeriod)
                .withInterestRateFrequencyTypeAsYear().build(null);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private Integer applyForLoanApplication(final Integer clientID, final Integer loanProductID, String principal) {
        log.info("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal(principal) //
                .withLoanTermFrequency("4") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("4") //
                .withRepaymentEveryAfter("1") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsDecliningBalance() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate("20 September 2014") //
                .withSubmittedOnDate("20 September 2014") //
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

}
