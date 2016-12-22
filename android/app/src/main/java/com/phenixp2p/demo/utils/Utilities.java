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

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class Utilities {
  private static Toast toast;

  public static boolean hasInternet(Context context) {
    return context != null && isConnected(context);
  }

  private static boolean isConnected(Context context) {
    if (context == null) {
      return false;
    }
    ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    if (connectivityManager != null) {
      NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
      return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
    return false;
  }

  public static String getStreamId(String streamId) {
    return streamId.substring(0, streamId.indexOf("#") + 1).concat("...").concat(streamId.substring(streamId.length() - 4, streamId.length()));
  }

  public static boolean isEquals(String a, String b) {
    return ((a == null && b == null) || (a != null && a.equals(b)));
  }

  public static void showToast(final Activity activity, final String str) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (toast != null) {
          toast.cancel();
        }
        toast = Toast.makeText(activity, str, Toast.LENGTH_LONG);
        toast.show();
      }
    });
  }

  public static void handleException(Activity activity, Exception e) {
    showToast(activity, e.getMessage());
  }

  public static int getHeight(Activity activity) {
    DisplayMetrics displaymetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
    return displaymetrics.heightPixels;
  }

  public static int getWidth(Activity activity) {
    DisplayMetrics displaymetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
    return displaymetrics.widthPixels;
  }

  public static void showDialog(String title, String message, final ActionDialog actionDialog) {
    if (actionDialog.getActivity() != null) {
      new AlertDialog.Builder(actionDialog.getActivity()).setTitle(title)
      .setMessage(message)
      .setCancelable(false)
      .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          actionDialog.buttonYes();
          dialogInterface.dismiss();
        }
      }).show();
    }
  }

  public interface ActionDialog {
    Activity getActivity();
    void buttonYes();
  }

  public static void setPreviewDimensions(Activity activity, RelativeLayout view) {
    int heightPixels = ((getHeight(activity) * 30 / 100));
    int widthPixels = ((getWidth(activity) * 30 / 100));
    RelativeLayout.LayoutParams rel_view = new RelativeLayout.LayoutParams(
    widthPixels, heightPixels);
    rel_view.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    rel_view.addRule(RelativeLayout.ALIGN_PARENT_END);
    view.setLayoutParams(rel_view);
  }

  public static void animateView(ImageView imageView) {
    ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(imageView,
    PropertyValuesHolder.ofFloat("scaleX", 1.2f),
    PropertyValuesHolder.ofFloat("scaleY", 1.2f));
    scaleDown.setDuration(310);
    scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
    scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
    scaleDown.start();
  }
}
