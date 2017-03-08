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

package com.phenixp2p.demo.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public final class Utilities {
  private static final String TAG = Utilities.class.getSimpleName();

  public static boolean hasInternet(Context context) {
    if (context == null) {
      return false;
    }
    ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivityManager != null) {
      NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
      return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
    return false;
  }

  public static void handleException(Activity activity, Exception e) {
    if (Fabric.isInitialized()) {
      Crashlytics.log(Log.ERROR, TAG, e.getMessage());
    }
    DialogUtils.showToast(activity, e.getMessage());
  }
}