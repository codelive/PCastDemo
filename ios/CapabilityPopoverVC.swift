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

enum CapabilitySelection: Int {
  case RealTime = 1
  case Broadcast = 2
  case Live = 3
}

protocol CapabilityDelegate {
  func selectCapability(selection: CapabilitySelection)
}

final class CapabilityPopoverVC : UIViewController {

  @IBOutlet weak var capabilityView: UIView!
  @IBOutlet weak var imageRealtimeSelected: UIImageView!
  @IBOutlet weak var imageBroadcastSelected: UIImageView!
  @IBOutlet weak var imageLiveSelected: UIImageView!

  let selectionCircle = #imageLiteral(resourceName: "selection-circle")
  var currentCapability = CapabilitySelection.RealTime
  var capabilityDelegate : CapabilityDelegate?

  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    self.view.superview?.layer.cornerRadius = 6.0
  }

  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
    self.setImageForCurrentCapability(currentCapability: self.currentCapability)
  }

  func setImageForCurrentCapability(currentCapability : CapabilitySelection) {
    self.imageRealtimeSelected.image = #imageLiteral(resourceName: "icon-circle")
    self.imageBroadcastSelected.image = #imageLiteral(resourceName: "icon-circle")
    self.imageLiveSelected.image = #imageLiteral(resourceName: "icon-circle")
    switch currentCapability {
    case .RealTime: self.imageRealtimeSelected.image = self.selectionCircle
    case .Broadcast: self.imageBroadcastSelected.image = self.selectionCircle
    case .Live: self.imageLiveSelected.image = self.selectionCircle
    }
  }

  @IBAction func realtimeClicked(_ sender: Any) {
    if let del = capabilityDelegate {
      del.selectCapability(selection: .RealTime)
    }
    dismiss(animated: true, completion: nil)
    self.setImageForCurrentCapability(currentCapability:.RealTime)
  }

  @IBAction func broadcastClicked(_ sender: Any) {
    if let del = capabilityDelegate {
      del.selectCapability(selection: .Broadcast)
    }
    dismiss(animated: true, completion: nil)
    self.setImageForCurrentCapability(currentCapability:.Broadcast)
  }

  @IBAction func liveClicked(_ sender: Any) {
    if let del = capabilityDelegate {
      del.selectCapability(selection: .Live)
    }
    dismiss(animated: true, completion: nil)
    self.setImageForCurrentCapability(currentCapability:.Live)
  }
}
