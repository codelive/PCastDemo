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

final class Backend {
  // todo: add socket for notifications
  static let shared = Backend()
  var rest = Rest()
  var authToken: String?
  private enum Path: String {
    case Login = "login"
    case Stream = "stream"
    case Streams = "streams"
  }

  func login(name:String, password:String, done:@escaping (Bool)->()) throws {
    try self.rest.request(method:.POST, path: Path.Login.rawValue, params:["name":name, "password":password], completion: { _, result in
      if let dict = result as? Rest.Params, let token = dict["authenticationToken"] as? String{
        self.authToken = token
        done(true)
      } else {
        done(false)
      }
    })
  }

  func createStreamToken(sessionId:String, originStreamId:String?, capabilities:[String]?, done:@escaping (String?)->()) throws {
    var params = [String: Any]()
    params["sessionId"] = sessionId
    if let o = originStreamId {
      params["originStreamId"] = o
    }
    if let c = capabilities {
      params["capabilities"] = c
    }
    try self.rest.request(method:.POST, path: Path.Stream.rawValue, params:params, completion: { _, result in
      if let dict = result as? Rest.Params, let token = dict["streamToken"] as? String{
        done(token)
      } else {
        done(nil)
      }
    })
  }

  func listStreams(done:@escaping (Array<String>)->()) throws {
    try self.rest.request(method:.PUT, path: Path.Streams.rawValue, params:nil, completion: { _, result in
      var streamsResult = Array<String>()
      if let dict = result as? Rest.Params{
        if let streamIdArray = dict["streams"] as? NSArray {
          for streamIdDict in streamIdArray{
            if let dict = streamIdDict as? Rest.Params, let streamId = dict["streamId"] as? String{
              streamsResult.append(streamId)
            }
          }
        }
      }
      done(streamsResult)
    })
  }
}
