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

package com.phenixrts.demo;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static com.phenixrts.demo.Constants.CREATE_SCREEN_CAPTURE;

public final class CaptureHelper {
  private static final String TAG = CaptureHelper.class.getSimpleName();

  public static void fireScreenCaptureIntent(Activity activity) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      MediaProjectionManager manager;
      manager = (MediaProjectionManager) activity.getSystemService(MEDIA_PROJECTION_SERVICE);
      ((PhenixApplication) activity.getApplication()).setProjectionManager(manager);
      activity.startActivityForResult(manager.createScreenCaptureIntent(), CREATE_SCREEN_CAPTURE);
    }
  }

  public static boolean handleActivityResult(Activity activity,
                                             int requestCode,
                                             int resultCode,
                                             Intent data) {
    if (requestCode != CREATE_SCREEN_CAPTURE) {
      return false;
    }

    if (resultCode == Activity.RESULT_OK) {
      activity.startService(PhenixService.newIntent(activity, resultCode, data));
    } else {
      if (Fabric.isInitialized()) {
        Crashlytics.log(Log.DEBUG, TAG, "Failed to acquire permission for screen capture.");
      }
    }
    return true;
  }
}
