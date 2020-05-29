/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.report.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.boot.FineractProperties;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.report.annotation.ReportService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.AppUser;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.DefaultReportEnvironment;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.CSVReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelReportUtil;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@ReportService(type = "Pentaho")
public class PentahoReportingProcessService implements ReportingProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PentahoReportingProcessService.class);
    private static final String REPORTING_ERROR_MSG_CODE = "error.msg.reporting.error";
    private static final String REPORTING_INVALID_OUTPUT_TYPE_CODE = "error.msg.invalid.outputType";
    public static final String MIFOS_BASE_DIR = System.getProperty("user.home") + File.separator + ".mifosx";

    private final boolean noPentaho;
    private final PlatformSecurityContext context;
    private final FineractProperties fineractProperties;

    @Autowired
    public PentahoReportingProcessService(PlatformSecurityContext context, FineractProperties fineractProperties) {
        ClassicEngineBoot.getInstance().start();
        this.noPentaho = false;
        this.context = context;
        this.fineractProperties = fineractProperties;
    }

    @Override
    public Response processRequest(String reportName, MultivaluedMap<String, String> queryParams) {

        if (this.noPentaho) {
            throw new PlatformDataIntegrityException("error.msg.no.pentaho", "Pentaho is not enabled",
                    "Pentaho is not enabled");
        }

        final String contentTypeName = "Content-Disposition";
        final String contentTypeValue = "attachment;filename=";
        final AppUser currentUser = this.context.authenticatedUser();
        final String outputTypeParam = queryParams.getFirst("output-type");

        String outputType = "HTML";
        if (StringUtils.isNotBlank(outputTypeParam)) {
            outputType = outputTypeParam;
        }

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Map<String, String> reportParams = getReportParams(queryParams);
        Locale locale = ApiParameterHelper.extractLocale(queryParams);
        this.createReport(reportName, byteArrayOutputStream, outputType, reportParams, currentUser, locale);
        switch (outputType.toLowerCase()) {
            case "pdf":
                return Response.ok().entity(byteArrayOutputStream.toByteArray()).type("application/pdf").build();
            case "xls":
                return Response.ok().entity(byteArrayOutputStream.toByteArray()).type("application/vnd.ms-excel")
                        .header(contentTypeName, contentTypeValue + reportName.replace(" ", "") + ".xls").build();
            case "xlsx":
                return Response.ok().entity(byteArrayOutputStream.toByteArray()).type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .header(contentTypeName, contentTypeValue + reportName.replace(" ", "") + ".xlsx").build();
            case "csv":
                return Response.ok().entity(byteArrayOutputStream.toByteArray()).type("text/csv")
                        .header(contentTypeName, contentTypeValue + reportName.replace(" ", "") + ".csv").build();
            case "html":
                return Response.ok().entity(byteArrayOutputStream.toByteArray()).type("text/html").build();
            default:
                throw new PlatformDataIntegrityException(REPORTING_INVALID_OUTPUT_TYPE_CODE, "No matching Output Type: " + outputType);
        }
    }

    private void createReport(String reportName, ByteArrayOutputStream byteArrayOutputStream,
                              String outputType, Map<String, String> reportParams,
                              AppUser currentUser, Locale locale) {

        ResourceManager manager = new ResourceManager();
        manager.registerDefaults();
        String reportPath = MIFOS_BASE_DIR + File.separator + "pentahoReports" + File.separator + reportName + ".prpt";
        LOGGER.info("Report path: {}", reportPath);

        try {
            Resource res = manager.createDirectly(reportPath, MasterReport.class);
            MasterReport masterReport = (MasterReport) res.getResource();
            this.addParametersToReport(masterReport, reportParams, currentUser);
            DefaultReportEnvironment reportEnvironment = (DefaultReportEnvironment) masterReport.getReportEnvironment();
            if (locale != null) {
                reportEnvironment.setLocale(locale);
            }
            switch (outputType.toLowerCase()) {
                case "pdf":
                    PdfReportUtil.createPDF(masterReport, byteArrayOutputStream);
                    break;
                case "xls":
                    ExcelReportUtil.createXLS(masterReport, byteArrayOutputStream);
                    break;
                case "xlsx":
                    ExcelReportUtil.createXLSX(masterReport, byteArrayOutputStream);
                    break;
                case "csv":
                    CSVReportUtil.createCSV(masterReport, byteArrayOutputStream, "UTF-8");
                    break;
                case "html":
                    HtmlReportUtil.createStreamHTML(masterReport, byteArrayOutputStream);
                    break;
                default:
                    throw new PlatformDataIntegrityException(REPORTING_INVALID_OUTPUT_TYPE_CODE, "No matching Output Type: " + outputType);
            }
        } catch (final ResourceException | IOException | ReportProcessingException e) {
            throw new PlatformDataIntegrityException(REPORTING_ERROR_MSG_CODE, e.getMessage());
        }
    }

    @Override
    public ByteArrayOutputStream generateReportAsOutputStream(String reportName,
                                                              Map<String, String> queryParams, AppUser currentUser, Locale locale) {

        String outputType = "HTML";
        String outputTypeParam = queryParams.get("output-type");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (StringUtils.isNotBlank(outputTypeParam)) {
            outputType = outputTypeParam;
        }
        this.createReport(reportName, byteArrayOutputStream, outputType, queryParams, currentUser, locale);
        return byteArrayOutputStream;
    }

    /**
     * only allow integer, long, date and string parameter types and
     * assume all mandatory - could go more detailed like Pawel did in
     * Mifos later and could match incoming and pentaho parameters
     * better... currently assuming they come in ok... and if not an
     * error
     *
     * @param report
     * @param queryParams
     */
    private void addParametersToReport(final MasterReport report, final Map<String, String> queryParams, AppUser currentUser) {

        final ReportParameterValues rptParamValues = report.getParameterValues();
        final ReportParameterDefinition paramsDefinition = report.getParameterDefinition();

        for (final ParameterDefinitionEntry paramDefEntry : paramsDefinition.getParameterDefinitions()) {
            final String paramName = paramDefEntry.getName();
            if (!((paramName.equals("tenantUrl")) || (paramName.equals("userhierarchy") || (paramName.equals("username")) || (paramName
                    .equals("password") || (paramName.equals("userid")))))) {
                LOGGER.info("paramName: {}", paramName);
                final String pValue = queryParams.get(paramName);
                if (StringUtils.isBlank(pValue)) {
                    throw new PlatformDataIntegrityException(REPORTING_ERROR_MSG_CODE, "Pentaho Parameter: " + paramName + " - not Provided");
                }
                final Class<?> clazz = paramDefEntry.getValueType();
                LOGGER.info("addParametersToReport({} : {}: {})", paramName, pValue, clazz.getCanonicalName());
                if (clazz.getCanonicalName().equalsIgnoreCase("java.lang.Integer")) {
                    rptParamValues.put(paramName, Integer.parseInt(pValue));
                } else if (clazz.getCanonicalName().equalsIgnoreCase("java.lang.Long")) {
                    rptParamValues.put(paramName, Long.parseLong(pValue));
                } else if (clazz.getCanonicalName().equalsIgnoreCase("java.sql.Date")) {
                    rptParamValues.put(paramName, Date.valueOf(pValue));
                } else {
                    rptParamValues.put(paramName, pValue);
                }
            }
        }
        String reportDbUrl = fineractProperties.getReportDbURl();
        String userHierarchy = currentUser.getOffice().getHierarchy();
        LOGGER.info("db URL: {}; userhierarchy: {}", reportDbUrl, userHierarchy);
        rptParamValues.put("userhierarchy", userHierarchy);
        final Long userId = currentUser.getId();
        LOGGER.info("db URL: {}; userid: {}", reportDbUrl, userId);
        rptParamValues.put("userid", userId);
        rptParamValues.put("tenantUrl", reportDbUrl);
        rptParamValues.put("username", fineractProperties.getReportDbUsername());
        rptParamValues.put("password", fineractProperties.getReportDbPassword());
    }

    private Map<String, String> getReportParams(final MultivaluedMap<String, String> queryParams) {

        final Map<String, String> reportParams = new HashMap<>();
        final Set<String> keys = queryParams.keySet();
        String pKey;
        String pValue;
        for (final String k : keys) {
            if (k.startsWith("R_")) {
                pKey = k.substring(2);
                pValue = queryParams.get(k).get(0);
                reportParams.put(pKey, pValue);
            }
        }
        return reportParams;
    }
}