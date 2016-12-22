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

package com.phenixp2p.demo.events;

import com.phenixp2p.pcast.FacingMode;

public class Events {
  public static class HideMyStreamId {
    public final boolean isHide;

    public HideMyStreamId(boolean isHide) {
      this.isHide = isHide;
    }
  }

  public static class ChangeCamera {
    public final boolean isChange;

    public ChangeCamera(boolean isChange) {
      this.isChange = isChange;
    }
  }

  public static class OnStopStream {
    public OnStopStream() {
    }
  }

  public static class OnRestartStream {
    public OnRestartStream() {
    }
  }

  public static class GetFacingMode {
    public final FacingMode facingMode;

    public GetFacingMode(FacingMode facingMode) {
      this.facingMode = facingMode;
    }
  }
}
