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

package com.phenixp2p.demo.presenters;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.phenixp2p.demo.PhenixApplication;
import com.phenixp2p.demo.api.ApiFactory;
import com.phenixp2p.demo.model.StreamList;
import com.phenixp2p.demo.presenters.inter.IMainPresenter;
import com.phenixp2p.demo.ui.view.IMainView;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.plugins.RxJavaPlugins;
import rx.schedulers.Schedulers;

public class MainPresenter implements IMainPresenter {
  private static final String TAG = MainPresenter.class.getSimpleName();
  private IMainView view;
  private Subscription mSubscription;

  public MainPresenter(IMainView view) {
    this.view = view;
  }

  @Override
  public void startRendering() {
    view.previewLocalUserMedia();
  }

  /**
   * List available streams on server
   *
   * @param length
   */
  @Override
  public void listStreams(final int length) {
    try {
      JSONObject params = new JSONObject();
      params.put("length", length);
      mSubscription = ApiFactory.getApiService().listStreams(new Gson().fromJson(params.toString(), JsonElement.class))
        .subscribeOn(Schedulers.io())
        .unsubscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .repeatWhen(new Func1<Observable<? extends Void>, Observable<?>>() {
          @Override
          public Observable<?> call(Observable<? extends Void> observable) {
            return observable.delay(5, TimeUnit.SECONDS);
          }
        })
        .retry()
        .subscribe(new Subscriber<StreamList>() {
          @Override
          public void onCompleted() {
          }

          @Override
          public void onError(Throwable e) {
            try {
              RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
            } catch (Exception pluginException) {
              pluginException.printStackTrace();
              view.onError(e.getMessage());
              Log.d(TAG, "onError:  " + e.getMessage());
            }
          }

          @Override
          public void onNext(StreamList streamID) {
            view.getListStreams(streamID);
          }
        });

      if (PhenixApplication.getOnError() != null && ! PhenixApplication.getOnError().getMessage().equals("HTTP 410 ")) {
        view.onError(PhenixApplication.getOnError().getMessage());
      }
    } catch (Exception e) {
      view.onError(e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public void onDestroy() {
    if (mSubscription != null) {
      mSubscription.unsubscribe();
    }
  }
}
