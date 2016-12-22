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

package com.phenixp2p.demo.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public abstract class BaseActivity extends AppCompatActivity {
  private CompositeSubscription mSubscriptions;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addSubscription(subscribeEvents());
  }

  protected Subscription subscribeEvents() {
    return null;
  }

  protected void addSubscription(Subscription subscription) {
    if (subscription == null) {
      return;
    }
    if (mSubscriptions == null) {
      mSubscriptions = new CompositeSubscription();
    }
    mSubscriptions.add(subscription);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mSubscriptions != null) {
      mSubscriptions.clear();
    }
  }

  public void autoUnsubBus() {
    if (mSubscriptions != null) {
      mSubscriptions.unsubscribe();
    }
  }
}
