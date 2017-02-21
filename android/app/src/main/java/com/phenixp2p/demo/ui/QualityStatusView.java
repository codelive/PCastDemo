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
import com.phenixp2p.demo.StatusQualityValues;

import static com.phenixp2p.demo.StatusQualityValues.NO_DATA;

public final class QualityStatusView extends View {
  private static final int QUARTER = 25;
  private static final int STATUS_PADDING = 50;
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
  private StatusQualityValues statusShow = NO_DATA;

  public QualityStatusView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.initialize(context, attrs, 0);
  }

  public QualityStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.initialize(context, attrs, defStyleAttr);
  }

  public void setStatusShow(StatusQualityValues statusShow) {
    this.statusShow = statusShow;
    invalidate();
  }

  private void initialize(Context context, AttributeSet attrs, int defStyle) {
    TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.QualityStatusView, defStyle, 0);
    this.firstColor = typedArray.getColor(R.styleable.QualityStatusView_first_color,
      ContextCompat.getColor(context, R.color.red));
    this.secondColor = typedArray.getColor(R.styleable.QualityStatusView_second_color,
      ContextCompat.getColor(context, R.color.orange));
    this.thirdColor = typedArray.getColor(R.styleable.QualityStatusView_third_color,
      ContextCompat.getColor(context, R.color.blue));
    this.fourthColor = typedArray.getColor(R.styleable.QualityStatusView_fourth_color,
      ContextCompat.getColor(context, R.color.blue));

    this.firstProcess = typedArray.getInt(R.styleable.QualityStatusView_first_process, QUARTER);
    this.secondProcess = typedArray.getInt(R.styleable.QualityStatusView_second_process, 2 * QUARTER);
    this.thirdProcess = typedArray.getInt(R.styleable.QualityStatusView_third_process, 3 * QUARTER);
    this.fourthProcess = typedArray.getInt(R.styleable.QualityStatusView_fourth_process, 4 * QUARTER);

    this.paddingColumn = typedArray.getDimensionPixelSize(R.styleable.QualityStatusView_padding_column, STATUS_PADDING);
    this.statusShow = StatusQualityValues.values()[typedArray.getInt(R.styleable.QualityStatusView_status_show, 0)];
    this.greyColor = ContextCompat.getColor(context, R.color.gray);
    typedArray.recycle();

    this.paint = new Paint();
    this.paint.setAntiAlias(true);
    this.paint.setStyle(Paint.Style.FILL);
  }

  @SuppressLint("DrawAllocation")
  @Override
  protected void onDraw(Canvas canvas) {
    int width = getWidth() - getPaddingLeft() - getPaddingRight();
    int height = getHeight() - getPaddingTop() - getPaddingBottom();
    int widthColumn = (width - this.paddingColumn * 3) / 4;

    RectF rectFirst = new RectF(getPaddingLeft(), getPaddingTop() + height - firstProcess * height / 100,
      getPaddingLeft() + widthColumn, height + getPaddingTop());

    RectF rectSecond = new RectF(rectFirst.right + this.paddingColumn, getPaddingTop() + height - this.secondProcess * height / 100,
      rectFirst.right + this.paddingColumn + widthColumn, height + getPaddingTop());

    RectF rectThird = new RectF(rectSecond.right + this.paddingColumn, getPaddingTop() + height - this.thirdProcess * height / 100,
      rectSecond.right + this.paddingColumn + widthColumn, height + getPaddingTop());

    RectF rectFourth = new RectF(rectThird.right + this.paddingColumn, getPaddingTop() + height - this.fourthProcess * height / 100,
      rectThird.right + this.paddingColumn + widthColumn, height + getPaddingTop());

    int firstItemColor, secondItemColor, thirdItemColor, fourItemColor;
    switch (this.statusShow) {
      case AUDIO_ONLY_LIMITED:
        firstItemColor = this.firstColor;
        secondItemColor = thirdItemColor = fourItemColor = this.greyColor;
        break;
      case ALL_UPLOAD:
        firstItemColor = this.secondColor;
        secondItemColor = this.secondColor;
        thirdItemColor = fourItemColor = this.greyColor;
        break;
      case ALL_DOWNLOAD:
        firstItemColor = this.thirdColor;
        secondItemColor = this.thirdColor;
        thirdItemColor = this.thirdColor;
        fourItemColor = this.greyColor;
        break;
      case AUDIO_ONLY_NONE:
        firstItemColor = this.fourthColor;
        secondItemColor = this.fourthColor;
        thirdItemColor = this.thirdColor;
        fourItemColor = this.fourthColor;
        break;
      default:
        firstItemColor = secondItemColor = thirdItemColor = fourItemColor = this.greyColor;
        break;
    }
    this.paint.setColor(firstItemColor);
    canvas.drawRect(rectFirst, this.paint);

    this.paint.setColor(secondItemColor);
    canvas.drawRect(rectSecond, this.paint);

    this.paint.setColor(thirdItemColor);
    canvas.drawRect(rectThird, this.paint);

    this.paint.setColor(fourItemColor);
    canvas.drawRect(rectFourth, this.paint);
  }
}
