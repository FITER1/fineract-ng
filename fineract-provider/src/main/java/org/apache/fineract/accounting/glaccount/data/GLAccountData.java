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
package org.apache.fineract.accounting.glaccount.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.accounting.common.AccountingEnumerations;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.accounting.glaccount.domain.GLAccountUsage;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

import java.util.Collection;
import java.util.List;

/**
 * Immutable object representing a General Ledger Account
 * 
 * Note: no getter/setters required as google-gson will produce json from fields
 * of object.
 */
@Data
@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GLAccountData {

    private final Long id;
    private final String name;
    private final Long parentId;
    private final String glCode;
    private final Boolean disabled;
    private final Boolean manualEntriesAllowed;
    private final EnumOptionData type;
    private final EnumOptionData usage;
    private final String description;
    private final String nameDecorated;
    private final CodeValueData tagId;
    private final Long organizationRunningBalance;

    // templates
    private final List<EnumOptionData> accountTypeOptions;
    private final List<EnumOptionData> usageOptions;
    private final List<GLAccountData> assetHeaderAccountOptions;
    private final List<GLAccountData> liabilityHeaderAccountOptions;
    private final List<GLAccountData> equityHeaderAccountOptions;
    private final List<GLAccountData> incomeHeaderAccountOptions;
    private final List<GLAccountData> expenseHeaderAccountOptions;
    private final Collection<CodeValueData> allowedAssetsTagOptions;
    private final Collection<CodeValueData> allowedLiabilitiesTagOptions;
    private final Collection<CodeValueData> allowedEquityTagOptions;
    private final Collection<CodeValueData> allowedIncomeTagOptions;
    private final Collection<CodeValueData> allowedExpensesTagOptions;
    private Integer rowIndex;

    public GLAccountData(Long id, String name, String glCode) {
        this(
            id,
            name,
            null,
            glCode,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public GLAccountData(final GLAccountData accountData, final List<EnumOptionData> accountTypeOptions,
                         final List<EnumOptionData> usageOptions, final List<GLAccountData> assetHeaderAccountOptions,
                         final List<GLAccountData> liabilityHeaderAccountOptions, final List<GLAccountData> equityHeaderAccountOptions,
                         final List<GLAccountData> incomeHeaderAccountOptions, final List<GLAccountData> expenseHeaderAccountOptions,
                         final Collection<CodeValueData> allowedAssetsTagOptions, final Collection<CodeValueData> allowedLiabilitiesTagOptions,
                         final Collection<CodeValueData> allowedEquityTagOptions, final Collection<CodeValueData> allowedIncomeTagOptions,
                         final Collection<CodeValueData> allowedExpensesTagOptions) {
        this.id = accountData.id;
        this.name = accountData.name;
        this.parentId = accountData.parentId;
        this.glCode = accountData.glCode;
        this.disabled = accountData.disabled;
        this.manualEntriesAllowed = accountData.manualEntriesAllowed;
        this.type = accountData.type;
        this.usage = accountData.usage;
        this.description = accountData.description;
        this.nameDecorated = accountData.nameDecorated;
        this.tagId = accountData.tagId;
        this.organizationRunningBalance = accountData.organizationRunningBalance;
        this.accountTypeOptions = accountTypeOptions;
        this.usageOptions = usageOptions;
        this.assetHeaderAccountOptions = assetHeaderAccountOptions;
        this.liabilityHeaderAccountOptions = liabilityHeaderAccountOptions;
        this.equityHeaderAccountOptions = equityHeaderAccountOptions;
        this.incomeHeaderAccountOptions = incomeHeaderAccountOptions;
        this.expenseHeaderAccountOptions = expenseHeaderAccountOptions;
        this.allowedAssetsTagOptions = allowedAssetsTagOptions;
        this.allowedLiabilitiesTagOptions = allowedLiabilitiesTagOptions;
        this.allowedEquityTagOptions = allowedEquityTagOptions;
        this.allowedIncomeTagOptions = allowedIncomeTagOptions;
        this.allowedExpensesTagOptions = allowedExpensesTagOptions;
    }

    public static GLAccountData sensibleDefaultsForNewGLAccountCreation(final Integer glAccType) {
        final EnumOptionData type;
        if (glAccType != null && glAccType >= GLAccountType.getMinValue() && glAccType <= GLAccountType.getMaxValue()) {
            type = AccountingEnumerations.gLAccountType(glAccType);
        } else {
            type = AccountingEnumerations.gLAccountType(GLAccountType.ASSET);
        }

        return new GLAccountData(
            null,
            null,
            null,
            null,
            false,
            true,
            type,
            AccountingEnumerations.gLAccountUsage(GLAccountUsage.DETAIL),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}