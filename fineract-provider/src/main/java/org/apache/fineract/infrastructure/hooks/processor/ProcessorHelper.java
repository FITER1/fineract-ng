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

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Slf4j
public class ProcessorHelper {

	@SuppressWarnings("null")
	public static OkHttpClient configureClient(final OkHttpClient client) {
		final TrustManager[] certs = new TrustManager[] {
			new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
				}

				@Override
				public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
				}
			}
		};

		SSLContext ctx = null;
		try {
			ctx = SSLContext.getInstance("TLS");
			ctx.init(null, certs, new SecureRandom());
		} catch (final java.security.GeneralSecurityException ex) {
		}

		try {
			final HostnameVerifier hostnameVerifier = (hostname, session) -> true;
			// TODO: fix this
			// client.hostnameVerifier(hostnameVerifier);
			// client.sslSocketFactory(ctx.getSocketFactory());
		} catch (final Exception e) {
		}

		return client;
	}

	public static OkHttpClient createClient() {
		// TODO: @aleks remove extensive request logging
		HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(log::debug);
		interceptor.level(HttpLoggingInterceptor.Level.BODY);

		OkHttpClient.Builder okClient = new OkHttpClient.Builder();
		okClient.addInterceptor(interceptor);

		return configureClient(okClient.build());
	}

	@SuppressWarnings("rawtypes")
	public static Callback createCallback(final String url) {

		return new Callback() {
			@Override
			public void onResponse(Call call, Response response) {
				if(response.code() < 400) {
					log.info("URL : " + url + "\tStatus : " + response.code());
				} else {
					log.error("URL : " + url + "\tStatus : " + response.code());
				}
			}

			@Override
			public void onFailure(Call call, Throwable t) {
				log.error("URL : " + call.request().url() + "\tMessage : " + t.getMessage());
			}
		};
	}

	public static WebHookService createWebHookService(final String url) {

		final OkHttpClient client = ProcessorHelper.createClient();

		Retrofit.Builder builder = new Retrofit.Builder()
			.baseUrl(url)
			.client(client)
			.addConverterFactory(GsonConverterFactory.create())
			.addConverterFactory(JacksonConverterFactory.create())
		;

		Retrofit retrofit = builder.build();

		return retrofit.create(WebHookService.class);
	}

}