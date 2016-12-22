/*
 * Copyright (c) 2016. PhenixP2P Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0(the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.phenixp2p.demo.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.phenixp2p.demo.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiFactory {
  public static final String ENDPOINT = "https://demo.phenixp2p.com/demoApp/";
  private static OkHttpClient sOkHttpClient;
  private static Retrofit sRetrofit;
  private static ApiServices sApiService;

  public static ApiServices getApiService() {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY
    : HttpLoggingInterceptor.Level.NONE);

    if (sOkHttpClient == null) {
      sOkHttpClient = new OkHttpClient.Builder()
      .addInterceptor(new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
          Request original = chain.request();

          Request.Builder requestBuilder = original.newBuilder()
          .addHeader("Content-Type", "application/json");

          Request request = requestBuilder.build();

          return chain.proceed(request);
        }
      })
      .addInterceptor(logging)
      .build();
    }

    if (sRetrofit == null) {
      Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();

      sRetrofit = new Retrofit.Builder()
      .baseUrl(ENDPOINT)
      .client(sOkHttpClient)
      .addConverterFactory(GsonConverterFactory.create(gson))
      .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
      .build();
    }

    if (sApiService == null) {
      sApiService = sRetrofit.create(ApiServices.class);
    }
    return sApiService;
  }
}
