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
import Crashlytics

final class Rest {
  enum RestError: Error {
    case InvalidUrlPath
  }

  enum Method: String {
    case POST   = "POST"
    case GET  = "GET"
    case PUT  = "PUT"
    case DELETE = "DELETE"
  }

  private var retryNumber = 1

  typealias Completion = (_ success:Bool, _ result:AnyObject?) -> ()
  typealias Params = Dictionary<String, Any>

  private func urlRequest(path: String, params: Params?) throws -> NSMutableURLRequest {
    guard let url = URL(string: Phenix.shared.phenixInfo.http! + path) else {
      throw Rest.RestError.InvalidUrlPath
    }

    let request = NSMutableURLRequest(url: url)

    if path == "streams" {
      let jsonParams: [String: Any] = ["length": NSNumber(value: 100), "options":["global"]]
      let jsonData = try JSONSerialization.data(withJSONObject: jsonParams, options: JSONSerialization.WritingOptions())
      request.setValue("application/json", forHTTPHeaderField: "Content-Type")
      request.httpBody = jsonData
    } else if path == "stream" {
      if let paramsReq = params {
        let jsonData = try JSONSerialization.data(withJSONObject: paramsReq, options: JSONSerialization.WritingOptions())
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = jsonData
      }
    } else {
      if let paramsReq = params {
        var paramString = ""
        for (key, value) in paramsReq {
          if let escapedKey = key.addingPercentEncoding(withAllowedCharacters: NSCharacterSet.urlQueryAllowed), let escapedValue = (value as AnyObject).addingPercentEncoding(withAllowedCharacters:NSCharacterSet.urlQueryAllowed) {
            paramString += "\(escapedKey)=\(escapedValue)&"
          }
        }
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = paramString.data(using: String.Encoding.utf8)
      }
    }

    return request
  }

  private static func dataTask(numberOfRetry: Int, request: NSMutableURLRequest, method: String, completion: @escaping Completion) {
    var retry = numberOfRetry
    request.httpMethod = method
    request.setValue("Keep-Alive", forHTTPHeaderField: "Connection")
    let session = URLSession(configuration: URLSessionConfiguration.default)
    DispatchQueue.global().async { // http request in background thread
      NSLog("%@", "\(request.url!) request: " + timeElapsed())
      session.dataTask(with: request as URLRequest) { (data, response, error) -> Void in
        if let data = data, let dataString = String(data: data, encoding: .utf8), let rt = (response as? HTTPURLResponse)?.allHeaderFields["X-Response-Time"] {
          NSLog("%@", "\(request.url!) response \(dataString) (x-response-time=\(rt): " + timeElapsed())
        }
        DispatchQueue.main.sync { // UX callback in main thread
          if let data = data {
            let json = try? JSONSerialization.jsonObject(with: data, options: [])
            if let response = response as? HTTPURLResponse, 200...299 ~= response.statusCode {
              completion(true, json as AnyObject?)
              return
            } else {
              completion(false, json as AnyObject?)
            }
          } else if let err = error {
            print(err.localizedDescription)
          } else {
            completion(false, nil)
          }
          if retry > 0 {
            retry -= 1
            Rest.dataTask(numberOfRetry: retry, request: request, method: method, completion: completion)
          } else {
            completion(false, nil)
            return
          }
        }
      }.resume()
    }
  }

  func request(method:Method, path:String, params: Params?, completion: @escaping Completion) throws {
    let ur = try self.urlRequest(path: path, params:params)
    Rest.dataTask(numberOfRetry: retryNumber, request: ur, method: method.rawValue, completion: completion)
  }
}
