/**
 * Copyright 2016 PhenixP2P Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.phenixp2p.demo;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HTTPtask extends AsyncTask<String, String, JSONObject> {

  final static String TAG = "HTTPtask";

  public interface Caller {
    void callback(JSONObject result);
  }

  private HttpURLConnection urlConnection;
  private final Caller caller;
  private final String path;
  private final JSONObject postJson;

  public HTTPtask(String path, JSONObject postJson, Caller caller) {
    this.caller = caller;
    this.path = path;
    this.postJson = postJson;
  }

  @Override
  protected JSONObject doInBackground(String... args) {
    StringBuilder result = new StringBuilder();
    JSONObject json = null;

    try {
      URL url = new URL(this.path);
      urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setRequestMethod("POST");

      String urlParameters = this.postJson.toString();
      byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
      urlConnection.setRequestProperty("Content-Type", "application/json");
      urlConnection.setRequestProperty("Content-Length", Integer.toString( postData.length));
      urlConnection.setUseCaches(false);

      Log.d(TAG, this.path + " request");

      try (DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream())) {
        outputStream.write(postData);
      }

      InputStream in = new BufferedInputStream(urlConnection.getInputStream());
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line);
      }
      json = new JSONObject(result.toString());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      urlConnection.disconnect();
    }

    Log.d(TAG, this.path + " response");

    return json;
  }

  @Override
  protected void onPostExecute(JSONObject json) {
    this.caller.callback(json);
  }
}
