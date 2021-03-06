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
package org.apache.fineract.integrationtests.bulkimport.populator.office;

import org.apache.fineract.infrastructure.bulkimport.constants.OfficeConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.integrationtests.BaseIntegrationTest;
import org.apache.fineract.integrationtests.common.OfficeHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class OfficeWorkBookPopulatorTest extends BaseIntegrationTest {

    @Test
    public void testOfficeWorkbookPopulate() throws IOException {
        OfficeHelper officeHelper=new OfficeHelper(requestSpec,responseSpec);
        Workbook workbook=officeHelper.getOfficeWorkBook("dd MMMM yyyy");
        Sheet sheet=workbook.getSheet(TemplatePopulateImportConstants.OFFICE_SHEET_NAME);
        Row firstRow= sheet.getRow(1);
        Assert.assertNotNull("No parent offices found",firstRow.getCell(OfficeConstants.LOOKUP_OFFICE_COL).getStringCellValue());
        Assert.assertEquals(1,firstRow.getCell(OfficeConstants.LOOKUP_OFFICE_ID_COL).getNumericCellValue(),0.0);

    }
}
