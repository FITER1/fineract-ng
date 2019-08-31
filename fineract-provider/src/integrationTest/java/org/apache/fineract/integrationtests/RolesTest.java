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
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.integrationtests.common.organisation.StaffHelper;
import org.apache.fineract.integrationtests.useradministration.roles.RolesHelper;
import org.apache.fineract.integrationtests.useradministration.users.UserHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Slf4j
public class RolesTest extends BaseIntegrationTest {

    @Before
    public void setup() {
        super.setup();
    }

    @Test
    public void testCreateRolesStatus() {

        log.info("---------------------------------CREATING A ROLE---------------------------------------------");
        final Integer roleId = RolesHelper.createRole(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(roleId);

        log.info("--------------------------------- Getting ROLE -------------------------------");
        HashMap<String, Object> role = RolesHelper.getRoleDetails(requestSpec, responseSpec, roleId);
        assertEquals(role.get("id"), roleId);

    }

    @Test
    public void testDisableRolesStatus() {

        log.info("---------------------------------CREATING A ROLE---------------------------------------------");
        final Integer roleId = RolesHelper.createRole(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(roleId);

        log.info("--------------------------------- Getting ROLE -------------------------------");
        HashMap<String, Object> role = RolesHelper.getRoleDetails(requestSpec, responseSpec, roleId);
        assertEquals(role.get("id"), roleId);

        log.info("--------------------------------- DISABLING ROLE -------------------------------");
        final Integer disableRoleId = RolesHelper.disableRole(this.requestSpec, this.responseSpec, roleId);
        assertEquals(disableRoleId, roleId);
        role = RolesHelper.getRoleDetails(requestSpec, responseSpec, roleId);
        assertEquals(role.get("id"), roleId);
        assertEquals(role.get("disabled"), true);

    }

    @SuppressWarnings("cast")
    @Test
    public void testEnableRolesStatus() {

        log.info("---------------------------------CREATING A ROLE---------------------------------------------");
        final Integer roleId = RolesHelper.createRole(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(roleId);

        log.info("--------------------------------- Getting ROLE -------------------------------");
        HashMap<String, Object> role = RolesHelper.getRoleDetails(requestSpec, responseSpec, roleId);
        assertEquals((Integer) role.get("id"), roleId);

        log.info("--------------------------------- DISABLING ROLE -------------------------------");
        final Integer disableRoleId = RolesHelper.disableRole(this.requestSpec, this.responseSpec, roleId);
        assertEquals(disableRoleId, roleId);
        role = RolesHelper.getRoleDetails(requestSpec, responseSpec, roleId);
        assertEquals((Integer) role.get("id"), roleId);
        assertEquals((Boolean) role.get("disabled"), true);

        log.info("--------------------------------- ENABLING ROLE -------------------------------");
        final Integer enableRoleId = RolesHelper.enableRole(this.requestSpec, this.responseSpec, roleId);
        assertEquals(enableRoleId, roleId);
        role = RolesHelper.getRoleDetails(requestSpec, responseSpec, roleId);
        assertEquals((Integer) role.get("id"), roleId);
        assertEquals((Boolean) role.get("disabled"), false);

    }

    @Test
    public void testDeleteRoleStatus() {

        log.info("-------------------------------- CREATING A ROLE---------------------------------------------");
        final Integer roleId = RolesHelper.createRole(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(roleId);

        log.info("--------------------------------- Getting ROLE -------------------------------");
        HashMap<String, Object> role = RolesHelper.getRoleDetails(requestSpec, responseSpec, roleId);
        assertEquals(role.get("id"), roleId);

        log.info("--------------------------------- DELETE ROLE -------------------------------");
        final Integer deleteRoleId = RolesHelper.deleteRole(this.requestSpec, this.responseSpec, roleId);
        assertEquals(deleteRoleId, roleId);
    }

    @Test
    public void testRoleShouldGetDeletedIfNoActiveUserExists() {
        final Integer roleId = RolesHelper.createRole(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(roleId);

        final Integer staffId = StaffHelper.createStaff(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(staffId);

        final Integer userId = UserHelper.createUser(this.requestSpec, this.responseSpec, roleId, staffId);
        Assert.assertNotNull(userId);

        final Integer deletedUserId = UserHelper.deleteUser(this.requestSpec, this.responseSpec, userId);
        Assert.assertEquals(deletedUserId, userId);

        final Integer deletedRoleId = RolesHelper.deleteRole(this.requestSpec, this.responseSpec, roleId);
        assertEquals(deletedRoleId, roleId);
    }

    @Test
    public void testRoleShouldNotGetDeletedIfActiveUserExists() {
        final Integer roleId = RolesHelper.createRole(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(roleId);

        final Integer staffId = StaffHelper.createStaff(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(staffId);

        final Integer userId = UserHelper.createUser(this.requestSpec, this.responseSpec, roleId, staffId);
        Assert.assertNotNull(userId);

        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(403).build();
        final Integer deletedRoleId = RolesHelper.deleteRole(this.requestSpec, this.responseSpec, roleId);
        assertNotEquals(deletedRoleId, roleId);
    }
}