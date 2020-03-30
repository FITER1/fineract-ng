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
package org.apache.fineract.infrastructure.hooks.processor;

import static org.apache.fineract.infrastructure.hooks.api.HookApiConstants.contentTypeName;
import static org.apache.fineract.infrastructure.hooks.api.HookApiConstants.payloadURLName;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.fineract.infrastructure.hooks.domain.Hook;
import org.apache.fineract.infrastructure.hooks.domain.HookConfiguration;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import retrofit2.Callback;

@Service
public class WebHookProcessor implements HookProcessor {

	@Override
	public void process(final Hook hook, @SuppressWarnings("unused") final AppUser appUser, final String payload, final String entityName,
						final String actionName, final String tenantIdentifier, final String authToken) {

		final Set<HookConfiguration> config = hook.getHookConfig();

		String url = "";
		String contentType = "";

		for (final HookConfiguration conf : config) {
			final String fieldName = conf.getFieldName();
			if (fieldName.equals(payloadURLName)) {
				url = conf.getFieldValue();
			}
			if (fieldName.equals(contentTypeName)) {
				contentType = conf.getFieldValue();
			}
		}

		sendRequest(url, contentType, payload, entityName, actionName, tenantIdentifier, authToken);

	}

	@SuppressWarnings("unchecked")
	private void sendRequest(final String url, final String contentType, final String payload, final String entityName,
							 final String actionName, final String tenantIdentifier, @SuppressWarnings("unused") final String authToken) {

		URL url1 = null;

		final String fineractEndpointUrl = System.getProperty("baseUrl");
		final WebHookService service = ProcessorHelper.createWebHookService(url);

		@SuppressWarnings("rawtypes")
		final Callback callback = ProcessorHelper.createCallback(url);

		if (contentType.equalsIgnoreCase("json") || contentType.contains("json")) {
			final JsonObject json = new JsonParser().parse(payload).getAsJsonObject();
			try {
				url1 = new URL(url);
				// Create a trust manager that does not validate certificate
				// chains
				TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(X509Certificate[] certs, String authType) {}

					public void checkServerTrusted(X509Certificate[] certs, String authType) {}
				} };
				// Install the all-trusting trust manager
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

				// Create all-trusting host name verifier
				HostnameVerifier allHostsValid = new HostnameVerifier() {

					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				};

				// Install the all-trusting host verifier
				HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
				HttpsURLConnection conn = (HttpsURLConnection) url1.openConnection();
				conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				OutputStream os = conn.getOutputStream();
				os.write(payload.getBytes("UTF-8"));
				InputStream in = new BufferedInputStream(conn.getInputStream());
			} catch (ClassCastException e) {
				try {
					url1 = new URL(url);
					HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
					conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
					conn.setRequestMethod("POST");
					conn.setDoOutput(true);
					OutputStream os = conn.getOutputStream();
					os.write(payload.getBytes("UTF-8"));
					InputStream in = new BufferedInputStream(conn.getInputStream());
				} catch (Exception eq) {
					e.printStackTrace();
				}
			}
			// service.sendJsonRequest(entityName, actionName, tenantIdentifier,
			// fineractEndpointUrl, "application/json", json, callback);
			catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyManagementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Map<String, String> map = new HashMap<>();
			map = new Gson().fromJson(payload, map.getClass());
			service.sendFormRequest(entityName, actionName, tenantIdentifier, fineractEndpointUrl, map, callback);
		}

	}

}
