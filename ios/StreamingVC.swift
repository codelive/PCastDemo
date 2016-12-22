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
import CoreMotion

class StreamingVC : UIViewController, UIPopoverPresentationControllerDelegate, CapabilityDelegate {
  // outlets and actions
  @IBOutlet weak var mainVideoView : UIView!
  @IBOutlet weak var previewVideoView : UIView!
  @IBOutlet weak var streamIdLabel : UILabel!
  @IBOutlet weak var switchCamera: UIImageView!

  // Define identifier
  let notificationBackToHome = Notification.Name("NotificationBackToHome")

  var currentCapability = 1    // 1:Real-time (default)    2:Streaming    3:Broadcast
  var streamId : String?
  var mainVideoLayer : CALayer?
  var previewVideoLayer : CALayer?
  var indicator = UIActivityIndicatorView(activityIndicatorStyle:.whiteLarge)
  var isUsingFrontCamera = false
  let pRenderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()

  override func viewDidLoad() {
    super.viewDidLoad()
    self.setNavBarButton()
    self.setSubviews()
    self.changeCapability(capability: "real-time")

    // Register to receive notification
    NotificationCenter.default.addObserver(self, selector: #selector(StreamingVC.showAlertBackToHome), name: Notification.Name("NotificationBackToHome"), object: nil)
  }

  override func viewWillAppear(_ animated:Bool) {
    super.viewWillAppear(animated)
  }

  override func viewWillDisappear(_ animated: Bool) {
    super.viewWillDisappear(animated)
    self.mainVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    self.mainVideoLayer?.removeFromSuperlayer()
  }

  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
    self.renderPreview()
  }

  override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
    super.viewWillTransition(to: size, with: coordinator)
    coordinator.animate(alongsideTransition: nil, completion: {
      _ in
      self.reloadVideoLayer()
      if let selfPreview = self.previewVideoLayer {
        let f = self.previewVideoView.frame
        selfPreview.frame = CGRect(x:0,y:0, width:f.width, height:f.height)
        DispatchQueue.main.async {
          self.previewVideoView.layer.addSublayer(selfPreview)
        }
      }
      self.indicator.frame = CGRect(x: 0, y: 0, width: 50, height: 50)
      self.indicator.center = self.view.center
      self.view.addSubview(self.indicator)
    })
  }

  deinit {
    NotificationCenter.default.removeObserver(self)
  }

  func setNavBarButton() {
    // Left button
    let leftButton = UIButton.init(type: .custom)
    leftButton.setImage(UIImage.init(named: "ic-back"), for: UIControlState.normal)
    leftButton.imageEdgeInsets = UIEdgeInsets(top: 5.0, left: 5.0, bottom: 5.0, right: 5.0)
    leftButton.addTarget(self, action:#selector(backToHome), for: UIControlEvents.touchUpInside)
    leftButton.frame = CGRect.init(x: 0, y: 0, width: 30, height: 30)
    leftButton.layer.cornerRadius = 30/2
    leftButton.backgroundColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.25)
    leftButton.alpha = 0.5
    let barButtonLeft = UIBarButtonItem.init(customView: leftButton)

    // Right button
    let rightButton = UIButton.init(type: .custom)
    rightButton.setImage(UIImage.init(named: "ic-menu"), for: UIControlState.normal)
    rightButton.imageEdgeInsets = UIEdgeInsets(top: 5.0, left: 5.0, bottom: 5.0, right: 5.0)
    rightButton.addTarget(self, action:#selector(popoverStreamCapability), for: UIControlEvents.touchUpInside)
    rightButton.frame = CGRect.init(x: 0, y: 0, width: 30, height: 30)
    rightButton.layer.cornerRadius = 30/2
    rightButton.backgroundColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.25)
    rightButton.alpha = 0.5

    let barButtonRight = UIBarButtonItem.init(customView: rightButton)
    DispatchQueue.main.async {
      self.navigationItem.leftBarButtonItem = barButtonLeft
      self.navigationItem.rightBarButtonItem = barButtonRight
    }
  }

  func setSubviews() {
    if self.isUsingFrontCamera {
      self.switchCamera.image = UIImage(named: "ic_camera_front")
    } else {
      self.switchCamera.image = UIImage(named: "ic_camera_rear")
    }

    self.indicator.frame = CGRect(x: 0, y: 0, width: 50, height: 50)
    self.indicator.center = self.view.center
    self.view.addSubview(self.indicator)
    self.previewVideoView.layer.borderWidth = 1.0
    self.previewVideoView.layer.cornerRadius = 5.0
    self.previewVideoView.layer.borderColor = UIColor.gray.cgColor
    self.previewVideoView.layer.masksToBounds = true
    self.streamIdLabel.layer.cornerRadius = self.streamIdLabel.frame.height / 2
    self.streamIdLabel.layer.masksToBounds = true
    if self.streamId == nil {
      self.streamIdLabel.text = "Unavailable";
    } else {
      if let regionString = streamId?.components(separatedBy:"#").first {
        if let endString = streamId?.substring(from:(streamId?.index((streamId?.endIndex)!, offsetBy: -4))!) {
          DispatchQueue.main.async {
            self.streamIdLabel.text = regionString + "#...." + endString
          }
        }
      }
    }
  }

  func reloadVideoLayer() {
    let f = self.view.frame
    if let videoSublayer = self.mainVideoLayer {
      videoSublayer.frame = CGRect(x:0,y:0, width:f.width, height:f.height)
      DispatchQueue.main.async {
        self.mainVideoView.layer.addSublayer(videoSublayer)
      }
    }
  }

  func backToHome() {
    navigationController!.popToRootViewController(animated: true)
  }

  func showAlertBackToHome() {
    let alert = UIAlertController(title: "The video publisher stopped.", message:"", preferredStyle: .alert)
    alert.addAction(UIAlertAction(title: "OK", style: .default) {
      _ in
      self.pRenderer?.stop()
      self.streamIdLabel.text = ""
      self.backToHome()
    })
    self.present(alert, animated: true){
    }
  }

  func popoverStreamCapability() {
    let popController = self.storyboard?.instantiateViewController(withIdentifier: "CapabilityPopover") as? CapabilityPopoverVC
    popController?.currentCapability = self.currentCapability
    popController?.capabilityDelegate = self

    // set the presentation style
    popController?.modalPresentationStyle = UIModalPresentationStyle.popover

    // set up the popover presentation controller
    popController?.popoverPresentationController?.delegate = self
    popController?.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection.up
    popController?.popoverPresentationController?.sourceView = self.navigationItem.rightBarButtonItem?.customView
    popController?.popoverPresentationController?.sourceRect = (self.navigationItem.rightBarButtonItem?.customView?.bounds)!
    popController?.popoverPresentationController?.backgroundColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.5)
    popController?.preferredContentSize = CGSize(width: 200, height: 120)

    // present the popover
    self.present(popController!, animated: true, completion: nil)
  }

  // delegate to get data back from pop-over
  func selectCapability(selection:Int) {
    switch selection {
    case 1:
      self.changeCapability(capability: "real-time")
      self.currentCapability = 1
    case 2:
      self.changeCapability(capability: "broadcast")
      self.currentCapability = 2
    case 3:
      self.changeCapability(capability: "streaming")
      self.currentCapability = 3
    default: break
    }
  }

  func changeCapability(capability:String) {
    DispatchQueue.main.async  {
      self.indicator.startAnimating()
      self.mainVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    }

    if let publishingSession = Phenix.shared.authSession, let streamIdFullString = self.streamId {
      do {
        try Backend.shared.createStreamToken(sessionId:publishingSession, originStreamId:streamIdFullString, capabilities:capability, done:subscribeTokenCallback)
      } catch {
      }
    } else {
      DispatchQueue.main.async  {
        self.indicator.stopAnimating()
      }
    }
  }

  func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
    // return UIModalPresentationStyle.FullScreen
    return UIModalPresentationStyle.none
  }

  func subscribeTokenCallback(subscribeToken:String?) {
    if let sid = self.streamId, let token = subscribeToken {
      Phenix.shared.subscribeToStream(streamId:sid, subscribeStreamToken:token, subscribeCallback:subscribing)
    } else {
      DispatchQueue.main.async  {
        self.indicator.stopAnimating()
      }
    }
  }

  func subscribing(success:Bool) {
    if success {
      DispatchQueue.main.async {
        if let subscribingStream = Phenix.shared.stream, Phenix.shared.authSession != nil {
          Phenix.shared.viewStream(stream:subscribingStream, renderReady:self.viewable, qualityChanged:self.rendering, renderStatus: self.renderStatus)
          subscribingStream.setStreamEndedCallback({ (mediaStream, phenixStreamEndedReason, reasonDescription) in
            self.mainVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
            let alert = UIAlertController(title: "The video publisher stopped.", message:reasonDescription, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default) {
              _ in
              self.pRenderer?.stop()
              self.streamIdLabel.text = ""
              self.navigationController!.popToRootViewController(animated: true)
            })
            self.present(alert, animated: true){
            }
          })
        }
      }
    } else {
      DispatchQueue.main.async  {
        self.indicator.stopAnimating()
        let alert = UIAlertController(title: "The video publisher stopped.", message:"", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default) {
          _ in
          self.pRenderer?.stop()
          self.streamIdLabel.text = ""
          self.navigationController!.popToRootViewController(animated: true)
        })
        self.present(alert, animated: true){
        }
      }
    }
  }

  func rendering() {
    // TO DO
  }

  func renderStatus(status:String?) {
    if let stt = status {
      let alert = UIAlertController(title: "Renderer failed", message:stt, preferredStyle: .alert)
      alert.addAction(UIAlertAction(title: "OK", style: .default) {
        _ in
        Phenix.shared.renderer?.stop()
      })
      self.present(alert, animated: true){
      }
    }
  }

  func viewable(layer:CALayer?) {
    DispatchQueue.main.async {
      self.indicator.stopAnimating()
      if let videoSublayer = layer {
        self.mainVideoLayer = videoSublayer
        self.reloadVideoLayer()
      }
    }
  }

  func renderPreview() {
    if self.pRenderer == nil {
      return
    }
    self.pRenderer?.stop()
    self.pRenderer?.setRenderSurfaceReadyCallback({ (renderer, layer) in
      if let selfPreview = layer {
        self.previewVideoLayer = selfPreview
        let f = self.previewVideoView.frame
        selfPreview.frame = CGRect(x:0,y:0, width:f.width, height:f.height)
        DispatchQueue.main.async {
          self.previewVideoView.layer.addSublayer(selfPreview)
        }
      }
    })
    let status = self.pRenderer?.start()
    if status == .ok {
      print("Renderer start status = \(status)")
    } else {
      let statusString = ("\(status)")
      let alert = UIAlertController(title: "Renderer failed", message:statusString, preferredStyle: .alert)
      alert.addAction(UIAlertAction(title: "OK", style: .default) {
        _ in
        self.pRenderer?.stop()
      })
      self.present(alert, animated: true){
      }
    }
    if self.pRenderer?.isAudioMuted == false {
      self.pRenderer?.muteAudio()
    }
  }

  @IBAction func cameraDirection(_ sender: Any) {
    var gumOptions = PhenixUserMediaOptions()
    if self.isUsingFrontCamera {
      self.switchCamera.image = UIImage(named: "ic_camera_rear")
      gumOptions.video.facingMode = .environment
      self.isUsingFrontCamera = false
    } else {
      self.switchCamera.image = UIImage(named: "ic_camera_front")
      gumOptions.video.facingMode = .user
      self.isUsingFrontCamera = true
    }
    Phenix.shared.userMediaStream?.apply(&gumOptions)
  }
}
