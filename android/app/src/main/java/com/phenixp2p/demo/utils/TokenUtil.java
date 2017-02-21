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

package com.phenixp2p.demo.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import static com.phenixp2p.demo.Constants.SESSION_ID;
import static com.phenixp2p.demo.Constants.STREAM_ID_FROM_LIST;

public final class TokenUtil {
  public static String getStreamId(String streamId) {
    return streamId.substring(0,
            streamId.indexOf("#") + 1).concat("...").concat(streamId.substring(streamId.length() - 4,
            streamId.length()));
  }

  public static void saveSessionIdIntoLocal(Context context, String sessionId) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(SESSION_ID, sessionId).apply();
  }

  public static String getSessionIdLocal(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(SESSION_ID, null);
  }

  public static void saveStreamIdIntoLocal(Context context, String streamId) {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(STREAM_ID_FROM_LIST, streamId).apply();
  }

  public static String getStreamIdLocal(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).getString(STREAM_ID_FROM_LIST, null);
  }
}
