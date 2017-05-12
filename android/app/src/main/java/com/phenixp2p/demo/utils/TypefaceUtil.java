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

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Hashtable;

public class TypefaceUtil {
  private static final Hashtable<String, Typeface> cache = new Hashtable<String, Typeface>();

  public static void overrideFont(Context context, String defaultFontNameToOverride, String customFontFileNameInAssets) {
    try {
      final Typeface customFontTypeface = Typeface.createFromAsset(context.getAssets(), customFontFileNameInAssets);

      final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);
      defaultFontTypefaceField.setAccessible(true);
      defaultFontTypefaceField.set(null, customFontTypeface);
    } catch (Exception ignored) {}
  }

  public static Typeface get(Context c, String assetPath) {
    synchronized (cache) {
      if (!cache.containsKey(assetPath)) {
        try {
          Typeface t = Typeface.createFromAsset(c.getAssets(),
            assetPath);
          cache.put(assetPath, t);
        } catch (Exception e) {
          return null;
        }
      }
      return cache.get(assetPath);
    }
  }
}
