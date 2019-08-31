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
import org.apache.fineract.integrationtests.common.xbrl.XBRLIntegrationTestHelper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Slf4j
public class XBRLIntegrationTest extends BaseIntegrationTest {

    private XBRLIntegrationTestHelper xbrlHelper;

    @Test
    public void shouldRetrieveTaxonomyList() {
        this.xbrlHelper = new XBRLIntegrationTestHelper(this.requestSpec, this.responseSpec);

        final ArrayList<HashMap> taxonomyList = this.xbrlHelper.getTaxonomyList();
        verifyTaxonomyList(taxonomyList);
    }

    private void verifyTaxonomyList(final ArrayList<HashMap> taxonomyList) {
        log.info("--------------------VERIFYING TAXONOMY LIST--------------------------");
        assertEquals("Checking for the 1st taxonomy", "AdministrativeExpense", taxonomyList.get(0).get("name"));
    }

}
