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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.phenixp2p.demo.events.Events;
import com.phenixp2p.demo.ui.activities.MainActivity;
import com.phenixp2p.pcast.android.AndroidPCastFactory;

import io.fabric.sdk.android.Fabric;

import static android.app.Notification.PRIORITY_MIN;
import static com.phenixp2p.demo.Constants.EXTRA_DATA;
import static com.phenixp2p.demo.Constants.EXTRA_RESULT_CODE;
import static com.phenixp2p.demo.Constants.IS_SHARE_SCREEN;
import static com.phenixp2p.demo.Constants.NOTIFICATION_ID;

public final class PhenixService extends Service {
  private static final String TAG = PhenixService.class.getSimpleName();
  private static boolean running;
  private ShareScreenSession shareScreenSession;

  static Intent newIntent(Context context, int resultCode, Intent data) {
    Intent intent = new Intent(context, PhenixService.class);
    intent.putExtra(EXTRA_RESULT_CODE, resultCode);
    intent.putExtra(EXTRA_DATA, data);
    return intent;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private final ShareScreenSession.IListener listener = new ShareScreenSession.IListener() {

    @Override
    public void onStart(Context context, MediaProjection projection) {
      Intent showMain = new Intent(context, MainActivity.class);
      showMain.putExtra(IS_SHARE_SCREEN, running);

      int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
      PendingIntent pendingIntent = PendingIntent.getActivity(context, uniqueInt, showMain, PendingIntent.FLAG_UPDATE_CURRENT);
      String title = context.getString(R.string.notification_share_title);
      String subtitle = context.getString(R.string.notification_share_subtitle);
      Notification notification = new Notification.Builder(context) //
        .setContentTitle(title)
        .setContentText(subtitle)
        .setSmallIcon(R.drawable.ic_share_src)
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(PRIORITY_MIN)
        .build();
      startForeground(NOTIFICATION_ID, notification);
      AndroidPCastFactory.setMediaProjection(projection);
      RxBus.getInstance().post(new Events.OnShareScreen(true));
    }

    @Override
    public void onStop() {
      running = false;
    }
  };

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
    if (running) {
      return START_NOT_STICKY;
    }
    running = true;

    int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
    Intent data = intent.getParcelableExtra(EXTRA_DATA);
    if (resultCode == 0 || data == null) {
      if (Fabric.isInitialized()) {
        Crashlytics.log(Log.ERROR, TAG, "Result code or data missing.");
      }
    }

    this.shareScreenSession =
      new ShareScreenSession(this, this.listener, resultCode, data);
    this.shareScreenSession.startShareScreen();
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    this.shareScreenSession.destroy();
    running = false;
    PhenixApplication phenixApplication = (PhenixApplication)getApplication();
    phenixApplication.setShare(false);
    phenixApplication.setBackground(false);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(@NonNull Intent intent) {
    if (Fabric.isInitialized()) {
      Crashlytics.log(Log.ERROR, TAG, "Not supported.");
    }
    throw new AssertionError("Not supported.");
  }

  public static boolean isReady() {
    return running;
  }
}
