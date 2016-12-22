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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.phenixp2p.demo.api.ApiFactory;
import com.phenixp2p.demo.api.response.Authentication;
import com.phenixp2p.demo.api.response.StreamToken;
import com.phenixp2p.demo.presenters.inter.IMainActivityPresenter;
import com.phenixp2p.demo.ui.view.IDetailStreamView;
import com.phenixp2p.demo.ui.view.IMainActivityView;

import org.json.JSONArray;
import org.json.JSONObject;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivityPresenter implements IMainActivityPresenter {
  private IMainActivityView view;
  private IDetailStreamView streamView;
  private CompositeSubscription mCompositeSubscription;

  public MainActivityPresenter(IMainActivityView view) {
    this.view = view;
    mCompositeSubscription = new CompositeSubscription();
  }

  public MainActivityPresenter(IDetailStreamView streamView) {
    this.streamView = streamView;
    mCompositeSubscription = new CompositeSubscription();
  }

  /**
   * REST API: authenticate with the app-maker's own server.
   * The app talks to a Phenix demo server, but you could also use the node.js server provided in this repo.
   *
   * @param user
   * @param password
   */
  @Override
  public void login(String user, String password) {
    view.showProgress();
    Subscription subscription = ApiFactory.getApiService().login(user, password)
      .subscribeOn(Schedulers.io())
      .unsubscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(new Subscriber<Authentication>() {
        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
          view.onError(e.getMessage());
        }

        @Override
        public void onNext(Authentication authentication) {
          view.authenticationToken(authentication);
        }
      });
    mCompositeSubscription.add(subscription);
  }

  @Override
  public void createStreamToken(String sessionId, final String originStreamId, String capability, final Streamer streamer) {
    try {
      JSONObject params = new JSONObject();
      params.put("sessionId", sessionId);
      if (originStreamId != null) {
        params.put("originStreamId", originStreamId);
      }
      if (capability != null) {
        params.put("capabilities", new JSONArray(new String[]{capability}));
      }

      Subscription subscription = ApiFactory.getApiService().stream(new Gson().fromJson(params.toString(), JsonElement.class))
        .subscribeOn(Schedulers.io())
        .unsubscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<StreamToken>() {
          @Override
          public void onCompleted() {
          }

          @Override
          public void onError(Throwable e) {
            if (view != null) {
              view.onError(e.getMessage());
            }

            if (streamView != null) {
              streamView.onError(e.getMessage());
            }
          }

          @Override
          public void onNext(StreamToken streamToken) {
            streamer.hereIsYourStreamToken(streamToken.getStreamToken());
            if (originStreamId != null && view != null) {
              view.hideProgress();
            }
          }
        });
      mCompositeSubscription.add(subscription);

    } catch (Exception e) {
      view.onError(e.getMessage());
      if (view != null) {
        view.onError(e.getMessage());
      }

      if (streamView != null) {
        streamView.onError(e.getMessage());
      }
    }
  }

  @Override
  public void onDestroy() {
    if (mCompositeSubscription != null)
      mCompositeSubscription.clear();
  }

  public interface Streamer {
    void hereIsYourStreamToken(String streamToken);
  }
}
