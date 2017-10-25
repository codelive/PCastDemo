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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class StreamTokenRequest {
  @Expose
  @SerializedName("sessionId")
  private String sessionId;
  @Expose
  @SerializedName("originStreamId")
  private String originStreamId;
  @Expose
  @SerializedName("capabilities")
  private List<String> capabilities;

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public void setOriginStreamId(String originStreamId) {
    this.originStreamId = originStreamId;
  }

  public void setCapabilities(List<String> capabilities) {
    this.capabilities = capabilities;
  }
}
