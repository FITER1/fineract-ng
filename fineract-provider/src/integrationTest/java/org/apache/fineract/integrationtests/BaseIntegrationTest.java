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

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import java.io.File;
import java.time.Duration;

public abstract class BaseIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);

    @ClassRule
    public static DockerComposeContainer fineract = new DockerComposeContainer(new File("src/integrationTest/resources/compose-test.yml"))
        .withExposedService("mysql_1", 3306)
        .withExposedService("fineract_1", 8443, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(120)))
        .withLocalCompose(true)
        .withLogConsumer("fineract_1", new Slf4jLogConsumer(log))
        // .waitingFor("fineract_1", Wait.forHttps("/api/v1/jobs").forPort(8443).withBasicCredentials("mifos", "password"))
        // .waitingFor("mysql_1", new WaitAllStrategy())
        // .waitingFor("fineract_1", new WaitAllStrategy())
    ;

    protected ResponseSpecification responseSpec;
    protected RequestSpecification requestSpec;

    @Before
    public void setup() {
        SSLConfig config = new SSLConfig()
            .with().keystore("src/main/resources/keystore.jks", "openmf")
            .and()
            .allowAllHostnames()
            .and()
            .relaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(config);
        RestAssured.port = fineract.getServicePort("fineract_1", 8443);
        RestAssured.baseURI = String.format("https://%s:%s/api/v1", fineract.getServiceHost("fineract_1", 8443), RestAssured.port);

        log.info("BASE URI: {}", RestAssured.baseURI);

        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", String.format("Basic %s", "bWlmb3M6cGFzc3dvcmQ=" /*Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey()*/));
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
    }
}
