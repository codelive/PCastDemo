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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.phenixp2p.demo.R;

public final class ArcImage extends View {
  private int colorSeek;
  private int colorShadow;
  private int widthSeek;
  private int widthShadow;
  private int padding;
  private int process;
  private Paint paint;
  private Bitmap bitmapStart;
  private Bitmap bitmapEnd;
  private Bitmap bitmapTop;
  private int startAngle;
  private int angle;
  private int sizeIcon;
  private RectF rectangle;

  private boolean typeNormal;

  public ArcImage(Context context, AttributeSet attrs) {
    super(context, attrs);
    inits(context, attrs, 0);
  }

  public ArcImage(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    inits(context, attrs, defStyleAttr);
  }

  public void setSizeIcon(int sizeIcon) {
    this.sizeIcon = sizeIcon;
    reloadBitmapIcon();
    invalidate();
  }

  public void setWidthSeek(int widthSeek) {
    this.widthSeek = widthSeek;
    widthShadow = (int) (1.8f * widthSeek);
    invalidate();
  }

  private void inits(Context context, AttributeSet attrs, int defStyleAttr) {
    TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ArcImage, defStyleAttr, 0);
    colorSeek = typedArray.getColor(R.styleable.ArcImage_color_seek, ContextCompat.getColor(context, R.color.blue));
    colorShadow = typedArray.getColor(R.styleable.ArcImage_color_shadow, ContextCompat.getColor(context, R.color.blue));
    widthSeek = typedArray.getDimensionPixelSize(R.styleable.ArcImage_width_seek, 10);
    widthShadow = typedArray.getDimensionPixelSize(R.styleable.ArcImage_width_shadow, 18);
    padding = typedArray.getDimensionPixelSize(R.styleable.ArcImage_padding, 50);
    process = typedArray.getInt(R.styleable.ArcImage_process, 0);
    sizeIcon = typedArray.getDimensionPixelSize(R.styleable.ArcImage_size_icon, getResources().getDimensionPixelSize(R.dimen.size_default_icon));

    resStart = typedArray.getResourceId(R.styleable.ArcImage_icon_start, R.mipmap.ic_user);
    resEnd = typedArray.getResourceId(R.styleable.ArcImage_icon_end, R.mipmap.ic_user);
    resTop = typedArray.getResourceId(R.styleable.ArcImage_icon_top, R.mipmap.ic_cloud);
    reloadBitmapIcon();
    startAngle = - typedArray.getInt(R.styleable.ArcImage_start_angle, 30);
    angle = - typedArray.getInt(R.styleable.ArcImage_angle, 140);
    typeNormal = typedArray.getBoolean(R.styleable.ArcImage_type_draw_nomal, true);
    typedArray.recycle();

    paint = new Paint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setAntiAlias(true);
    rectangle = new RectF();
  }

  private int resStart, resEnd, resTop;

  private void reloadBitmapIcon() {
    Bitmap temBitmap = BitmapFactory.decodeResource(getResources(), resStart);
    bitmapStart = Bitmap.createBitmap(sizeIcon, sizeIcon, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmapStart);
    canvas.drawBitmap(temBitmap, new Rect(0, 0, temBitmap.getWidth(), temBitmap.getHeight()),
      new RectF(0, 0, sizeIcon, sizeIcon), null);
    temBitmap.recycle();

    Bitmap temEnd = BitmapFactory.decodeResource(getResources(), resEnd);
    bitmapEnd = Bitmap.createBitmap(sizeIcon, sizeIcon, Bitmap.Config.ARGB_8888);
    Canvas canvas1 = new Canvas(bitmapEnd);
    canvas1.drawBitmap(temEnd, new Rect(0, 0, temEnd.getWidth(), temEnd.getHeight()),
      new RectF(0, 0, sizeIcon, sizeIcon), null);
    temEnd.recycle();

    Bitmap temTop = BitmapFactory.decodeResource(getResources(), resTop);
    bitmapTop = Bitmap.createBitmap(sizeIcon, sizeIcon, Bitmap.Config.ARGB_8888);
    Canvas canvas2 = new Canvas(bitmapTop);
    canvas2.drawBitmap(temTop, new Rect(0, 0, temTop.getWidth(), temTop.getHeight()),
      new RectF(0, 0, sizeIcon, sizeIcon), null);
    temTop.recycle();
  }

  public void setProcess(int process) {
    this.process = process;
    invalidate();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    Matrix matrix = new Matrix();
    matrix.setScale(- 1, 1, w / 2, (h - padding) * 3 + padding * 2);
    rectangle.set(padding, padding, getWidth() - padding, (getHeight() - padding) * 3 + padding);
  }

  public void setTypeNomal(boolean typeNomal) {
    this.typeNormal = typeNomal;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    paint.setColor(colorShadow);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(widthShadow);
    int start, currentEn;
    if (typeNormal) {
       currentEn = process * angle / 100;
       start = 180 + startAngle + currentEn;
    } else {
      currentEn = -process * angle/ 200;
      start = 90;
    }
    canvas.drawArc(rectangle, - start, currentEn, false, paint);
    paint.setColor(colorSeek);
    paint.setStrokeWidth(widthSeek);
    canvas.drawArc(rectangle, - start, currentEn, false, paint);
    canvas.drawBitmap(bitmapStart, padding - bitmapStart.getWidth() / 2, getHeight() - bitmapStart.getHeight(), null);
    canvas.drawBitmap(bitmapEnd, getWidth() - padding - bitmapEnd.getWidth() / 2, getHeight() - bitmapEnd.getHeight(), null);
    if (widthSeek >= 10) {
      canvas.drawBitmap(bitmapTop, getWidth() / 2 - bitmapTop.getWidth() / 2, 0, null);
    } else {
      canvas.drawBitmap(bitmapTop, getWidth() / 2 - bitmapTop.getWidth() / 2, padding / 2, null);
    }
  }
}
