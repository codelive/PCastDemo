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

import com.phenixp2p.demo.AsyncService;
import com.phenixp2p.demo.HttpTask;
import com.phenixp2p.demo.model.AuthenticationRequest;
import com.phenixp2p.demo.model.AuthenticationResponse;
import com.phenixp2p.demo.model.ListStreamRequest;
import com.phenixp2p.demo.model.ListStreamResponse;
import com.phenixp2p.demo.model.StreamTokenRequest;
import com.phenixp2p.demo.model.StreamTokenResponse;
import com.phenixp2p.demo.presenters.inter.IMainPresenter;
import com.phenixp2p.demo.ui.view.IMainActivityView;
import com.phenixp2p.demo.ui.view.IMainView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.phenixp2p.demo.Constants.APP_TAG;
import static com.phenixp2p.demo.Constants.STREAM_LIST_LENGTH;
import static com.phenixp2p.demo.HttpTask.Method.POST;
import static com.phenixp2p.demo.HttpTask.Method.PUT;

public final class MainPresenter implements IMainPresenter {
  private IMainView view;
  private IMainActivityView activityView;
  private Timer timer;
  private final static int TIMER_DELAY = 5000;

  public MainPresenter(IMainView view) {
    this.view = view;
  }

  public MainPresenter(IMainActivityView activityView) {
    this.activityView = activityView;
  }

  @Override
  public void login(String user, String password, String endpoint) {
    this.activityView.showProgress();
    AuthenticationRequest request = new AuthenticationRequest();
    request.setName(user);
    request.setPassword(password);
    HttpTask<AuthenticationRequest, AuthenticationResponse> task = new HttpTask<>(new HttpTask.Callback<AuthenticationResponse>() {
      @Override
      public void onResponse(AuthenticationResponse result) {
        if (result != null) {
          MainPresenter.this.activityView.authenticationToken(result.getAuthenticationToken());
        }
      }

      @Override
      public void onError(Exception e) {
        MainPresenter.this.activityView.onError(e.getMessage());
      }
    }, endpoint.concat("login"), POST, request, AuthenticationResponse.class);
    task.execute(AsyncService.getInstance().getExecutorService());
  }

  @Override
  public void createStreamToken(String endpoint,
                                String sessionId,
                                final String originStreamId,
                                String[] capabilities,
                                final IStreamer streamer) {
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
    HttpTask<StreamTokenRequest, StreamTokenResponse> task = new HttpTask<>(new HttpTask.Callback<StreamTokenResponse>() {
      private int errorCount = 0;

      @Override
      public void onResponse(StreamTokenResponse result) {
        if (result != null) {
          streamer.hereIsYourStreamToken(result.getStreamToken());
          if (originStreamId != null && MainPresenter.this.activityView != null) {
            MainPresenter.this.activityView.hideProgress();
          }
        } else {
          if (activityView != null) {
            MainPresenter.this.activityView.hideProgress();
          }
        }
      }

      @Override
      public void onError(Exception e) {
        Log.w(APP_TAG, "Caught error [" + e + "] while attempting to obtain stream token");
        this.errorCount++;
        streamer.isError(this.errorCount);
      }
    }, endpoint.concat("stream"), POST, request, StreamTokenResponse.class);
    task.execute(AsyncService.getInstance().getExecutorService());
  }

  @Override
  public synchronized void startRendering() {
    this.view.previewLocalUserMedia();
  }

  @Override
  public synchronized void listStreams(final int length, final String endpoint) {
    final ListStreamRequest params = new ListStreamRequest();
    params.setLength(length);
    List<String> options = new ArrayList<>();
    Collections.addAll(options, "global");
    params.setOptions(options);
    onRequest(params, endpoint);
    if (this.timer == null) {
      this.timer = new Timer();
      this.timer.schedule(new TimerTask() {
        @Override
        public void run() {
          onRequest(params, endpoint);
        }
      }, TIMER_DELAY, TIMER_DELAY);
    }
  }

  private synchronized <T>void onRequest(T params, final String endpoint) {
    HttpTask<T, ListStreamResponse> task = new HttpTask<>(new HttpTask.Callback<ListStreamResponse>() {
      @Override
      public void onResponse(ListStreamResponse result) {
        if (result != null) {
          List<ListStreamResponse.Stream> streamId = result.getStreams();
          if (streamId != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < streamId.size(); i++) {
              list.add(streamId.get(i).getStreamId());
            }
            MainPresenter.this.view.getListStreams(list);
          }
        } else {
          onDestroy();
          listStreams(STREAM_LIST_LENGTH, endpoint);
        }
      }

      @Override
      public void onError(Exception e) {
        if (MainPresenter.this.view != null) {
          MainPresenter.this.view.onError(e.getMessage());
        }
      }
    }, endpoint.concat("streams"), PUT, params, ListStreamResponse.class);
    task.execute(AsyncService.getInstance().getExecutorService());
  }

  @Override
  public void onDestroy() {
    if (this.timer != null) {
      this.timer.cancel();
      this.timer.purge();
      this.timer = null;
    }
    AsyncService.getInstance().cancelAll();
  }

  public interface IStreamer {
    void hereIsYourStreamToken(String streamToken);
    void isError(int count);
  }
}
