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

package com.phenixrts.demo.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public final class DialogUtil {
  private static final String TAG = DialogUtil.class.getSimpleName();
  private static Toast toast;

  public static void showDialog(String title, String message, final ActionDialog actionDialog) {
    if (actionDialog.getContext() != null && !actionDialog.getContext().isFinishing()) {
      AlertDialog.Builder builder = new AlertDialog.Builder((actionDialog.getContext()));
      builder.setTitle(title);
      builder.setMessage(message);
      builder.setCancelable(false);
      builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          actionDialog.buttonYes();
          dialog.dismiss();
        }
      });
      AlertDialog alertDialog = builder.create();
      alertDialog.show();
      actionDialog.autoDismiss(alertDialog);
    }
  }

  public interface ActionDialog {
    AppCompatActivity getContext();
    void buttonYes();
    void autoDismiss(AlertDialog alertDialog);
  }

  static void showToast(final Activity activity, final String str) {
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
}
