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
import UIKit

let PhenixNameForLogFile = "PCast Demo App Log.txt"

let PhenixDefaults = UserDefaults.standard
struct PhenixDefaultsKey {
  static let FirstLaunch = "UserDefaults.Phenix.FirstLaunch"
}

enum PublishOption: Int {
  case None = 0
  case AudioOnly = 1
  case VideoOnly = 2
  case All = 3
  case ShareScreen = 4
}

enum QualityStatus: String {
  case NoData = "no data"
  case All = "all"
  case AudioOnly = "audio only"
}

enum QualityReason: String {
  case None = "none"
  case Upload = "upload limited"
  case Download = "download limited"
  case Network = "network limited"
  case Publisher = "publisher limited"
}

struct PhenixColor {
  static let Black = UIColor.black
  static let Blue = UIColor.rgb(red: 102, green: 153, blue: 204, alpha: 1.0)
  static let Red = UIColor.rgb(red: 204, green: 51, blue: 51, alpha: 1.0)
  static let Gray = UIColor.rgb(red: 88, green: 88, blue: 88, alpha: 1.0)
  static let Orange = UIColor.rgb(red: 204, green: 102, blue: 51, alpha: 1.0)
  static let LightOrange = UIColor.rgb(red: 204, green: 153, blue: 204, alpha: 1.0)
  static let GradientStart = UIColor.rgb(red: 202, green: 42, blue: 116, alpha: 1.0)
  static let GradientEnd = UIColor.rgb(red: 137, green: 43, blue: 96, alpha: 1.0)
}

struct PhenixNotification {
  static let BackToHome = "Notification.Phenix.BackToHome"
  static let ReconnectStream = "Notification.Phenix.ReconnectStream"
  static let ServerChanged = "Notification.Phenix.ServerChanged"
}

struct PhenixSegue {
  static let StreamSegue = "StreamSegue"
}

struct PhenixStoryboardID {
  static let CapabilityPopover = "CapabilityPopover"
  static let SecretUrlPopover = "SecretUrlPopover"
}
