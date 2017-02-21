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

package com.phenixp2p.demo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceView;

public final class RoundedRectSurfaceView extends SurfaceView {

  private final static int MAX_WIDTH = 1000;
  private final static int ROUNDING_RADIUS = 15;

  public RoundedRectSurfaceView(Context context) {
    super(context);
  }

  public RoundedRectSurfaceView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public RoundedRectSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    int count = canvas.save();
    Path rectPath = new Path();
    int width = this.getWidth();
    int height = this.getHeight();
    if (width >= MAX_WIDTH) {
      rectPath.addRoundRect(new RectF(0, 0, width, height), 0, 0, Path.Direction.CW);
    } else {
      rectPath.addRoundRect(new RectF(0, 0, width, height), ROUNDING_RADIUS, ROUNDING_RADIUS, Path.Direction.CW);
    }
    canvas.clipPath(rectPath);
    super.dispatchDraw(canvas);
    canvas.restoreToCount(count);
  }
}
