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

protocol CapabilityDelegate {
  func selectCapability(selection : Int)
}

class CapabilityPopoverVC : UIViewController {

  @IBOutlet weak var capabilityView: UIView!
  @IBOutlet weak var imgRealtimeSelected: UIImageView!
  @IBOutlet weak var imgBroadcastSelected: UIImageView!
  @IBOutlet weak var imgLiveSelected: UIImageView!
  let image3 = UIImage(named: "selection-circle")
  var currentCapability = 1
  var capabilityDelegate : CapabilityDelegate?

  override func viewDidLoad() {
    super.viewDidLoad()
  }

  override func viewDidAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    self.setImageForCurrentCapability(currentCapability: currentCapability)
  }

  func setImageForCurrentCapability(currentCapability : Int) {
    self.imgRealtimeSelected.image = nil
    self.imgBroadcastSelected.image = nil
    self.imgLiveSelected.image = nil

    switch currentCapability {
    case 1: self.imgRealtimeSelected.image = image3
    case 2: self.imgBroadcastSelected.image = image3
    case 3: self.imgLiveSelected.image = image3
    default: break
    }
  }

  @IBAction func realtimeClicked(_ sender: Any) {
    if let del = capabilityDelegate {
      del.selectCapability(selection: 1)
    }
    dismiss(animated: true, completion: nil)
    self.setImageForCurrentCapability(currentCapability:1)
  }

  @IBAction func broadcastClicked(_ sender: Any) {
    if let del = capabilityDelegate {
      del.selectCapability(selection: 2)
    }
    dismiss(animated: true, completion: nil)
    self.setImageForCurrentCapability(currentCapability:2)
  }

  @IBAction func liveClicked(_ sender: Any) {
    if let del = capabilityDelegate {
      del.selectCapability(selection: 3)
    }
    dismiss(animated: true, completion: nil)
    self.setImageForCurrentCapability(currentCapability:3)
  }
}
