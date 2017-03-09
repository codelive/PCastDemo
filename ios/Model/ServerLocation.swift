/**
 * Copyright 2016 PhenixP2P Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import Foundation

final class ServerLocation {
  var name: String?
  var uri: String?
  var http: String?

  init(name: String, uri: String, http: String) {
    self.name = name
    self.uri = uri
    self.http = http
  }
}

//Server Location Strings
enum EndpointType: String {
  case Production = "https://demo.phenixp2p.com/demoApp/"
  case Staging = "https://stg.phenixp2p.com/demoApp/"
  case Local = "http://192.168.2.111:8080/demoApp/"
}

let PhenixServerList: [[String : String]] = [
  ["name":"Anycast (Closest Data Center)", "uri":"http://pcast.phenixp2p.com", "http":EndpointType.Production.rawValue],
  ["name":"APAC East", "uri":"wss://pcast-asia-east.phenixp2p.com", "http":EndpointType.Production.rawValue],
  ["name":"APAC Northeast", "uri":"wss://pcast-asia-northeast.phenixp2p.com", "http":EndpointType.Production.rawValue],
  ["name":"US West", "uri":"wss://pcast-us-west.phenixp2p.com", "http":EndpointType.Production.rawValue],
  ["name":"US Central", "uri":"wss://pcast-us-central.phenixp2p.com", "http":EndpointType.Production.rawValue],
  ["name":"US East", "uri":"wss://pcast-us-east.phenixp2p.com", "http":EndpointType.Production.rawValue],
  ["name":"EU West", "uri":"wss://pcast-europe-west.phenixp2p.com", "http":EndpointType.Production.rawValue],
  ["name":"Anycast - Staging", "uri":"http://pcast-stg.phenixp2p.com", "http":EndpointType.Staging.rawValue],
  ["name":"US Central - Staging", "uri":"wss://pcast-stg-us-central.phenixp2p.com", "http":EndpointType.Staging.rawValue],
  ["name":"EU West - Staging", "uri":"wss://pcast-stg-europe-west.phenixp2p.com", "http":EndpointType.Staging.rawValue],
  ["name":"Anycast - Local", "uri":"http://192.168.2.111:8080", "http":EndpointType.Local.rawValue],
  ["name":"Local", "uri":"wss://192.168.2.111:8443", "http":EndpointType.Local.rawValue]
]
