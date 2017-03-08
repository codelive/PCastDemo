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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.phenixp2p.demo.R;

public final class QualityStatusView extends View {
  private int firstColor;
  private int secondColor;
  private int thirdColor;
  private int fourthColor;
  private int firstProcess;
  private int secondProcess;
  private int thirdProcess;
  private int fourthProcess;
  private int paddingColumn;
  private int greyColor;

  private Paint paint;
  private int statusShow = 0;

  public QualityStatusView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize(context, attrs, 0);
  }

  public QualityStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize(context, attrs, defStyleAttr);
  }

  public void setStatusShow(int statusShow) {
    this.statusShow = statusShow;
    invalidate();
  }

  private void initialize(Context context, AttributeSet attrs, int defStyle) {
    TypedArray typedArray = context.obtainStyledAttributes(attrs,
            R.styleable.QualityStatusView,
            defStyle, 0);
    firstColor = typedArray.getColor(R.styleable.QualityStatusView_first_color,
      ContextCompat.getColor(context, R.color.red));
    secondColor = typedArray.getColor(R.styleable.QualityStatusView_second_color,
      ContextCompat.getColor(context, R.color.orange));
    thirdColor = typedArray.getColor(R.styleable.QualityStatusView_third_color,
      ContextCompat.getColor(context, R.color.blue));
    fourthColor = typedArray.getColor(R.styleable.QualityStatusView_fourth_color,
      ContextCompat.getColor(context, R.color.blue));

    firstProcess = typedArray.getInt(R.styleable.QualityStatusView_first_process, 25);
    secondProcess = typedArray.getInt(R.styleable.QualityStatusView_second_process, 50);
    thirdProcess = typedArray.getInt(R.styleable.QualityStatusView_third_process, 75);
    fourthProcess = typedArray.getInt(R.styleable.QualityStatusView_fourth_process, 100);

    paddingColumn = typedArray.getDimensionPixelSize(R.styleable.QualityStatusView_padding_column, 50);
    statusShow = typedArray.getInt(R.styleable.QualityStatusView_status_show, 0);
    greyColor = ContextCompat.getColor(context, R.color.gray);
    typedArray.recycle();

    paint = new Paint();
    paint.setAntiAlias(true);
    paint.setStyle(Paint.Style.FILL);
  }

  @SuppressLint("DrawAllocation")
  @Override
  protected void onDraw(Canvas canvas) {
    int width = getWidth() - getPaddingLeft() - getPaddingRight();
    int height = getHeight() - getPaddingTop() - getPaddingBottom();
    int widthColumn = (width - paddingColumn * 3) / 4;

    RectF rectFirst = new RectF(getPaddingLeft(), getPaddingTop() + height - firstProcess * height / 100,
      getPaddingLeft() + widthColumn, height + getPaddingTop());

    RectF rectSecond = new RectF(rectFirst.right + paddingColumn, getPaddingTop() + height - secondProcess * height / 100,
      rectFirst.right + paddingColumn + widthColumn, height + getPaddingTop());

    RectF rectThird = new RectF(rectSecond.right + paddingColumn, getPaddingTop() + height - thirdProcess * height / 100,
      rectSecond.right + paddingColumn + widthColumn, height + getPaddingTop());

    RectF rectFourth = new RectF(rectThird.right + paddingColumn, getPaddingTop() + height - fourthProcess * height / 100,
      rectThird.right + paddingColumn + widthColumn, height + getPaddingTop());

    int firstItemColor, secondItemColor, thirdItemColor, fourItemColor;
    switch (statusShow) {
      case 1:
        firstItemColor = firstColor;
        secondItemColor = thirdItemColor = fourItemColor = greyColor;
        break;
      case 2:
        firstItemColor = secondColor;
        secondItemColor = secondColor;
        thirdItemColor = fourItemColor = greyColor;
        break;
      case 3:
        firstItemColor = thirdColor;
        secondItemColor = thirdColor;
        thirdItemColor = thirdColor;
        fourItemColor = greyColor;
        break;
      case 4:
        firstItemColor = fourthColor;
        secondItemColor = fourthColor;
        thirdItemColor = thirdColor;
        fourItemColor = fourthColor;
        break;
      default:
        firstItemColor = secondItemColor = thirdItemColor = fourItemColor = greyColor;
        break;
    }
    paint.setColor(firstItemColor);
    canvas.drawRect(rectFirst, paint);

    paint.setColor(secondItemColor);
    canvas.drawRect(rectSecond, paint);

    paint.setColor(thirdItemColor);
    canvas.drawRect(rectThird, paint);

    paint.setColor(fourItemColor);
    canvas.drawRect(rectFourth, paint);
  }
}
