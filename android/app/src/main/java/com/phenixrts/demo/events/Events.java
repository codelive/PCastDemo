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

package com.phenixrts.demo.events;

import com.phenixrts.pcast.DataQualityReason;
import com.phenixrts.pcast.DataQualityStatus;

public final class Events {
  public static class ChangeCamera {
    public final boolean isChange;

    public ChangeCamera(boolean isChange) {
      this.isChange = isChange;
    }
  }

  public static class OnStopStream {
    public OnStopStream() {}
  }

  public static class OnRestartStream {
    public OnRestartStream() {}
  }

  public static class OnStateDataQuality {
    public final DataQualityStatus dataQualityStatus;
    public final DataQualityReason dataQualityReason;
    public final boolean isStateRender;

    public OnStateDataQuality(boolean isStateRender,
                              DataQualityStatus dataQualityStatus,
                              DataQualityReason dataQualityReason) {
      this.isStateRender = isStateRender;
      this.dataQualityStatus = dataQualityStatus;
      this.dataQualityReason = dataQualityReason;
    }
  }

  public static class OnShareScreen {
    public final boolean isStart;

    public OnShareScreen(boolean isStart) {
      this.isStart = isStart;
    }
  }
}
