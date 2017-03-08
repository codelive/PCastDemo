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
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.phenixp2p.demo.R;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public final class LayoutUtils {
  private static final String TAG = LayoutUtils.class.getSimpleName();

  private static int getHeight(Activity activity) {
    DisplayMetrics displaymetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
    return displaymetrics.heightPixels;
  }

  private static int getWidth(Activity activity) {
    DisplayMetrics displaymetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
    return displaymetrics.widthPixels;
  }

  public static void setPreviewDimensions(Activity activity, ViewGroup view) {
    int heightPixels = ((getHeight(activity) * 30 / 100));
    int widthPixels = ((getWidth(activity) * 30 / 100));
    RelativeLayout.LayoutParams rel_view = new RelativeLayout.LayoutParams(
    widthPixels, heightPixels);
    rel_view.setMargins(16, 0, 16, 10);
    rel_view.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    rel_view.addRule(RelativeLayout.ALIGN_PARENT_END);
    view.setLayoutParams(rel_view);
  }

  public static void setPreviewDimensionsTablet(Activity activity, ViewGroup view) {
    int heightPixels = ((getHeight(activity) * 40 / 100));
    int widthPixels = ((getWidth(activity) * 40 / 100));
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

  public static void setLayoutTablet(ViewGroup view, boolean isPortrait) {
    RelativeLayout.LayoutParams rel_view = new RelativeLayout.LayoutParams(
    WRAP_CONTENT, WRAP_CONTENT);
    if (isPortrait) {
      rel_view.addRule(RelativeLayout.ABOVE, R.id.draggableView);
    } else {
      rel_view.addRule(RelativeLayout.LEFT_OF, R.id.draggableView);
    }
    rel_view.setMargins(24, 24, 24, 24);
    view.setLayoutParams(rel_view);
  }

  public static int dpToPx(Activity activity, int dp) {
    DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
    return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
  }

  public static void setLayout(Activity activity,
                               boolean isLandscape,
                               View buttonAudio,
                               View buttonVideo,
                               View buttonVideoAudio,
                               View buttonShareScreen,
                               ImageView imageAudio,
                               ImageView imageVideo) {
    RelativeLayout.LayoutParams audio = new RelativeLayout.LayoutParams(
    isLandscape ? dpToPx(activity, 38) : WRAP_CONTENT, isLandscape ? dpToPx(activity, 38) : WRAP_CONTENT);
    if (isLandscape) {
      audio.setMargins(dpToPx(activity, 38), dpToPx(activity, 12), 0, 0);
      audio.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
      audio.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    } else {
      audio.setMargins(0, 0, 0, dpToPx(activity, 8));
    }
    audio.addRule(RelativeLayout.CENTER_HORIZONTAL);
    buttonAudio.setLayoutParams(audio);

    RelativeLayout.LayoutParams video = new RelativeLayout.LayoutParams(
      isLandscape ? dpToPx(activity, 38) : WRAP_CONTENT, isLandscape ? dpToPx(activity, 38) : WRAP_CONTENT);
    if (isLandscape) {
      video.setMargins(0, dpToPx(activity, 12), dpToPx(activity, 26), 0);
      video.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      video.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    } else {
      video.setMargins(0, 0, 0, dpToPx(activity, 8));
      video.addRule(RelativeLayout.BELOW, R.id.audio);
    }
    video.addRule(RelativeLayout.CENTER_HORIZONTAL);
    buttonVideo.setLayoutParams(video);

    RelativeLayout.LayoutParams play = new RelativeLayout.LayoutParams(
      WRAP_CONTENT, WRAP_CONTENT);
    if (isLandscape) {
      play.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
      play.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
      play.setMargins(dpToPx(activity, 20), 0, 0, dpToPx(activity, 12));
    } else {
      play.addRule(RelativeLayout.BELOW, R.id.video);
      play.setMargins(0, 0, 0, dpToPx(activity, 8));
    }
    play.addRule(RelativeLayout.CENTER_HORIZONTAL);
    buttonVideoAudio.setLayoutParams(play);

    LinearLayout.LayoutParams agen = new LinearLayout.LayoutParams(
      isLandscape ? dpToPx(activity, 38) : WRAP_CONTENT, isLandscape ? dpToPx(activity, 38) : WRAP_CONTENT);
    imageAudio.setLayoutParams(agen);
    imageVideo.setLayoutParams(agen);

    RelativeLayout.LayoutParams share = new RelativeLayout.LayoutParams(
      isLandscape ? dpToPx(activity, 28) : WRAP_CONTENT, isLandscape ? dpToPx(activity, 28) : WRAP_CONTENT);
    if (isLandscape) {
      share.setMargins(0, 0, dpToPx(activity, 32), dpToPx(activity, 14));
      share.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
      share.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    } else {
      share.setMargins(0, 0, 0, 0);
      share.addRule(RelativeLayout.BELOW, R.id.play);
    }
    share.addRule(RelativeLayout.CENTER_HORIZONTAL);
    buttonShareScreen.setLayoutParams(share);
  }
}
