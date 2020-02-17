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
package org.apache.fineract.infrastructure.dataqueries.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.dataqueries.domain.*;
import org.apache.fineract.infrastructure.dataqueries.exception.ReportNotFoundException;
import org.apache.fineract.infrastructure.dataqueries.exception.ReportParameterNotFoundException;
import org.apache.fineract.infrastructure.dataqueries.serialization.ReportCommandFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.Permission;
import org.apache.fineract.useradministration.domain.PermissionRepository;
import org.apache.fineract.useradministration.exception.PermissionNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportWritePlatformServiceImpl implements ReportWritePlatformService {

    private final PlatformSecurityContext context;
    private final ReportCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final ReportRepository reportRepository;
    private final ReportParameterUsageRepository reportParameterUsageRepository;
    private final ReportParameterRepository reportParameterRepository;
    private final PermissionRepository permissionRepository;
    private final ReadReportingService readReportingService;

    @Transactional
    @Override
    public CommandProcessingResult createReport(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validate(command.json());

            final Report report = Report.fromJson(command, this.readReportingService.getAllowedReportTypes());
            final Set<ReportParameterUsage> reportParameterUsages = assembleSetOfReportParameterUsages(report, command);
            report.update(reportParameterUsages);

            this.reportRepository.save(report);

            final Permission permission = new Permission("report", report.getReportName(), "READ");
            this.permissionRepository.save(permission);

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(report.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleReportDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleReportDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateReport(final Long reportId, final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validate(command.json());

            final Report report = this.reportRepository.findById(reportId)
                    .orElseThrow(() -> new ReportNotFoundException(reportId));

            final Map<String, Object> changes = report.update(command, this.readReportingService.getAllowedReportTypes());

            if (changes.containsKey("reportParameters")) {
                final Set<ReportParameterUsage> reportParameterUsages = assembleSetOfReportParameterUsages(report, command);
                final boolean updated = report.update(reportParameterUsages);
                if (!updated) {
                    changes.remove("reportParameters");
                }
            }

            if (!changes.isEmpty()) {
                this.reportRepository.saveAndFlush(report);
            }

            return CommandProcessingResult.builder() //
                    .commandId(command.commandId()) //
                    .resourceId(report.getId()) //
                    .changes(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleReportDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResult();
        }catch (final PersistenceException dve) {
        	Throwable throwable = ExceptionUtils.getRootCause(dve.getCause()) ;
        	handleReportDataIntegrityIssues(command, throwable, dve);
        	return new CommandProcessingResult();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteReport(final Long reportId) {

        final Report report = this.reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        if (report.isCoreReport()) {
            //
            throw new PlatformDataIntegrityException("error.msg.cant.delete.core.report", "Core Reports Can't be Deleted", "");
        }

        final Permission permission = this.permissionRepository.findOneByCode("READ" + "_" + report.getReportName());
        if (permission == null) { throw new PermissionNotFoundException("READ" + "_" + report.getReportName()); }

        this.reportRepository.delete(report);
        this.permissionRepository.delete(permission);

        return CommandProcessingResult.builder() //
                .resourceId(reportId) //
                .build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleReportDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("unq_report_name") || realCause.getMessage().contains("report_name_UNIQUE")) {
            final String name = command.stringValueOfParameterNamed("reportName");
            throw new PlatformDataIntegrityException("error.msg.report.duplicate.name", "A report with name '" + name + "' already exists",
                    "name", name);
        }

        log.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.report.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }

    private Set<ReportParameterUsage> assembleSetOfReportParameterUsages(final Report report, final JsonCommand command) {

        Set<ReportParameterUsage> reportParameterUsages = null;

        if (command.parameterExists("reportParameters")) {
            final JsonArray reportParametersArray = command.arrayOfParameterNamed("reportParameters");
            if (reportParametersArray != null) {

                reportParameterUsages = new HashSet<>();

                for (int i = 0; i < reportParametersArray.size(); i++) {

                    final JsonObject jsonObject = reportParametersArray.get(i).getAsJsonObject();

                    Long id = null;
                    ReportParameterUsage reportParameterUsageItem = null;
                    ReportParameter reportParameter = null;
                    String reportParameterName = null;

                    if (jsonObject.has("id")) {
                        final String idStr = jsonObject.get("id").getAsString();
                        if (StringUtils.isNotBlank(idStr)) {
                            id = Long.parseLong(idStr);
                        }
                    }

                    if (id != null) {
                        // existing report parameter usage
                        reportParameterUsageItem = this.reportParameterUsageRepository.findById(id).orElse(null);
                        if (reportParameterUsageItem == null) { throw new ReportParameterNotFoundException(id); }

                        // check parameter
                        if (jsonObject.has("parameterId")) {
                            final Long parameterId = jsonObject.get("parameterId").getAsLong();
                            reportParameter = this.reportParameterRepository.findById(parameterId)
                                    .orElseThrow(() -> new ReportParameterNotFoundException(parameterId));
                            if (reportParameterUsageItem.getParameter()!=null && !parameterId.equals(reportParameterUsageItem.getParameter().getId())) {
                                //
                                throw new ReportParameterNotFoundException(parameterId);
                            }
                        }

                        if (jsonObject.has("reportParameterName")) {
                            reportParameterName = jsonObject.get("reportParameterName").getAsString();
                            reportParameterUsageItem.setReportParameterName(reportParameterName);
                        }
                    } else {
                        // new report parameter usage
                        if (jsonObject.has("parameterId")) {
                            final Long parameterId = jsonObject.get("parameterId").getAsLong();
                            reportParameter = this.reportParameterRepository.findById(parameterId)
                                    .orElseThrow(() -> new ReportParameterNotFoundException(parameterId));
                        } else {
                            throw new PlatformDataIntegrityException("error.msg.parameter.id.mandatory.in.report.parameter",
                                    "parameterId column is mandatory in Report Parameter Entry");
                        }

                        if (jsonObject.has("reportParameterName")) {
                            reportParameterName = jsonObject.get("reportParameterName").getAsString();
                        }

                        reportParameterUsageItem = new ReportParameterUsage(report, reportParameter, reportParameterName);
                    }

                    reportParameterUsages.add(reportParameterUsageItem);
                }
            }
        }

        return reportParameterUsages;
    }
}