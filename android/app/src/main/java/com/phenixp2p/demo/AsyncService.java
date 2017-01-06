/**
 * Copyright 2016 PhenixP2P Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phenixp2p.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AsyncService {
  private ExecutorService service;
  private static AsyncService instance = new AsyncService();
  private AsyncService() {
    this.service = Executors.newFixedThreadPool(1);
  }

  public ExecutorService getExecutorService() {
    return this.service;
  }

  public void cancelAll() {
    this.service.shutdownNow();
    this.service = Executors.newFixedThreadPool(1);
  }

  public static AsyncService getInstance() {
    return instance;
  }
}
