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

import android.content.Context;

import com.phenixp2p.demo.R;
import com.phenixp2p.demo.ServerAddress;

import java.util.ArrayList;
import java.util.List;

public class ServerLocationFactory {

  private final List<ServerLocation> urlList = new ArrayList<>();

  private void addLocation(final Context context, final int titleId, final String serverAddress, final String pcastAddress) {
    String title = context.getString(titleId);
    ServerLocation serverLocation = new ServerLocation(title, serverAddress, pcastAddress);
    this.urlList.add(serverLocation);
  }

  public ServerLocationFactory(final Context context) {
    String productionEndpoint = ServerAddress.PRODUCTION_ENDPOINT.getServerAddress();
    String stagingEndpoint = ServerAddress.STAGING_ENDPOINT.getServerAddress();
    String localEndpoint = ServerAddress.LOCAL_ENDPOINT.getServerAddress();
    this.addLocation(context, R.string.list_manual, productionEndpoint, "");
    this.addLocation(context, R.string.list_anycast, productionEndpoint, "http://pcast.phenixp2p.com");
    this.addLocation(context, R.string.list_apac_east, productionEndpoint, "wss://pcast-asia-east.phenixp2p.com");
    this.addLocation(context, R.string.list_apac_northeast, productionEndpoint, "wss://pcast-asia-northeast.phenixp2p.com");
    this.addLocation(context, R.string.list_us_west, productionEndpoint, "wss://pcast-us-west.phenixp2p.com");
    this.addLocation(context, R.string.list_us_central, productionEndpoint, "wss://pcast-us-central.phenixp2p.com");
    this.addLocation(context, R.string.list_us_east, productionEndpoint, "wss://pcast-us-east.phenixp2p.com");
    this.addLocation(context, R.string.list_eu_west, productionEndpoint, "wss://pcast-europe-west.phenixp2p.com");
    this.addLocation(context, R.string.list_anycast_staging, stagingEndpoint, "http://pcast-stg.phenixp2p.com");
    this.addLocation(context, R.string.list_us_central_staging, stagingEndpoint, "wss://pcast-stg-us-central.phenixp2p.com");
    this.addLocation(context, R.string.list_eu_west_staging, stagingEndpoint, "wss://pcast-stg-europe-west.phenixp2p.com");
    this.addLocation(context, R.string.list_anycast_local, localEndpoint, "192.168.2.111:8080");
    this.addLocation(context, R.string.list_local, localEndpoint, "wss://192.168.2.111:8443");
  }

  public final List<ServerLocation> getLocations() {
    return this.urlList;
  }
}
