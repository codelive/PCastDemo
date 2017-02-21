/*
 * Copyright 2016 PhenixP2P Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phenixp2p.demo;

import android.os.AsyncTask;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

import io.fabric.sdk.android.Fabric;

import static com.phenixp2p.demo.Constants.APP_TAG;
import static com.phenixp2p.demo.Constants.NUM_HTTP_RETRIES;

public final class HttpTask<TRequest, TResponse> {
  private static final String TAG = HttpTask.class.getSimpleName();

  public enum Method {
    POST,
    PUT
  }

  public interface Callback<TResponse> {
    void onResponse(TResponse result);
    void onError(Exception e);
  }

  private final Callback<TResponse> callback;
  private final String path;
  private final Method method;
  private final TRequest request;
  private final Class<TResponse> classResponse;

  public HttpTask(Callback<TResponse> callback,
                  String path,
                  Method method,
                  TRequest request,
                  Class<TResponse> classResponse) {
    this.callback = callback;
    this.path = path;
    this.method = method;
    this.request = request;
    this.classResponse = classResponse;
  }

  public final void execute(ExecutorService executorService) {
    this.asyncTask.executeOnExecutor(executorService, null, null, null);
  }

  private static String convertStreamToString(java.io.InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  private class HttpResponse {
    TResponse response;
    boolean success = true;
    HttpResponse() {
      this.success = false;
    }
    HttpResponse(TResponse response) {
      this.response = response;
    }
  }

  private final AsyncTask<Void, Void, HttpResponse> asyncTask = new AsyncTask<Void, Void, HttpResponse>() {

    @Override
    protected final HttpResponse doInBackground(Void... voids) {
      HttpURLConnection urlConnection = null;
      int countRetry = 0;
      while (countRetry < NUM_HTTP_RETRIES) {
        try {
          Log.d(APP_TAG, "HTTP [" + HttpTask.this.method +"] ["+ HttpTask.this.path +"]");
          URL url = new URL(HttpTask.this.path);
          urlConnection = (HttpURLConnection) url.openConnection();
          urlConnection.setRequestMethod(HttpTask.this.method.toString());

          String requestAsString = new Gson().toJson(HttpTask.this.request);
          byte[] postData = requestAsString.getBytes(StandardCharsets.UTF_8);
          urlConnection.setRequestProperty("content-type", "application/json");
          urlConnection.setRequestProperty("content-length", Integer.toString(postData.length));
          urlConnection.setUseCaches(false);
          Log.d(TAG, "[" + HttpTask.this.path + "] request");
          try (DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream())) {
            outputStream.write(postData);
          }
          try (InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream())) {
            String result = HttpTask.convertStreamToString(inputStream);
            TResponse response = new Gson().fromJson(result, HttpTask.this.classResponse);
            return new HttpResponse(response);
          }
        } catch (Exception e) {
          HttpTask.this.callback.onError(e);
          if (Fabric.isInitialized()) {
            Crashlytics.log(Log.ERROR, TAG, e.getMessage());
          }
        } finally {
          if (urlConnection != null) {
            urlConnection.disconnect();
          }
        }
        countRetry++;
      }
      Log.d(TAG, "[" + HttpTask.this.path + "] response");
      return new HttpResponse();
    }

    @Override
    protected void onPostExecute(HttpResponse result) {
      if (result.success) {
        HttpTask.this.callback.onResponse(result.response);
      }
    }
  };
}