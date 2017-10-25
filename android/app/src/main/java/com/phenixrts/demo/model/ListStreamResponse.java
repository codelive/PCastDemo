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

public final class ListStreamResponse extends PhenixResponse {
  @Expose
  @SerializedName("streams")
  private final List<Stream> streams;

  public ListStreamResponse() {
    this.streams = null;
  }

  public List<Stream> getStreams() {
    return this.streams;
  }

  public static class Stream {
    @SerializedName("streamId")
    private String streamId;

    public String getStreamId() {
      return this.streamId;
    }

    public void setStreamId(String streamId) {
      this.streamId = streamId;
    }
  }
}
