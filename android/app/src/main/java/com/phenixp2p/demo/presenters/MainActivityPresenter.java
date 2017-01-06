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

import com.phenixp2p.demo.AsyncService;
import com.phenixp2p.demo.HttpTask;
import com.phenixp2p.demo.model.AuthenticationRequest;
import com.phenixp2p.demo.model.AuthenticationResponse;
import com.phenixp2p.demo.model.StreamTokenRequest;
import com.phenixp2p.demo.model.StreamTokenResponse;
import com.phenixp2p.demo.presenters.inter.IMainActivityPresenter;
import com.phenixp2p.demo.ui.view.IDetailStreamView;
import com.phenixp2p.demo.ui.view.IMainActivityView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.phenixp2p.demo.HttpTask.Method.POST;

public final class MainActivityPresenter implements IMainActivityPresenter {
  private IMainActivityView view;
  private IDetailStreamView streamView;

  public MainActivityPresenter(IMainActivityView view) {
    this.view = view;
  }

  public MainActivityPresenter(IDetailStreamView streamView) {
    this.streamView = streamView;
  }

  @Override
  public void login(String user, String password, String endpoint) {
    this.view.showProgress();
    AuthenticationRequest request = new AuthenticationRequest();
    request.setName(user);
    request.setPassword(password);
    HttpTask.Callback<AuthenticationResponse> callback = new HttpTask.Callback<AuthenticationResponse>() {
      @Override
      public void onResponse(AuthenticationResponse result) {
        if (result != null) {
          MainActivityPresenter.this.view.authenticationToken(result.getAuthenticationToken());
        }
      }

      @Override
      public void onError(Exception e) {
        view.onError(e.getMessage());
      }
    };
    HttpTask<AuthenticationRequest, AuthenticationResponse> task = new HttpTask<>(callback, endpoint + "login", POST, request, AuthenticationResponse.class);
    task.execute(AsyncService.getInstance().getExecutorService());
  }

  @Override
  public void createStreamToken(String sessionId, final String originStreamId, String endpoint, String[] capabilities, final Streamer streamer) {
    StreamTokenRequest request = new StreamTokenRequest();
    request.setSessionId(sessionId);
    if (originStreamId != null) {
      request.setOriginStreamId(originStreamId);
    }

    if (capabilities != null) {
      List<String> listCapabilities = new ArrayList<>();
      Collections.addAll(listCapabilities, capabilities);
      request.setCapabilities(listCapabilities);
    }
    HttpTask.Callback<StreamTokenResponse> callback = new HttpTask.Callback<StreamTokenResponse>() {
      @Override
      public void onResponse(StreamTokenResponse result) {
        if (result != null) {
          streamer.hereIsYourStreamToken(result.getStreamToken());
          if (originStreamId != null && MainActivityPresenter.this.view != null) {
            MainActivityPresenter.this.view.hideProgress();
          }
        } else {
          if (MainActivityPresenter.this.streamView != null) {
            MainActivityPresenter.this.streamView.onNullStreamToken();
          }

          if (MainActivityPresenter.this.view != null) {
            MainActivityPresenter.this.view.hideProgress();
          }
        }
      }

      @Override
      public void onError(Exception e) {}
    };
    HttpTask<StreamTokenRequest, StreamTokenResponse> task = new HttpTask<>(callback, endpoint.concat("stream"), POST, request, StreamTokenResponse.class);
    task.execute(AsyncService.getInstance().getExecutorService());
  }

  @Override
  public void onDestroy() {}

  public interface Streamer {
    void hereIsYourStreamToken(String streamToken);
  }
}
