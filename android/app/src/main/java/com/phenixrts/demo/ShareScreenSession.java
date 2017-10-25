/*
 * Copyright (c) 2016. PhenixP2P Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0(the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phenixrts.demo;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

final class ShareScreenSession {
  private final Context context;
  private final IListener listener;
  private final int resultCode;
  private final Intent data;
  private final MediaProjectionManager projectionManager;
  private MediaProjection projection;
  private boolean running;
  private MediaProjectionCallback mediaProjectionCallback;

  ShareScreenSession(Context context, IListener listener, int resultCode, Intent data) {
    this.context = context;
    this.listener = listener;
    this.resultCode = resultCode;
    this.data = data;
    this.projectionManager = ((PhenixApplication) context.getApplicationContext()).getProjectionManager();
    ((PhenixApplication) context.getApplicationContext()).setProjectionManager(null);
  }

  void startShareScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      this.mediaProjectionCallback = new MediaProjectionCallback();
      this.projection = this.projectionManager.getMediaProjection(this.resultCode, this.data);
      this.projection.registerCallback(this.mediaProjectionCallback, null);
      this.listener.onStart(this.context, this.projection);
      this.running = true;
    }
  }

  private void stopShareScreen() {
    if (!this.running) {
      return;
    }
    this.running = false;
    try {
      // Stop the projection in order to flush everything to the recorder.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if (this.projection != null) {
          this.projection.unregisterCallback(this.mediaProjectionCallback);
          this.projection.stop();
          this.projection = null;
        }
      }
    } finally {
      this.listener.onStop();
    }
  }

  void destroy() {
    if (this.running) {
      stopShareScreen();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private class MediaProjectionCallback extends MediaProjection.Callback {
    @Override
    public void onStop() {
      destroy();
    }
  }

  interface IListener {
     // Invoked immediately prior to the start of share screen.
    void onStart(Context context, MediaProjection projection);

    // Invoked immediately after the end of share screen.
    void onStop();
  }
}
