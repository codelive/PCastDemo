/*
 * Copyright (c) 2016. PhenixP2P Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0(the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phenixrts.demo;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

public final class RxBus {
  private static final String TAG = "RxBus";
  private RxBus() {}

  private static class SingletonHolder {
    private static final RxBus INSTANCE = new RxBus();
  }

  public static RxBus getInstance() {
    return SingletonHolder.INSTANCE;
  }

  private PublishSubject<Object> mEventBus = PublishSubject.create();

  public void post(Object event) {
    mEventBus.onNext(event);
  }

  public Observable<Object> toObservable() {
    return mEventBus;
  }

  public static Subscriber<Object> defaultSubscriber() {
    return new Subscriber<Object>() {
      @Override
      public void onError(Throwable e) {
        if (Fabric.isInitialized()) {
          Crashlytics.log(Log.ERROR, TAG, "Error received: " + e.getMessage());
        }
      }

      @Override
      public void onCompleted() {}

      @Override
      public void onNext(Object o) {}
    };
  }
}
