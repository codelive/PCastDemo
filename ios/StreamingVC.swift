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
import Crashlytics

enum StatusBarType: Int {
  case max = 4
  case third = 3
  case half = 2
  case min = 1
  case none = 0
}

enum DataFeedbackType {
  case viewer
  case publisher
}

final class StreamingVC : UIViewController, UIPopoverPresentationControllerDelegate, CapabilityDelegate {
  // outlets and actions
  @IBOutlet weak var mainVideoView : UIView!
  @IBOutlet weak var previewVideoView : UIView!
  @IBOutlet weak var imageAudioOnly: UIImageView!
  @IBOutlet weak var animationView: UIView!
  @IBOutlet weak var streamIdLabel : UILabel!
  @IBOutlet weak var buttonSwitch: UIButton!
  @IBOutlet weak var buttonStopPublish: UIButton!
  @IBOutlet weak var publishBarStack: UIStackView!
  @IBOutlet weak var optionStack: UIStackView!
  @IBOutlet weak var publishingOptionView: UIView!
  @IBOutlet weak var optionStackHeightConstraint: NSLayoutConstraint!

  var currentCapability = CapabilitySelection.RealTime
  var streamId : String?
  var mainVideoLayer : CALayer?
  var previewVideoLayer : CALayer?
  var indicator = UIActivityIndicatorView(activityIndicatorStyle:.whiteLarge)
  var isUsingFrontCamera = false
  var paraLayer: CAShapeLayer!
  var animateTimer = Timer()
  var renderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
  var publishingAnimation = CAAnimationGroup()
  var isPublishing: Bool = true {
    didSet {
      DispatchQueue.main.async {
        self.buttonSwitch.isHidden = !self.isPublishing
        self.buttonStopPublish.isHidden = !self.isPublishing
        self.publishBarStack.isHidden = !self.isPublishing
        self.publishingOptionView.isHidden = self.isPublishing
      }
    }
  }
  var isStartingToPublish: Bool = false {
    didSet {
      if isStartingToPublish == true {
        DispatchQueue.main.async {
          self.indicator.startAnimating()
          self.view.isUserInteractionEnabled = false
        }
      } else {
        DispatchQueue.main.async {
          self.indicator.stopAnimating()
          self.view.isUserInteractionEnabled = true
        }
      }
    }
  }

  override var prefersStatusBarHidden: Bool{
    return false
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    self.setNavBarButton()
    self.setSubviews()
    self.changeCapability(capability: "real-time")

    // Register to receive notification
    NotificationCenter.default.addObserver(self, selector: #selector(StreamingVC.showAlertBackToHome), name: Notification.Name(PhenixName.BackToHomeNotification), object: nil)
  }

  override func viewWillAppear(_ animated:Bool) {
    super.viewWillAppear(animated)

    // Listen to and display Publisher stream quality
    self.publisherDataQualityFeedback()
  }

  override func viewWillDisappear(_ animated: Bool) {
    super.viewWillDisappear(animated)
    self.renderer?.stop()
    Phenix.shared.stopRenderVideo()
    self.streamIdLabel.text = ""
    self.mainVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    self.mainVideoLayer?.removeFromSuperlayer()
  }

  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
    let path = self.isPublishing ? PathType.all : PathType.subscribe
    self.drawParabola(path: path)
    self.animateParabola(duration: 4.8)
    self.animateTimer = Timer.scheduledTimer(timeInterval: 5.0, target: self, selector: #selector(repeatAnimation), userInfo: nil, repeats: true)
    self.renderPreview()
  }

  override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
    super.viewWillTransition(to: size, with: coordinator)
    coordinator.animate(alongsideTransition: nil, completion: {
      _ in
      let path = self.isPublishing ? PathType.all : PathType.subscribe
      self.drawParabola(path: path)
      if UIDevice.current.orientation.isLandscape {
        self.optionStack.axis = .horizontal
        self.optionStackHeightConstraint = self.optionStackHeightConstraint.setMultiplier(multiplier: 0.5)
      } else {
        self.optionStack.axis = .vertical
        self.optionStackHeightConstraint = self.optionStackHeightConstraint.setMultiplier(multiplier: 0.66)
      }
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

  func setNavBarButton() {
    // Left button
    let leftButton = UIButton.init(type: .custom)
    leftButton.setImage(UIImage.init(named: "icon-back"), for: .normal)
    leftButton.imageEdgeInsets = UIEdgeInsets.zero
    leftButton.addTarget(self, action:#selector(backToHome), for: .touchUpInside)
    leftButton.frame = CGRect.init(x: 0, y: 0, width: 38, height: 38)
    leftButton.layer.borderWidth = 1.0
    leftButton.layer.borderColor = UIColor.lightGray.cgColor
    leftButton.layer.cornerRadius = 38/2
    leftButton.backgroundColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.25)
    let barButtonLeft = UIBarButtonItem.init(customView: leftButton)

    // Right button
    let rightButton = UIButton.init(type: .custom)
    rightButton.setImage(UIImage.init(named: "icon-menu"), for: .normal)
    rightButton.imageEdgeInsets = UIEdgeInsets(top: 5.0, left: 5.0, bottom: 5.0, right: 5.0)
    rightButton.addTarget(self, action:#selector(popoverStreamCapability), for: .touchUpInside)
    rightButton.frame = CGRect.init(x: 0, y: 0, width: 38, height: 38)
    rightButton.layer.borderWidth = 1.0
    rightButton.layer.borderColor = UIColor.lightGray.cgColor
    rightButton.layer.cornerRadius = 38/2
    rightButton.backgroundColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.25)

    let barButtonRight = UIBarButtonItem.init(customView: rightButton)
    DispatchQueue.main.async {
      self.navigationItem.leftBarButtonItem = barButtonLeft
      self.navigationItem.rightBarButtonItem = barButtonRight
    }
  }

  func setSubviews() {
    // Quick container views setup for feedback bars using tags
    // 2101..2104: Subscriber bars
    // 2105..2108: Publisher bars
    for viewTag in 2101...2108 {
      if let barView = self.view.viewWithTag(viewTag) {
        barView.backgroundColor = UIColor.init(red: 0, green: 0, blue: 0, alpha: 0.25)
      }
    }
    let bgImage = self.isUsingFrontCamera ? #imageLiteral(resourceName: "icon-camera-front") : #imageLiteral(resourceName: "icon-camera-rear")
    self.buttonSwitch.setBackgroundImage(bgImage, for: .normal)
    self.indicator.frame = CGRect(x: 0, y: 0, width: 50, height: 50)
    self.indicator.center = self.view.center
    self.view.addSubview(self.indicator)
    self.previewVideoView.layer.borderWidth = 2.0
    self.previewVideoView.layer.cornerRadius = 5.0
    self.previewVideoView.layer.borderColor = UIColor.gray.cgColor
    self.previewVideoView.layer.masksToBounds = true
    self.streamIdLabel.backgroundColor = UIColor.init(red: 0, green: 0, blue: 0, alpha: 0.25)
    self.streamIdLabel.layer.cornerRadius = self.streamIdLabel.frame.height / 2
    self.streamIdLabel.layer.masksToBounds = true
    if self.streamId == nil {
      self.streamIdLabel.text = "Unavailable"
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

  func publisherDataQualityFeedback() {
    if let phenixPublisher = Phenix.shared.publisher {
      phenixPublisher.setDataQualityChangedCallback({ (publisher, qualityStatus, qualityReason) in
        DispatchQueue.main.async {
          self.imageAudioOnly.isHidden = (qualityStatus == .audioOnly) ? false : true
        }
        self.handleFeedback(qualityReason: qualityReason, qualityStatus: qualityStatus, feedbackType: .publisher)
      })
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
    mainVideoLayer = nil
    previewVideoView = nil
    animateTimer.invalidate()
    renderer = nil
    NotificationCenter.default.removeObserver(self)
    navigationController!.popToRootViewController(animated: true)
  }

  func showAlertBackToHome() {
    let alert = UIAlertController(title: "The video publisher stopped.", message:"", preferredStyle: .alert)
    var isDismissed = false
    self.renderer?.stop()
    self.streamIdLabel.text = ""
    alert.addAction(UIAlertAction(title: "OK", style: .default) {
      _ in
      isDismissed = true
      self.backToHome()
    })
    DispatchQueue.main.async {
      self.present(alert, animated: true)
    }
    Utilities.delayOnMainQueue(delay: 3.0) {
      if isDismissed { return }
      alert.dismiss(animated: true, completion: nil)
      self.backToHome()
    }
  }

  func popoverStreamCapability() {
    let popController = self.storyboard?.instantiateViewController(withIdentifier: PhenixStoryboardID.CapabilityPopover) as! CapabilityPopoverVC
    popController.currentCapability = self.currentCapability
    popController.capabilityDelegate = self

    // set the presentation style
    popController.modalPresentationStyle = UIModalPresentationStyle.popover

    // set up the popover presentation controller
    popController.popoverPresentationController?.delegate = self
    popController.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection.up
    popController.popoverPresentationController?.sourceView = self.navigationItem.rightBarButtonItem?.customView
    popController.popoverPresentationController?.sourceRect = (self.navigationItem.rightBarButtonItem?.customView?.bounds)!
    popController.popoverPresentationController?.backgroundColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.5)
    popController.preferredContentSize = CGSize(width: 200, height: 120)

    // present the popover
    self.present(popController, animated: true)
  }

  // delegate to get data back from pop-over
  func selectCapability(selection:CapabilitySelection) {
    switch selection {
    case .RealTime:
      self.changeCapability(capability: "real-time")
    case .Broadcast:
      self.changeCapability(capability: "broadcast")
    case .Live:
      self.changeCapability(capability: "streaming")
    }
    self.currentCapability = selection
  }

  func changeCapability(capability:String) {
    DispatchQueue.main.async {
      self.indicator.startAnimating()
      self.mainVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    }
    if let publishingSession = Phenix.shared.authSession, let streamIdFullString = self.streamId {
      do {
        let arrCap = [capability]
        try Backend.shared.createStreamToken(sessionId:publishingSession, originStreamId:streamIdFullString, capabilities:arrCap, done:subscribeTokenCallback)
      } catch {
        print("Could not create stream token \(error)")
      }
    } else {
      DispatchQueue.main.async {
        self.indicator.stopAnimating()
      }
    }
  }

  func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
    return UIModalPresentationStyle.none
  }

  func subscribeTokenCallback(subscribeToken:String?) {
    if let sid = self.streamId, let token = subscribeToken {
      Phenix.shared.subscribeToStream(streamId:sid, subscribeStreamToken:token, subscribeCallback:subscribing)
    } else {
      DispatchQueue.main.async {
        self.indicator.stopAnimating()
      }
    }
  }

  func subscribing(success:Bool) {
    DispatchQueue.main.async {
      self.indicator.stopAnimating()
      if success {
        if let subscribingStream = Phenix.shared.subscribeStream, Phenix.shared.authSession != nil {
          Phenix.shared.viewStream(stream:subscribingStream, renderReady:self.viewable, qualityChanged:self.rendering, renderStatus: self.renderStatus)
          subscribingStream.setStreamEndedCallback({ (mediaStream, phenixStreamEndedReason, reasonDescription) in
            self.mainVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
            if self.mainVideoLayer != nil {
              self.showAlertBackToHome()
            }
          })
        }
      } else {
        let alert = UIAlertController(title: "Could not subscribe.", message:"", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in })
        self.present(alert, animated: true)
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
      self.present(alert, animated: true)
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
    if self.renderer == nil || !self.isPublishing{
      return
    }
    self.publishingAnimation = Utilities.publishButtonAnimation()
    DispatchQueue.main.async {
      self.buttonStopPublish.layer.add(self.publishingAnimation, forKey: "pulse")
    }
    self.renderer?.stop()
    self.renderer?.setRenderSurfaceReadyCallback({ (renderer, layer) in
      if let selfPreview = layer {
        self.previewVideoLayer = selfPreview
        let f = self.previewVideoView.frame
        selfPreview.frame = CGRect(x:0,y:0, width:f.width, height:f.height)
        DispatchQueue.main.async {
          self.previewVideoView.layer.addSublayer(selfPreview)
        }
      }
    })

    // Listen to and display Viewer stream quality
    self.renderer?.setDataQualityChangedCallback({ (renderer, qualityStatus, qualityReason) in
      self.handleFeedback(qualityReason: qualityReason, qualityStatus: qualityStatus, feedbackType: .viewer)
    })

    let status = self.renderer?.start()
    if status == .ok {
      print("Renderer start status = \(status)")
    } else {
      let statusString = ("\(status)")
      let alert = UIAlertController(title: "Renderer failed", message:statusString, preferredStyle: .alert)
      alert.addAction(UIAlertAction(title: "OK", style: .default) {
        _ in
        self.renderer?.stop()
      })
      self.present(alert, animated: true)
    }
    if self.renderer?.isAudioMuted == false {
      self.renderer?.muteAudio()
    }
  }

  func handleFeedback(qualityReason: PhenixDataQualityReason, qualityStatus: PhenixDataQualityStatus, feedbackType: DataFeedbackType) {
    var barTye = StatusBarType.max
    if qualityStatus == .noData || (!self.isPublishing && feedbackType == .publisher) {
      barTye = .none
    } else if qualityStatus == .all {
      switch qualityReason {
      case .none:
        barTye = .max
      case .publisherLimited, .uploadLimited:
        barTye = .third
      case .networkLimited, .downloadLimited:
        barTye = .half
      }
    } else {
      barTye = .min
    }
    DispatchQueue.main.sync {
      self.updateSatusBar(barTye: barTye, feedbackType: feedbackType)
    }
  }

  func updateSatusBar(barTye: StatusBarType, feedbackType: DataFeedbackType) {
    var barColor = UIColor.clear
    switch barTye {
    case .min: barColor = PhenixColor.Red
    case .half: barColor = PhenixColor.Orange
    case .third, .max: barColor = PhenixColor.Blue
    case .none: barColor = .clear
    }

    let currentMaxBarTag = (feedbackType == .publisher) ? (2200 + barTye.rawValue) : (2210 + barTye.rawValue)
    for barNumber in 1...4 {
      // Set the bars visible and colors by tag
      // 2201..2204: Viewer image bars
      // 2211..2214: Publisher image bars
      let imageBarTag = (feedbackType == .publisher) ? (2200 + barNumber) : (2210 + barNumber)
      if let imageBar = self.view.viewWithTag(imageBarTag) as? UIImageView{
        if imageBarTag <= currentMaxBarTag {
          imageBar.backgroundColor = barColor
        } else {
          imageBar.backgroundColor = .clear
        }
      }
    }
  }

  // MARK: Publishing function
  func login(name:String, password:String) {
    do {
      try Backend.shared.login(name:name, password:password, done:{ [weak self] success in
        if success {
          if let authToken = Backend.shared.authToken, let callback = self?.authCallback {
            Phenix.shared.connectAndAuthenticate(authToken:authToken, authCallback:callback)
            return
          }
        } else {
          self?.isStartingToPublish = false
        }
      })
    } catch {
      print(error.localizedDescription)
    }
  }

  func authCallback(sessionId:String?) {
    if let authSession = sessionId {
      Phenix.shared.authSession = authSession
      Phenix.shared.getLocalUserMedia(mediaOption: .audioOnly, mediaReady:mediaReady)
    }
  }

  func mediaReady(success:Bool) {
    if success {
      self.renderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
      self.startPublishing()
    }
  }

  func startPublishing() {
    if let auSession = Phenix.shared.authSession {
      do {
        let arrCap = ["streaming", "archive"]
        try Backend.shared.createStreamToken(sessionId:auSession, originStreamId:nil, capabilities:arrCap, done:publishTokenCallback)
      } catch {
        print(error.localizedDescription)
      }
    } else {
      // No Authorization Token, re-login to get the token
      self.login(name:"demo-user", password:"demo-password")
    }
  }

  func publishTokenCallback(publishToken:String?) {
    if let token = publishToken, let stream = Phenix.shared.userMediaStream?.mediaStream {
      Phenix.shared.getPublishStreamID(publishStreamToken:token, stream:stream, publishStreamIDCallback:publishStreamIDCallback)
    }
  }

  func publishStreamIDCallback(success:Bool) {
    self.isStartingToPublish = false
    if success {
      self.isPublishing = true
      self.drawParabola(path: .all)
      self.renderPreview()
    }
  }

  // MARK: Change camera direction
  func updateCameraSetting(isFront: Bool) {
    let gumOptions = PhenixUserMediaOptions()
    let bgImage = self.isUsingFrontCamera ? #imageLiteral(resourceName: "icon-camera-front") : #imageLiteral(resourceName: "icon-camera-rear")
    self.buttonSwitch.setBackgroundImage(bgImage, for: .normal)
    gumOptions.video.facingMode = self.isUsingFrontCamera ? .user : .environment
    Phenix.shared.userMediaStream?.apply(gumOptions)
  }

  // MARK: Parabola animating
  func drawParabola(path: PathType) {
    self.animationView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    let publishPath = Utilities.parabolaPath(pathType: path, viewToDraw: self.animationView, flatness: 0.1)
    paraLayer = CAShapeLayer()
    paraLayer.path = publishPath.cgPath
    paraLayer.fillColor = UIColor.clear.cgColor
    paraLayer.strokeColor = PhenixColor.Gray.cgColor
    paraLayer.lineWidth = 5.0
    paraLayer.strokeEnd = 0.0
    DispatchQueue.main.async {
      self.animationView.layer.addSublayer(self.paraLayer)
    }
  }

  func animateParabola(duration: TimeInterval) {
    // Animating the strokeEnd property of the circleLayer
    let animation = CABasicAnimation(keyPath: "strokeEnd")
    animation.duration = duration

    // Animate from 0 (no shape) to 1 (full shape)
    animation.fromValue = 0
    animation.toValue = 1
    animation.timingFunction = CAMediaTimingFunction(name: kCAMediaTimingFunctionLinear)
    paraLayer.strokeEnd = 1.0

    // Commit animation
    paraLayer.add(animation, forKey: "animateCircle")
  }

  func repeatAnimation() {
    self.animateParabola(duration: 4.8)
  }

  @IBAction func cameraDirection(_ sender: Any) {
    self.isUsingFrontCamera = !self.isUsingFrontCamera
    self.updateCameraSetting(isFront: self.isUsingFrontCamera)
  }

  @IBAction func stopPublishingTouched(_ sender: Any) {
    self.drawParabola(path: .subscribe)
    if self.isPublishing {
      self.isPublishing = false
      self.renderer?.stop()
      self.renderer = nil
      Phenix.shared.stopPublish()
      self.buttonStopPublish.layer.removeAllAnimations()
      self.previewVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    }
  }

  @IBAction func startPublishingTouched(_ sender: AnyObject) {
    self.isStartingToPublish = true
    switch sender.tag {
    case 2001: Phenix.shared.getLocalUserMedia(mediaOption: .audioOnly, mediaReady: mediaReady)
    case 2002: Phenix.shared.getLocalUserMedia(mediaOption: .videoOnly, mediaReady: mediaReady)
    case 2003: Phenix.shared.getLocalUserMedia(mediaOption: .all, mediaReady: mediaReady)
    default: break
    }
  }
}
