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

package com.phenixp2p.demo.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StreamList {

  @SerializedName("status")
  private String status;
  @SerializedName("start")
  private String start;
  @SerializedName("length")
  private int length;
  @SerializedName("streams")
  private List<Stream> streams;

  public String getStatus() {
    return status;
  }

  public String getStart() {
    return start;
  }

  public int getLength() {
    return length;
  }

  public List<Stream> getStreams() {
    return streams;
  }

  public static class Stream {
    @SerializedName("streamId")
    private String streamId;

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Stream) {
        if (streamId.equals(((Stream) obj).streamId)) {
          return true;
        } else {
          return false;
        }
      }
      if (obj instanceof String) {
        if (streamId.equals(obj)) {
          return true;
        }
      }
      return super.equals(obj);
    }

    public String getStreamId() {
      return streamId;
    }

    public void setStreamId(String streamId) {
      this.streamId = streamId;
    }
  }
}
