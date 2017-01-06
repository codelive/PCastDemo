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
import com.phenixp2p.demo.model.ListStreamRequest;
import com.phenixp2p.demo.model.ListStreamResponse;
import com.phenixp2p.demo.presenters.inter.IMainPresenter;
import com.phenixp2p.demo.ui.view.IMainView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.phenixp2p.demo.Constants.STREAM_LIST_DELAY;
import static com.phenixp2p.demo.Constants.STREAM_LIST_LENGTH;
import static com.phenixp2p.demo.HttpTask.Method.PUT;

public final class MainPresenter implements IMainPresenter {
  private IMainView view;
  private Timer timer;

  public MainPresenter(IMainView view) {
    this.view = view;
  }

  @Override
  public synchronized void startRendering() {
    this.view.previewLocalUserMedia();
  }

  /**
   * List available streams on server
   */
  @Override
  public synchronized void listStreams(final String endpoint) {
    final ListStreamRequest params = new ListStreamRequest();
    params.setLength(STREAM_LIST_LENGTH);
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
      }, STREAM_LIST_DELAY, STREAM_LIST_DELAY);
    }
  }

  /**
   * Request API list streams
   * ENDPOINT : https://demo.phenixp2p.com/demoApp/streams
   * REQUEST METHOD: PUT
   * @param params length
   */
  private synchronized <T>void onRequest(T params, final String endpoint) {
    HttpTask.Callback<ListStreamResponse> callback = new HttpTask.Callback<ListStreamResponse>() {
      @Override
      public void onResponse(ListStreamResponse result) {
        if (result != null) {
          List<ListStreamResponse.Stream> streamId = result.getStreams();
          if (streamId != null) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < streamId.size(); i++) {
              list.add(streamId.get(i).getStreamId());
            }
            view.getListStreams(list);
          }
        } else {
          onDestroy();
          listStreams(endpoint);
        }
      }

      @Override
      public void onError(Exception e) {
        if (MainPresenter.this.view != null) {
          MainPresenter.this.view.onError(e.getMessage());
        }
      }
    };
    HttpTask<T, ListStreamResponse> task = new HttpTask<>(callback, endpoint.concat("streams"), PUT, params, ListStreamResponse.class);
    task.execute(AsyncService.getInstance().getExecutorService());
  }

  @Override
  public void onDestroy() {
    if (timer != null) {
      timer.cancel();
      timer.purge();
      timer = null;
    }
    AsyncService.getInstance().cancelAll();
  }
}
