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

enum PublishOption: Int {
  case audioOnly = 1
  case videoOnly = 2
  case all = 3
  case shareScreen = 4
}

enum QualityStatus: String {
  case statusNoData = "no data"
  case statusAll = "all"
  case statusAudioOnly = "audio only"
}

enum QualityReason: String {
  case reasonNone = "none"
  case reasonUpload = "upload limited"
  case reasonDownload = "download limited"
  case reasonNetwork = "network limited"
  case reasonPublisher = "publisher limited"
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

struct PhenixName {
  static let BackToHomeNotification = "Notification.Phenix.BackToHome"
}

struct PhenixSegue {
  static let StreamSegue = "StreamSegue"
}

struct PhenixStoryboardID {
  static let CapabilityPopover = "CapabilityPopover"
  static let SecretUrlPopover = "SecretUrlPopover"
}
