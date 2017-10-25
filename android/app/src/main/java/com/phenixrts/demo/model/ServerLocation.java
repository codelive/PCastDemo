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

package com.phenixrts.demo.model;

public final class ServerLocation {
  private String title;
  private String serverAddress;
  private String pcastAddress;

  public ServerLocation(String title, String serverAddress, String pcastAddress) {
    this.serverAddress = serverAddress;
    this.pcastAddress = pcastAddress;
    this.title = title;
  }

  public String getServerAddress() {
    return this.serverAddress;
  }

  public String getPcastAddress() {
    return this.pcastAddress;
  }

  public String getTitle() {
    return this.title;
  }
}
