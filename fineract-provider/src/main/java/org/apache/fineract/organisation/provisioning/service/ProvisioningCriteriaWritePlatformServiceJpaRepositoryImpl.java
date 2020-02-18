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
package org.apache.fineract.organisation.provisioning.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepository;
import org.apache.fineract.accounting.provisioning.service.ProvisioningEntriesReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.provisioning.constants.ProvisioningCriteriaConstants;
import org.apache.fineract.organisation.provisioning.data.ProvisioningCriteriaDefinitionData;
import org.apache.fineract.organisation.provisioning.domain.LoanProductProvisionCriteria;
import org.apache.fineract.organisation.provisioning.domain.ProvisioningCriteria;
import org.apache.fineract.organisation.provisioning.domain.ProvisioningCriteriaDefinition;
import org.apache.fineract.organisation.provisioning.domain.ProvisioningCriteriaRepository;
import org.apache.fineract.organisation.provisioning.exception.ProvisioningCategoryNotFoundException;
import org.apache.fineract.organisation.provisioning.exception.ProvisioningCriteriaCannotBeDeletedException;
import org.apache.fineract.organisation.provisioning.exception.ProvisioningCriteriaNotFoundException;
import org.apache.fineract.organisation.provisioning.serialization.ProvisioningCriteriaDefinitionJsonDeserializer;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvisioningCriteriaWritePlatformServiceJpaRepositoryImpl implements ProvisioningCriteriaWritePlatformService {

    private final ProvisioningCriteriaDefinitionJsonDeserializer fromApiJsonDeserializer;
    private final ProvisioningCriteriaAssembler provisioningCriteriaAssembler;
    private final ProvisioningCriteriaRepository provisioningCriteriaRepository;
    private final FromJsonHelper fromApiJsonHelper;
    private final GLAccountRepository glAccountRepository;
    private final ProvisioningEntriesReadPlatformService provisioningEntriesReadPlatformService ;

    @Override
    public CommandProcessingResult createProvisioningCriteria(JsonCommand command) {
        try {
            this.fromApiJsonDeserializer.validateForCreate(command.json());
            ProvisioningCriteria provisioningCriteria = provisioningCriteriaAssembler.fromParsedJson(command.parsedJson());
            this.provisioningCriteriaRepository.save(provisioningCriteria);
            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(provisioningCriteria.getId()).build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Override
    public CommandProcessingResult deleteProvisioningCriteria(Long criteriaId) {
        this.provisioningCriteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new ProvisioningCriteriaNotFoundException(criteriaId));
        if(this.provisioningEntriesReadPlatformService.retrieveProvisioningEntryDataByCriteriaId(criteriaId) != null) {
            throw new ProvisioningCriteriaCannotBeDeletedException(criteriaId) ;
        }
        this.provisioningCriteriaRepository.deleteById(criteriaId); ;
        return CommandProcessingResult.builder().resourceId(criteriaId).build();
    }

    @Override
    public CommandProcessingResult updateProvisioningCriteria(final Long criteriaId, JsonCommand command) {
    	try {
    		this.fromApiJsonDeserializer.validateForUpdate(command.json());
            ProvisioningCriteria provisioningCriteria = provisioningCriteriaRepository.findById(criteriaId).orElse(null);
            if(provisioningCriteria == null) {
                throw new ProvisioningCategoryNotFoundException(criteriaId) ;
            }
            List<LoanProduct> products = this.provisioningCriteriaAssembler.parseLoanProducts(command.parsedJson()) ;
            final Map<String, Object> changes = update(provisioningCriteria, command, products) ;
            if(!changes.isEmpty()) {
                updateProvisioningCriteriaDefinitions(provisioningCriteria, command) ;
                provisioningCriteriaRepository.saveAndFlush(provisioningCriteria) ;    
            }
            return CommandProcessingResult.builder().commandId(command.commandId()).resourceId(provisioningCriteria.getId()).build();
    	} catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }


    private Map<String, Object> update(final ProvisioningCriteria criteria, final JsonCommand command, final List<LoanProduct> loanProducts) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);
        if(command.isChangeInStringParameterNamed(ProvisioningCriteriaConstants.JSON_CRITERIANAME_PARAM, criteria.getCriteriaName())) {
            final String valueAsInput = command.stringValueOfParameterNamed(ProvisioningCriteriaConstants.JSON_CRITERIANAME_PARAM);
            actualChanges.put(ProvisioningCriteriaConstants.JSON_CRITERIANAME_PARAM, valueAsInput);
            criteria.setCriteriaName(valueAsInput);
        }

        Set<LoanProductProvisionCriteria> temp = new HashSet<>() ;
        Set<LoanProduct> productsTemp = new HashSet<>() ;

        for(LoanProductProvisionCriteria mapping: criteria.getLoanProductMapping()) {
            if(!loanProducts.contains(mapping.getLoanProduct())) {
                temp.add(mapping) ;
            }else {
                productsTemp.add(mapping.getLoanProduct()) ;
            }
        }
        criteria.getLoanProductMapping().removeAll(temp) ;

        for(LoanProduct loanProduct: loanProducts) {
            if(!productsTemp.contains(loanProduct)) {
                criteria.getLoanProductMapping().add( new LoanProductProvisionCriteria(criteria, loanProduct)) ;
            }
        }

        actualChanges.put(ProvisioningCriteriaConstants.JSON_LOANPRODUCTS_PARAM, criteria.getLoanProductMapping());
        return actualChanges ;
    }

    private void updateProvisioningCriteriaDefinitions(ProvisioningCriteria provisioningCriteria, JsonCommand command) {
        final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(command.parsedJson().getAsJsonObject());
        JsonArray jsonProvisioningCriteria = this.fromApiJsonHelper.extractJsonArrayNamed(
                ProvisioningCriteriaConstants.JSON_PROVISIONING_DEFINITIONS_PARAM, command.parsedJson());
        for (JsonElement element : jsonProvisioningCriteria) {
            JsonObject jsonObject = element.getAsJsonObject();
            Long id = this.fromApiJsonHelper.extractLongNamed("id", jsonObject) ;
            Long categoryId = this.fromApiJsonHelper.extractLongNamed(ProvisioningCriteriaConstants.JSON_CATEOGRYID_PARAM, jsonObject);
            Long minimumAge = this.fromApiJsonHelper.extractLongNamed(ProvisioningCriteriaConstants.JSON_MINIMUM_AGE_PARAM, jsonObject);
            Long maximumAge = this.fromApiJsonHelper.extractLongNamed(ProvisioningCriteriaConstants.JSON_MAXIMUM_AGE_PARAM, jsonObject);
            BigDecimal provisioningpercentage = this.fromApiJsonHelper.extractBigDecimalNamed(ProvisioningCriteriaConstants.JSON_PROVISIONING_PERCENTAGE_PARAM,
                    jsonObject, locale);
            Long liabilityAccountId = this.fromApiJsonHelper.extractLongNamed(ProvisioningCriteriaConstants.JSON_LIABILITY_ACCOUNT_PARAM, jsonObject);
            Long expenseAccountId = this.fromApiJsonHelper.extractLongNamed(ProvisioningCriteriaConstants.JSON_EXPENSE_ACCOUNT_PARAM, jsonObject);
            GLAccount liabilityAccount = glAccountRepository.findById(liabilityAccountId).orElse(null);
            GLAccount expenseAccount = glAccountRepository.findById(expenseAccountId).orElse(null);
            String categoryName = null ;
            String liabilityAccountName = null ;
            String expenseAccountName = null ;
            ProvisioningCriteriaDefinitionData data = ProvisioningCriteriaDefinitionData.builder()
                .id(id)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .minAge(minimumAge)
                .maxAge(maximumAge)
                .provisioningPercentage(provisioningpercentage)
                .liabilityAccount(liabilityAccount.getId())
                .liabilityCode(liabilityAccount.getGlCode())
                .liabilityName(liabilityAccountName)
                .expenseAccount(expenseAccount.getId())
                .expenseCode(expenseAccount.getGlCode())
                .expenseName(expenseAccountName)
                .build();
            update(provisioningCriteria, data, liabilityAccount, expenseAccount) ;
        }
    }

    private void update(final ProvisioningCriteria criteria, final ProvisioningCriteriaDefinitionData data, final GLAccount liability, final GLAccount expense) {
        for(ProvisioningCriteriaDefinition def: criteria.getProvisioningCriteriaDefinition()) {
            if(data.getId().equals(def.getId())) {
                def.toBuilder()
                    .minimumAge(data.getMinAge())
                    .maximumAge(data.getMaxAge())
                    .provisioningPercentage(data.getProvisioningPercentage())
                    .liabilityAccount(liability)
                    .expenseAccount(expense)
                    .build();
                break ;
            }
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("criteria_name")) {
            final String name = command.stringValueOfParameterNamed("criteria_name");
            throw new PlatformDataIntegrityException("error.msg.provisioning.duplicate.criterianame", "Provisioning Criteria with name `"
                    + name + "` already exists", "category name", name);
        }else if (realCause.getMessage().contains("product_id")) {
			throw new PlatformDataIntegrityException(
					"error.msg.provisioning.product.id(s).already.associated.existing.criteria",
					"The selected products already associated with another Provisioning Criteria");
		}
        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.provisioning.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}
