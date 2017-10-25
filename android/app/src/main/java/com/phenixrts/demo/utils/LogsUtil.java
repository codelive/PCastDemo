/**
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

package com.phenixrts.demo.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.phenixrts.demo.Constants.APP_TAG;

public final class LogsUtil {
  public static String currentDate() {
    final Calendar calendar = Calendar.getInstance();
    final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd-yyyy hh:mm:ss", Locale.getDefault());
    return dateFormat.format(calendar.getTime());
  }

  public static File generateLog() {
    final File logFolder = new File(Environment.getExternalStorageDirectory(), "PCastDemo");
    if (!logFolder.exists()) {
      if (!logFolder.mkdirs()) {
        Log.w(APP_TAG, "Failed to create log output folder");
        return null;
      }
    }
    final String filename = "PCastDemo_log_" + new Date().getTime() + ".log";
    final File logFile = new File(logFolder, filename);
    try {
      final String[] command = new String[]{"logcat", "-f", logFile.getAbsolutePath(), "-v", "time", "ActivityManager:W", "PCastDemo"};
      Runtime.getRuntime().exec(command);
      return logFile;
    } catch (IOException ioEx) {
      Log.w(APP_TAG, ioEx.getMessage());
    }
    return null;
  }
}
