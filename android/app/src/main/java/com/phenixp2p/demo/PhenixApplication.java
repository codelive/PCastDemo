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

package com.phenixp2p.demo;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.phenixp2p.pcast.PCast;

import io.fabric.sdk.android.Fabric;

import static com.phenixp2p.demo.Constants.APP_TAG;

public final class PhenixApplication extends Application {
  private MediaProjectionManager projectionManager = null;
  private boolean isStopPublish = false;
  private boolean isLandscape;
  private boolean isShare;
  private boolean isBackground;
  private PCast pCast;
  private int positionUriMenu;
  private String serverAddress;
  private String pcastAddress;

  @Override
  public void onCreate() {
    super.onCreate();
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      Log.d(APP_TAG, "version [" + pInfo.versionName +"] ["+ pInfo.versionCode + "]");
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    // Initialize crash analytics by Fabric
    Fabric.with(this, new Crashlytics(), new Answers());
  }

  public MediaProjectionManager getProjectionManager() {
    return this.projectionManager;
  }

  public void setProjectionManager(MediaProjectionManager projectionManager) {
    this.projectionManager = projectionManager;
  }

  public boolean isStopPublish() {
    return this.isStopPublish;
  }

  public void setStopPublish(boolean stopPublish) {
    this.isStopPublish = stopPublish;
  }

  public boolean isLandscape() {
    return this.isLandscape;
  }

  public void setLandscape(boolean isLandscape) {
    this.isLandscape = isLandscape;
  }

  public boolean isShare() {
    return this.isShare;
  }

  public void setShare(boolean share) {
    this.isShare = share;
  }

  public PCast getPCast() {
    return this.pCast;
  }

  public void setPCast(PCast pCast) {
    this.pCast = pCast;
  }

  public boolean isBackground() {
    return this.isBackground;
  }

  public void setBackground(boolean background) {
    this.isBackground = background;
  }

  public int getPositionUriMenu() {
    return this.positionUriMenu;
  }

  public void setPositionUriMenu(int positionUriMenu) {
    this.positionUriMenu = positionUriMenu;
  }

  public String getServerAddress() {
    return this.serverAddress != null ? this.serverAddress : ServerAddress.PRODUCTION_ENDPOINT.getServerAddress();
  }

  public void setServerAddress(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  public String getPcastAddress() {
    return this.pcastAddress;
  }

  public void setPcastAddress(String pcastAddress) {
    this.pcastAddress = pcastAddress;
  }
}
