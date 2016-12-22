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

package com.phenixp2p.demo;

import android.util.Log;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

public class RxBus {

  private static final String TAG = "RxBus";

  private static volatile RxBus sInstance;
  /**
   * PublishSubject<Object> subject = PublishSubject.create();
   * // observer1 will receive all onNext and onCompleted events
   * subject.subscribe(observer1);
   * subject.onNext("one");
   * subject.onNext("two");
   * // observer2 will only receive "three" and onCompleted
   * subject.subscribe(observer2);
   * subject.onNext("three");
   * subject.onCompleted();
   */
  private PublishSubject<Object> mEventBus = PublishSubject.create();

  public static RxBus getInstance() {
    if (sInstance == null) {
      synchronized (RxBus.class) {
        if (sInstance == null) {
          sInstance = new RxBus();
        }
      }
    }
    return sInstance;
  }

  /**
   * A simple logger for RxBus which can also prevent
   * potential crash(OnErrorNotImplementedException) caused by error in the workflow.
   */
  public static Subscriber<Object> defaultSubscriber() {
    return new Subscriber<Object>() {
      @Override
      public void onCompleted() {
        Log.d(TAG, "Duty off!!!");
      }

      @Override
      public void onError(Throwable e) {
        Log.e(TAG, "What is this? Please solve this as soon as possible!", e);
      }

      @Override
      public void onNext(Object o) {
        Log.d(TAG, "New event received: " + o);
      }
    };
  }

  public void post(Object event) {
    mEventBus.onNext(event);
  }

  public Observable<Object> toObservable() {
    return mEventBus;
  }
}
