/*
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

package com.phenixrts.demo;

public enum Capabilities {
  STREAMING("streaming"),
  REAL_TIME("real-time"),
  BROADCAST("broadcast"),
  ARCHIVE("archive");

  private String value;

  Capabilities(String value) {
    this.value = value;
  }

  public String getValue() {
    return this.value;
  }
}
