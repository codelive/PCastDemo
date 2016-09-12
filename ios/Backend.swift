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

enum TheServerIs: String {
  case LocalHost = "http://localhost:3000/"
  case RemoteHost =  "https://demo.phenixp2p.com/demoApp/"
}

let findTheServerAt = TheServerIs.RemoteHost

class Backend {
  // todo: add socket for notifications

  static let shared = Backend()

  var rest = Rest(basePath: findTheServerAt.rawValue)

  private enum Path: String {
    case Login = "login"
    case Stream = "stream"
  }

  var authToken: String?

  func login(name:String, password:String, done:@escaping (Bool)->()) throws {

    try self.rest.request(method:.POST, path: Path.Login.rawValue, params:["name":name, "password":password], completion: { _, result in
      if let dict = result as? Rest.Params, let token = dict["authenticationToken"] {
        self.authToken = token
        done(true)
      } else {
        done(false)
      }
    })
  }

  func streamToken(sessionId:String, originStreamId:String?, done:@escaping (String?)->()) throws {
    var params = ["sessionId":sessionId]
    if let o = originStreamId {
      params["originStreamId"] = o
    }
    try self.rest.request(method:.POST, path: Path.Stream.rawValue, params:params, completion: { _, result in
      if let dict = result as? Rest.Params, let token = dict["streamToken"] {
        done(token)
      } else {
        done(nil)
      }
    })
  }
}
