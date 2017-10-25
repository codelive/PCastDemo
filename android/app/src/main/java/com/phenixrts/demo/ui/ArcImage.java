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
package com.phenixrts.demo.ui;

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

import com.phenixrts.demo.R;

public final class ArcImage extends View {
  private int colorSeek;
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
  private int resourceStart;
  private int resourceEnd;
  private int resourceTop;

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
    this.widthShadow = (int) (1.8f * widthSeek);
    invalidate();
  }

  public void setAngle(int angle) {
    this.angle = -angle;
    invalidate();
  }

  private void inits(Context context, AttributeSet attrs, int defStyleAttr) {
    TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ArcImage, defStyleAttr, 0);
    this.colorSeek = typedArray.getColor(R.styleable.ArcImage_color_seek, ContextCompat.getColor(context, R.color.blue));
    this.widthSeek = typedArray.getDimensionPixelSize(R.styleable.ArcImage_width_seek, 10);
    this.widthShadow = typedArray.getDimensionPixelSize(R.styleable.ArcImage_width_shadow, 18);
    this.padding = typedArray.getDimensionPixelSize(R.styleable.ArcImage_padding, 50);
    this.process = typedArray.getInt(R.styleable.ArcImage_process, 0);
    this.sizeIcon = typedArray.getDimensionPixelSize(R.styleable.ArcImage_size_icon, getResources().getDimensionPixelSize(
        com.phenixrts.demo.R.dimen.size_default_icon));

    this.resourceStart = typedArray.getResourceId(R.styleable.ArcImage_icon_start, R.mipmap.ic_user);
    this.resourceEnd = typedArray.getResourceId(R.styleable.ArcImage_icon_end, R.mipmap.ic_user);
    this.resourceTop = typedArray.getResourceId(R.styleable.ArcImage_icon_top, R.mipmap.ic_cloud);
    reloadBitmapIcon();
    this.startAngle = - typedArray.getInt(R.styleable.ArcImage_start_angle, 30);
    this.angle = - typedArray.getInt(R.styleable.ArcImage_angle, 140);
    this.typeNormal = typedArray.getBoolean(R.styleable.ArcImage_type_draw_nomal, true);
    typedArray.recycle();

    this.paint = new Paint();
    this.paint.setStyle(Paint.Style.STROKE);
    this.paint.setAntiAlias(true);
    this.rectangle = new RectF();
  }

  private void reloadBitmapIcon() {
    Bitmap temBitmap = BitmapFactory.decodeResource(getResources(), this.resourceStart);
    this.bitmapStart = Bitmap.createBitmap(this.sizeIcon, this.sizeIcon, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(this.bitmapStart);
    canvas.drawBitmap(temBitmap, new Rect(0, 0, temBitmap.getWidth(), temBitmap.getHeight()),
      new RectF(0, 0, this.sizeIcon, this.sizeIcon), null);
    temBitmap.recycle();

    Bitmap temEnd = BitmapFactory.decodeResource(getResources(), this.resourceEnd);
    this.bitmapEnd = Bitmap.createBitmap(this.sizeIcon, this.sizeIcon, Bitmap.Config.ARGB_8888);
    Canvas canvas1 = new Canvas(this.bitmapEnd);
    canvas1.drawBitmap(temEnd, new Rect(0, 0, temEnd.getWidth(), temEnd.getHeight()),
      new RectF(0, 0, this.sizeIcon,this.sizeIcon), null);
    temEnd.recycle();

    Bitmap temTop = BitmapFactory.decodeResource(getResources(), resourceTop);
    this.bitmapTop = Bitmap.createBitmap(this.sizeIcon, this.sizeIcon, Bitmap.Config.ARGB_8888);
    Canvas canvas2 = new Canvas(this.bitmapTop);
    canvas2.drawBitmap(temTop, new Rect(0, 0, temTop.getWidth(), temTop.getHeight()),
      new RectF(0, 0, this.sizeIcon, this.sizeIcon), null);
    temTop.recycle();
  }

  public void setProcess(int process) {
    this.process = process;
    invalidate();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    Matrix matrix = new Matrix();
    matrix.setScale(- 1, 1, w / 2, (h - this.padding) * 3 + this.padding * 2);
    this.rectangle.set(padding, this.padding, getWidth() - this.padding, (getHeight() - this.padding) * 3 + this.padding);
  }

  public void setTypeNomal(boolean typeNomal) {
    this.typeNormal = typeNomal;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    this.paint.setStyle(Paint.Style.STROKE);
    this.paint.setStrokeWidth(this.widthShadow);
    int start, currentEn;
    if (this.typeNormal) {
      currentEn = this.process * this.angle / 100;
      start = 180 + this.startAngle + currentEn;
    } else {
      currentEn = - this.process * this.angle / 200;
      start = 90;
    }
    canvas.drawArc(this.rectangle, - start, currentEn, false, this.paint);
    this.paint.setColor(this.colorSeek);
    this.paint.setStrokeWidth(this.widthSeek);
    canvas.drawArc(this.rectangle, - start, currentEn, false, this.paint);
    canvas.drawBitmap(this.bitmapStart, this.padding - this.bitmapStart.getWidth() / 2, getHeight() - this.bitmapStart.getHeight(), null);
    canvas.drawBitmap(this.bitmapEnd, getWidth() - this.padding - this.bitmapEnd.getWidth() / 2, getHeight() - this.bitmapEnd.getHeight(), null);
    if (this.widthSeek >= 10) {
      canvas.drawBitmap(this.bitmapTop, getWidth() / 2 - this.bitmapTop.getWidth() / 2, 0, null);
    } else {
      canvas.drawBitmap(this.bitmapTop, getWidth() / 2 - this.bitmapTop.getWidth() / 2, this.padding / 2, null);
    }
  }
}
