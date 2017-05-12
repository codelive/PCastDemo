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
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout;
import android.util.AttributeSet;

import com.phenixp2p.demo.R;
import com.phenixp2p.demo.utils.TypefaceUtil;

public class CustomTextView extends AppCompatTextView {

  public CustomTextView(final Context context) {
    this(context, null);
  }

  public CustomTextView(final Context context, final AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CustomTextView(final Context context, final AttributeSet attrs, final int defStyle) {
    super(context, attrs, defStyle);
    if (this.isInEditMode()) {
      return;
    }

    final TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView);
    if (array != null) {
      final String typefaceAssetPath = array.getString(
        R.styleable.CustomTextView_customTypeFace);

      if (typefaceAssetPath != null) {
        Typeface typeface;
        typeface = TypefaceUtil.get(getContext(), typefaceAssetPath);
        setTypeface(typeface);
      }
      array.recycle();
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int specModeW = MeasureSpec.getMode(widthMeasureSpec);
    if (specModeW != MeasureSpec.EXACTLY) {
      Layout layout = getLayout();
      int linesCount = layout.getLineCount();
      if (linesCount > 1) {
        float textRealMaxWidth = 0;
        for (int n = 0; n < linesCount; ++n) {
          textRealMaxWidth = Math.max(textRealMaxWidth, layout.getLineWidth(n));
        }
        int w = Math.round(textRealMaxWidth);
        if (w < getMeasuredWidth()) {
          super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST),
            heightMeasureSpec);
        }
      }
    }
  }
}