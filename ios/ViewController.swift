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

import UIKit
import Crashlytics

// todo:use promises, futures
// todo:popup errors instead of print

enum StatusBarType: Int {
  case Max = 4
  case Third = 3
  case Half = 2
  case Min = 1
  case None = 0
}

enum DataFeedbackType {
  case Subscriber
  case Publisher
}

final class ViewController:
    UIViewController,
    UITableViewDataSource,
    UITableViewDelegate,
    UIPopoverPresentationControllerDelegate {

  // outlets and actions
  @IBOutlet weak var selectStreamHomeView:UIView!
  @IBOutlet weak var previewVideoView:UIView!
  @IBOutlet weak var previewVideoViewSmall:UIView!
  @IBOutlet weak var publishingOptionView:UIView!
  @IBOutlet weak var progress:UIProgressView!
  @IBOutlet weak var status:UILabel!
  @IBOutlet weak var versionNumber: UILabel!
  @IBOutlet weak var serverConectionNotice: UILabel!
  @IBOutlet weak var streamIdLabel : UILabel!
  @IBOutlet weak var birdLogo: UIImageView!
  @IBOutlet weak var idTableView: UITableView!
  @IBOutlet weak var changePreviewSize: UIButton!
  @IBOutlet weak var changePublishState: UIButton!
  @IBOutlet weak var buttonSubscribeBack: UIButton!
  @IBOutlet weak var buttonSubscribeOption: UIButton!
  @IBOutlet weak var buttonSwitch: UIButton!
  @IBOutlet weak var optionStack: UIStackView!
  @IBOutlet weak var publishStatusStack: UIStackView!
  @IBOutlet weak var subscribeStatusStack: UIStackView!
  @IBOutlet weak var optionStackHeightConstraint: NSLayoutConstraint!
  @IBOutlet weak var switchButtonToBottomParent: NSLayoutConstraint!
  @IBOutlet weak var stopPublishTrailingToParent: NSLayoutConstraint!
  @IBOutlet weak var previewProporsionalToParentHeight: NSLayoutConstraint!
  @IBOutlet weak var previewProporsionalToParentWidth: NSLayoutConstraint!
  @IBOutlet weak var animationView: UIView!
  @IBOutlet weak var subscribeView: UIView!
  @IBOutlet weak var imageAudioOnly: UIImageView!

  enum Step: String {
    case Launch = "Screen launch"
    case Login = "Login to backend server"
    case Auth = "Authorize with SDK"
    case Media = "Get user media"
    case PublishToken = "Get publish token"
    case PublishStream = "Publish and get stream ID"
    case SubscribeToken = "Get subscribe token"
    case Subscribe = "Subscribe"
    case View = "Ready to view video"
    case Render = "Data quality update"
  }

  let ThreeTaps = 3

  var currentCapability = CapabilitySelection.RealTime
  var popover = UIViewController()
  var parabolaLayer: CAShapeLayer!
  var publishingAnimation = CAAnimationGroup()
  let NUM_STEPS = 9
  var steps = 0
  var selectedStreamId:String?
  var pulseTimer = Timer()
  var updateStreamListTimer = Timer()
  var streamIdList = Array<String?>()
  var previewRecognizer: UIGestureRecognizer?
  var isUsingFrontCamera = false
  var isEndedByEnterBackground = false
  var indicator = UIActivityIndicatorView(activityIndicatorStyle:.whiteLarge)
  var renderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
  var subscribeLayer: CALayer?
  var parabolaType = PathType.Publish
  var isFullScreen: Bool = false
  var isSubscribing: Bool = false {
    didSet {
      DispatchQueue.main.async {
        UIView.transition(with: self.idTableView,
                          duration: 0.2,
                          options: .transitionCrossDissolve,
                          animations: { () -> Void in
          self.buttonSubscribeBack.isHidden = !self.isSubscribing
          self.buttonSubscribeOption.isHidden = !self.isSubscribing
          self.streamIdLabel.isHidden = !self.isSubscribing
          self.subscribeView.isHidden = !self.isSubscribing
          self.subscribeStatusStack.isHidden = !self.isSubscribing
          self.idTableView.isHidden = self.isSubscribing
          self.versionNumber.isHidden = self.isSubscribing
          self.changePreviewSize.isHidden = (!self.isSubscribing && self.isPublishing) ? false : true
        }, completion: nil);
      }
    }
  }
  var isPublishing: Bool = true {
    didSet {
      DispatchQueue.main.async {
        self.buttonSwitch.isHidden = !self.isPublishing
        self.publishStatusStack.isHidden = !self.isPublishing
        self.changePreviewSize.isHidden = (!self.isSubscribing && self.isPublishing) ? false : true
        self.changePublishState.isHidden = !self.isPublishing
        self.publishingOptionView.isHidden = self.isPublishing
      }
    }
  }
  var showSplashAnimation: Bool = true {
    didSet {
      if showSplashAnimation == true {
        if self.pulseTimer.isValid {
          self.pulseTimer.invalidate()
        }
        self.pulseTimer = Timer.scheduledTimer(timeInterval: 1.0,
                                               target: self,
                                               selector: #selector(addPulse),
                                               userInfo: nil,
                                               repeats: true)
        self.birdLogo.isHidden = false
        self.selectStreamHomeView.isHidden = true
      } else {
        self.pulseTimer.invalidate()
        self.birdLogo.isHidden = true
        self.selectStreamHomeView.isHidden = false
      }
    }
  }
  var isViewProgressing: Bool = false {
    didSet {
      if isViewProgressing == true {
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
  var isAbleToConnect: Bool = true {
    didSet {
      DispatchQueue.main.async {
        self.serverConectionNotice.isHidden = self.isAbleToConnect
      }
    }
  }
  // for loopback, the same stream ID is used for both publish and subscribe. But the stream tokens must be different.
  var streamIdThisPhone:String?

  deinit {
    NotificationCenter.default.removeObserver(self)
    self.updateStreamListTimer.invalidate()
    self.pulseTimer.invalidate()
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    self.addNotificationObserver()
    self.setupSubviews()
    self.restartProgress()
    self.startPublishing()
    self.navigationController?.navigationBar.setBackgroundImage(UIImage(), for: UIBarMetrics.default)
    self.navigationController?.navigationBar.shadowImage = UIImage()
    self.navigationController?.navigationBar.isTranslucent = true
  }

  override func viewDidLayoutSubviews() {
    self.drawParabola(path: self.parabolaType)
    self.updatePreviewLayer()
  }

  override func viewWillAppear(_ animated:Bool) {
    super.viewWillAppear(animated)
    if self.isPublishing {
      self.handlePreview()
    }
  }

  override func viewDidAppear(_ animated:Bool) {
    super.viewDidAppear(animated)
    if let publishingStreamId = Phenix.shared.streamId {
      self.streamIdThisPhone = publishingStreamId
      self.idTableView.reloadData()
    }
    self.updateStreamList()
    self.updateStreamListTimer = Timer.scheduledTimer(timeInterval: 5.0,
                                                      target: self,
                                                      selector: #selector(updateStreamList),
                                                      userInfo: nil,
                                                      repeats: true)
    self.enterFullScreen(isFullScreen: self.isFullScreen)
  }

  override func viewWillDisappear(_ animated: Bool) {
    super.viewWillDisappear(animated)
    self.previewVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    self.updateStreamListTimer.invalidate()
  }

  override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
    super.viewWillTransition(to: size, with: coordinator)
    coordinator.animate(alongsideTransition: nil, completion: {
      _ in
      self.updatePopoverFrame()
      self.updatesubscribeLayer()
      self.drawParabola(path: self.parabolaType)
      if UIDevice.current.orientation.isLandscape {
        self.optionStack.axis = .horizontal
        self.optionStackHeightConstraint = self.optionStackHeightConstraint.newConstraint(multiplier: 0.5)
      } else {
        self.optionStack.axis = .vertical
        self.optionStackHeightConstraint = self.optionStackHeightConstraint.newConstraint(multiplier: 0.66)
      }
      self.indicator.frame = CGRect(x: 0, y: 0, width: 50, height: 50)
      self.indicator.center = self.view.center
      self.view.addSubview(self.indicator)
      self.enterFullScreen(isFullScreen: self.isFullScreen)
      self.idTableView.reloadData()
    })
  }

  func addNotificationObserver() {
    NotificationCenter.default.addObserver(self,
                                           selector: #selector(ViewController.handleAppResignActive),
                                           name: Notification.Name.UIApplicationWillResignActive,
                                           object: nil)
    NotificationCenter.default.addObserver(self,
                                           selector: #selector(ViewController.handleAppEnterBackground(notification:)),
                                           name: Notification.Name.UIApplicationDidEnterBackground,
                                           object: nil)
    NotificationCenter.default.addObserver(self,
                                           selector: #selector(ViewController.handleAppEnterForeground(notification:)),
                                           name: Notification.Name.UIApplicationWillEnterForeground,
                                           object: nil)
    NotificationCenter.default.addObserver(self,
                                           selector: #selector(ViewController.serverInfoChanged),
                                           name: Notification.Name(PhenixNotification.ServerChanged),
                                           object: nil)
  }

  func serverInfoChanged() {
    self.isAbleToConnect = true
    self.restartProgress()
    self.renderer?.stop()
    Phenix.shared.authSession = nil
    Phenix.shared.stop()
    self.startPublishing()
  }

  func addPulse() {
    let pulse = PulsingAnimation(numberOfPulses: 1, radius: 200, position: self.birdLogo.center)
    pulse.backgroundColor = UIColor.gray.cgColor
    self.view.layer.insertSublayer(pulse, below: self.birdLogo.layer)
  }

  func getStreamList() {
    do {
      try Backend.shared.listStreams(done:updateStreamListOnTableView)
    } catch {
      print(error.localizedDescription)
      self.showAlertRetry(title: "Could not get streams list", message: "Try again.")
    }
  }

  func updateStreamList() {
    self.animateParabola(duration: 4.8)
    self.getStreamList()
  }

  func updateStreamListOnTableView(streamList:Array<String>) {
    self.streamIdList = streamList.sorted()
    if isPublishing {
      var i = 0;
      // swap self-Id to first index
      for streamIdFullString in self.streamIdList {
        if streamIdFullString != nil, streamIdFullString == self.streamIdThisPhone {
          let element = self.streamIdList.remove(at: i)
          self.streamIdList.insert(element, at: 0)
        }
        i += 1
      }
    }

    DispatchQueue.main.async {
      UIView.transition(with: self.idTableView, duration: 0.2, options: .transitionCrossDissolve, animations: { () -> Void in
          self.idTableView.reloadData()
      }, completion: nil);
    }
  }

  func checkStreamIdAvailability(streamList: Array<String>) {
    for streamIdFullString in streamList {
      if streamIdFullString == self.selectedStreamId {
        self.selectCapability(selection: self.currentCapability)
        return
      }
    }
    if self.isSubscribing {
      self.showAlertGoBackToMain()
    }
  }

  func setupSubviews() {
    var versionString = ""
    for buttonTag in 101...102 {
      if let menuButton = self.view.viewWithTag(buttonTag) {
        menuButton.backgroundColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.25)
        menuButton.layer.borderColor = UIColor.lightGray.cgColor
        menuButton.layer.cornerRadius = 36/2
        menuButton.layer.borderWidth = 1.0
      }
    }

    if let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
      versionString = "v" + version
    }

    if let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String {
      versionString += "(\(build))"
    }

    self.versionNumber.text = versionString
    self.indicator.frame = CGRect(x: 0, y: 0, width: 50, height: 50)
    self.indicator.center = self.view.center
    self.view.addSubview(self.indicator)
    self.previewVideoView.layer.borderWidth = 2.0
    self.previewVideoView.layer.cornerRadius = 5.0
    self.previewVideoView.layer.masksToBounds = true
    self.previewVideoView.layer.borderColor = UIColor.gray.cgColor
    self.selectStreamHomeView.isHidden = true

    // Triple Tap
    let tripleTap = UITapGestureRecognizer(target: self, action: #selector(ViewController.handleTripleTapOnLabelVersion))
    tripleTap.numberOfTapsRequired = ThreeTaps
    self.versionNumber.isUserInteractionEnabled = true
    self.versionNumber.addGestureRecognizer(tripleTap)
  }

  func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
    return UIModalPresentationStyle.none
  }

  func handleTripleTapOnLabelVersion() {
    self.showSecretPopup()
  }

  func showSecretPopup() {
    popover = storyboard?.instantiateViewController(withIdentifier: PhenixStoryboardID.SecretUrlPopover) as! SecretUrlPopoverVC
    popover.modalPresentationStyle = UIModalPresentationStyle.popover
    popover.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    self.updatePopoverFrame()
    self.present(popover, animated: true, completion: nil)
  }

  func updatePopoverFrame() {
    popover.popoverPresentationController?.delegate = self
    popover.popoverPresentationController?.sourceView = self.view
    popover.popoverPresentationController?.sourceRect = self.view.bounds
    popover.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection(rawValue: 0)
    popover.preferredContentSize = CGSize(width: self.view.bounds.width, height: 210.0)
  }

  func restartProgress() {
    self.showSplashAnimation = true
    self.steps = 0
    self.progress.setProgress(0.0, animated:false)
    self.progress.setNeedsDisplay()
  }

  func login(name:String, password:String) {
    do {
      try Backend.shared.login(name:name, password:password, done:{ success in
        self.isAbleToConnect = success
        self.reportStatus(step:.Login, success:success)
        if success {
          if let authToken = Backend.shared.authToken {
            Phenix.shared.connectAndAuthenticate(authToken:authToken, authCallback:self.authCallback)
            return
          }
        }
      })
    } catch {
      self.isAbleToConnect = false
      self.reportStatus(step:.Login, success:false)
      print(error.localizedDescription)
      self.showAlertRetry(title: "Could not log in", message: "Try again.")
    }
  }

  func authCallback(sessionId: String?) {
    self.reportStatus(step:.Auth, success:sessionId != nil)
    if let authSession = sessionId {
      Phenix.shared.authSession = authSession
      Phenix.shared.getLocalUserMedia(mediaOption: Phenix.shared.phenixPublishingOption, mediaReady:mediaReady)
      self.getStreamList()
    }
  }

  func mediaReady(success:Bool) {
    self.reportStatus(step:.Media, success:success)
    if success {
      self.renderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
      if Phenix.shared.phenixPublishingOption != .none {
        self.startPublishing()
      }
    }
  }

  func publishTokenCallback(publishToken:String?) {
    if let token = publishToken, let stream = Phenix.shared.userMediaStream?.mediaStream {
      Phenix.shared.getPublishStreamID(publishStreamToken:token, stream:stream, publishStreamIDCallback:publishStreamIDCallback)
    } else {
      self.isAbleToConnect = false
      self.reportStatus(step:.PublishToken, success:false)
    }
  }

  func publishStreamIDCallback(success:Bool) {
    self.reportStatus(step:.PublishStream, success:success)
    if success {
      if let publishingStreamId = Phenix.shared.streamId {
        self.streamIdThisPhone = publishingStreamId
      }
      self.isPublishing = true
      self.isViewProgressing = false
      self.handlePreview()
      self.updateParabolaPath()
      DispatchQueue.main.async {
        self.showSplashAnimation = false
        self.idTableView.reloadData()
      }
    }
  }

  func publisherDataQualityFeedback() {
    if let phenixPublisher = Phenix.shared.publisher {
      phenixPublisher.setDataQualityChangedCallback({ (publisher, qualityStatus, qualityReason) in
        DispatchQueue.main.async {
          self.imageAudioOnly.isHidden = (qualityStatus == .audioOnly) ? false : true
        }
        self.handleFeedback(qualityReason: qualityReason, qualityStatus: qualityStatus, feedbackType: .Publisher)
      })
    }
  }

  func reportStatus(step:Step, success:Bool) {
    if !success {
      self.isViewProgressing = success
    }
    let ms = timeElapsed()
    let text = step.rawValue + ": " + (success ? "completed" :"failed")
    NSLog("%@", "Step '" + text + "' " + ms)
  }

  func handleFeedback(qualityReason: PhenixDataQualityReason, qualityStatus: PhenixDataQualityStatus, feedbackType: DataFeedbackType) {
    var barTye = StatusBarType.Max
    if qualityStatus == .noData || (!self.isPublishing && feedbackType == .Publisher) {
      barTye = .None
    } else if qualityStatus == .all {
      switch qualityReason {
      case .none:
        barTye = .Max
      case .publisherLimited, .uploadLimited:
        barTye = .Third
      case .networkLimited, .downloadLimited:
        barTye = .Half
      }
    } else {
      barTye = .Min
    }
    DispatchQueue.main.sync {
      self.updateSatusBar(barTye: barTye, feedbackType: feedbackType)
    }
  }

  func updateSatusBar(barTye: StatusBarType, feedbackType: DataFeedbackType) {
    var barColor = UIColor.clear
    switch barTye {
    case .Min: barColor = PhenixColor.Red
    case .Half: barColor = PhenixColor.Orange
    case .Third, .Max: barColor = PhenixColor.Blue
    case .None: barColor = .clear
    }

    let currentMaxBarTag = (feedbackType == .Publisher) ? (2200 + barTye.rawValue) : (2210 + barTye.rawValue)
    for barNumber in 1...4 {
      // Set the bars visible and colors by tag
      // 2201..2204: Viewer image bars
      // 2211..2214: Publisher image bars
      let imageBarTag = (feedbackType == .Publisher) ? (2200 + barNumber) : (2210 + barNumber)
      if let imageBar = self.view.viewWithTag(imageBarTag) as? UIImageView{
        if imageBarTag <= currentMaxBarTag {
          imageBar.backgroundColor = barColor
        } else {
          imageBar.backgroundColor = .clear
        }
      }
    }
  }

  func startPublishing() {
    if Phenix.shared.phenixPublishingOption == .none {
      Utilities.executeOnMainQueueAfterDelay(seconds: 2) {
        self.showSplashAnimation = false
        return
      }
    }
    if let auSession = Phenix.shared.authSession {
      do {
        let arrCap = ["streaming", "archive"]
        try Backend.shared.createStreamToken(sessionId:auSession,
                                             originStreamId:nil,
                                             capabilities:arrCap,
                                             done:publishTokenCallback)
      } catch {
        self.isAbleToConnect = false
        self.reportStatus(step:.PublishToken, success:false)
        print(error.localizedDescription)
        self.showAlertRetry(title: "Could not create streams token", message: "Try again.")
      }
    } else {
      // No Authorization Token, re-login to get the token
      self.login(name:"demo-user", password:"demo-password")
    }
  }

  override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
    get {
      return UIInterfaceOrientationMask.all
    }
  }

  func handleAppResignActive(notification: Notification) {
    self.isEndedByEnterBackground = true
  }

  func handleAppEnterBackground(notification: Notification) {
    self.renderer?.stop()
    Phenix.shared.authSession = nil
    Phenix.shared.stop()
  }

  func handleAppEnterForeground(notification: Notification) {
    self.isEndedByEnterBackground = false
    self.versionNumber.isHidden = false
    self.streamIdLabel.isHidden = true
    self.restartProgress()
    self.startPublishing()
  }

  func shortenStreamId(streamId:String) -> String {
    if let regionString = streamId.components(separatedBy:"#").first {
      let endString = streamId.substring(from:(streamId.index((streamId.endIndex), offsetBy: -4)))
      return (regionString + "#...." + endString)
    }

    return ""
  }

  func enterFullScreen(isFullScreen : Bool) {
    var f = CGRect()
    if isFullScreen {
      self.previewProporsionalToParentWidth = self.previewProporsionalToParentWidth.newConstraint(multiplier: 1.0)
      self.previewProporsionalToParentHeight = self.previewProporsionalToParentHeight.newConstraint(multiplier: 1.0)
      f = self.selectStreamHomeView.bounds
      self.changePreviewSize.setBackgroundImage(UIImage(named: "icon-full-screen-exit"), for: .normal)
      DispatchQueue.main.async {
        self.previewVideoView.layer.borderWidth = 0
        self.previewVideoView.layer.cornerRadius = 0
      }
      self.switchButtonToBottomParent.constant = f.size.height - 102
      self.stopPublishTrailingToParent.constant = f.size.width - 27
    } else {
      f = self.previewVideoViewSmall.frame
      self.changePreviewSize.setBackgroundImage(UIImage(named: "icon-full-screen-enter"), for: .normal)
      DispatchQueue.main.async {
        UIView.animate(withDuration: 0.3, animations: {
          self.previewProporsionalToParentWidth = self.previewProporsionalToParentWidth.newConstraint(multiplier: 0.25)
          self.previewProporsionalToParentHeight = self.previewProporsionalToParentHeight.newConstraint(multiplier: 0.25)
          self.previewVideoView.layer.borderWidth = 2.0
          self.previewVideoView.layer.cornerRadius = 5.0
          self.previewVideoView.layer.borderColor = PhenixColor.Gray.cgColor
        })
      }
      self.switchButtonToBottomParent.constant = f.size.height - 27
      self.stopPublishTrailingToParent.constant = f.size.width - 25
    }
  }

  func handlePreview() {
    self.renderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
    if Phenix.shared.phenixPublishingOption != .none {
      self.publishingAnimation = Utilities.publishButtonAnimation()
      DispatchQueue.main.async {
        self.changePublishState.layer.add(self.publishingAnimation, forKey: "pulse")
      }
      self.renderer?.setRenderSurfaceReadyCallback({ (renderer, layer) in
        if let previewLayer = layer {
          Phenix.shared.userMediaLayer = previewLayer
          self.updatePreviewLayer()
          DispatchQueue.main.async {
            let bgImage = self.isUsingFrontCamera ? #imageLiteral(resourceName: "icon_camera_front") : #imageLiteral(resourceName: "icon_camera_rear")
            self.buttonSwitch.setBackgroundImage(bgImage, for: .normal)
          }
        }
      })

      // Listen to and display Viewer stream quality
      self.renderer?.setDataQualityChangedCallback({ (renderer, qualityStatus, qualityReason) in
        self.handleFeedback(qualityReason: qualityReason, qualityStatus: qualityStatus, feedbackType: .Subscriber)
      })

      let status = self.renderer?.start()
      if status == nil || status == .conflict {
        return
      }
      if status == .ok {
        print("Renderer start status .ok")
      } else {
        let statusString = ("\(status)")
        let alert = UIAlertController(title: "Renderer failed", message:statusString, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default) {
          _ in
        })
        self.present(alert, animated: true)
      }
    } else {
      self.isPublishing = false
      self.isFullScreen = false
      self.changePublishState.layer.removeAllAnimations()
      self.previewVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    }
  }

  func updatePreviewLayer() {
    self.previewVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    if !isPublishing {
      return
    }
    if let previewLayer = Phenix.shared.userMediaLayer {
      if isFullScreen {
        previewLayer.frame = self.selectStreamHomeView.bounds
      } else {
        previewLayer.frame = self.previewVideoViewSmall.bounds
      }
      DispatchQueue.main.async {
        self.previewVideoView.layer.addSublayer(previewLayer)
      }
    }
  }

  func updateCameraSetting(isFront: Bool) {
    let gumOptions = PhenixUserMediaOptions()
    let bgImage = isFront ? #imageLiteral(resourceName: "icon_camera_front") : #imageLiteral(resourceName: "icon_camera_rear")
    self.buttonSwitch.setBackgroundImage(bgImage, for: .normal)
    gumOptions.video.facingMode = isFront ? .user : .environment
    Phenix.shared.userMediaStream?.apply(gumOptions)
  }

  // MARK: IBAction
  @IBAction func subscribingOptionTouched(_ sender: AnyObject) {
    switch sender.tag {
    case 101:
      self.isEndedByEnterBackground = false
      self.goBackToMain()
    case 102: self.popoverStreamCapability()
    default: break
    }
  }

  @IBAction func startPublishingTouched(_ sender: AnyObject) {
    self.isViewProgressing = true
    if self.isPublishing {
      self.isViewProgressing = false
      return
    }
    switch sender.tag {
    case 1001: Phenix.shared.phenixPublishingOption = .AudioOnly
    case 1002: Phenix.shared.phenixPublishingOption = .VideoOnly
    case 1003: Phenix.shared.phenixPublishingOption = .All
    default: break
    }
    Phenix.shared.getLocalUserMedia(mediaOption: Phenix.shared.phenixPublishingOption, mediaReady: mediaReady)
  }

  @IBAction func switchCamera(_ sender: AnyObject) {
    self.isUsingFrontCamera = !self.isUsingFrontCamera
    self.updateCameraSetting(isFront: self.isUsingFrontCamera)
  }

  @IBAction func changePreviewSizeClicked(_ sender: AnyObject) {
    self.isFullScreen = !self.isFullScreen
    self.enterFullScreen(isFullScreen: self.isFullScreen)
  }

  @IBAction func changePublishStateClicked(_ sender: AnyObject) {
    if self.isPublishing {
      self.isPublishing = false
      self.isFullScreen = false
      self.renderer?.stop()
      self.renderer = nil
      self.updateParabolaPath()
      Phenix.shared.stopPublish()
      self.updatePreviewLayer()
      Phenix.shared.phenixPublishingOption = .None
      self.enterFullScreen(isFullScreen: self.isFullScreen)
      self.changePublishState.layer.removeAllAnimations()
      self.previewVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }

      // Remove own device's ID immediately to advoid clicking on while publishing stopped
      if let idThisPhone = self.streamIdThisPhone, self.streamIdList.count > 0 ,idThisPhone == self.streamIdList[0] {
        self.streamIdList.remove(at: 0)
        self.streamIdThisPhone = nil
        self.idTableView.reloadData()
      }
    }
  }

  // MARK: UITableViewDataSource
  func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
    return self.streamIdList.count
  }

  func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
    let cell:StreamIdTableViewCell = self.idTableView.dequeueReusableCell(withIdentifier: "StreamIdCell") as! StreamIdTableViewCell
    if self.streamIdList.count > indexPath.row {
      if let streamIdFullString = self.streamIdList[indexPath.row] {
        if indexPath.row == 0 {
          if isPublishing, let streamId = self.streamIdThisPhone {
            cell.idLabel.text = self.shortenStreamId(streamId: streamId) + " (this device)"
          } else {
            cell.idLabel.text = self.shortenStreamId(streamId: streamIdFullString)
          }
        } else {
          cell.idLabel.text = self.shortenStreamId(streamId: streamIdFullString)
        }
      }
    }
    cell.backgroundColor = UIColor.clear
    let gradientFrame = CGRect(x: 0.0, y: 0.0, width: tableView.layer.frame.width, height: cell.contentView.bounds.size.height)
    let gradientColors = [PhenixColor.GradientStart.cgColor, PhenixColor.GradientEnd.cgColor]
    let gradient = Utilities.createGradientLayer(frame: gradientFrame, colors: gradientColors)
    gradient.startPoint = CGPoint(x:0, y:0)
    gradient.endPoint = CGPoint(x:1.0, y:0)
    gradient.zPosition = -1

    cell.idGradientView.layer.cornerRadius = 5.0
    cell.idGradientView.layer.masksToBounds = true
    cell.idGradientView.layer.addSublayer(gradient)

    return cell
  }

  // MARK: UIPopoverController Delegate
  func popoverPresentationControllerShouldDismissPopover(_ popoverPresentationController: UIPopoverPresentationController) -> Bool {
    if isSubscribing {
      return true
    }
    return false
  }

  // MARK: UITableViewDelegate
  func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
    if let streamIdFullString = self.streamIdList[indexPath.row] {
      self.selectedStreamId = streamIdFullString
      let cell = tableView.cellForRow(at: indexPath) as! StreamIdTableViewCell
      self.streamIdLabel.text = cell.idLabel.text
    } else {
      self.streamIdLabel.text = "Unavailable"
    }
    self.selectCapability(selection: self.currentCapability)
  }

  func changeCapability(capability:String) {
    DispatchQueue.main.async {
      self.showSplashAnimation = false
      self.subscribeView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    }
    self.isViewProgressing = true
    if let publishingSession = Phenix.shared.authSession, let streamIdFullString = self.selectedStreamId {
      do {
        let capabilities = [capability]
        try Backend.shared.createStreamToken(sessionId: publishingSession,
                                             originStreamId: streamIdFullString,
                                             capabilities: capabilities,
                                             done: subscribeTokenCallback)
      } catch {
        print(error.localizedDescription)
        self.showAlertRetry(title: "Could not create streams list", message: "Try again.")
      }
    } else {
      DispatchQueue.main.async {
        self.isViewProgressing = false
      }
    }
  }

  func subscribeTokenCallback(subscribeToken:String?) {
    if let sid = self.selectedStreamId, let token = subscribeToken {
      Phenix.shared.subscribeToStream(streamId:sid,
                                      subscribeStreamToken:token,
                                      subscribeCallback:subscribing)
    } else {
      self.isViewProgressing = false
    }
  }

  func subscribing(success:Bool) {
    self.isViewProgressing = false
    DispatchQueue.main.async {
      if success {
        self.isSubscribing = true
        self.updateParabolaPath()
        if let subscribingStream = Phenix.shared.subscribeStream, Phenix.shared.authSession != nil {
          Phenix.shared.viewStream(stream: subscribingStream,
                                   renderReady: self.viewable,
                                   qualityChanged: self.rendering,
                                   renderStatus: self.renderStatus)
          subscribingStream.setStreamEndedCallback({ (mediaStream,
            phenixStreamEndedReason,
            reasonDescription) in
            self.subscribeView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
            if self.subscribeLayer != nil, !self.isEndedByEnterBackground {
              self.showAlertGoBackToMain()
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

  func viewable(layer: CALayer?) {
    if let videoSublayer = layer {
      self.subscribeLayer = videoSublayer
      self.updatesubscribeLayer()
    }
  }

  func updatesubscribeLayer() {
    guard let f = self.view?.frame else { return }
    if let videoSublayer = self.subscribeLayer {
      videoSublayer.frame = CGRect(x:0,y:0, width:f.width, height:f.height)
      DispatchQueue.main.async {
        self.subscribeView.layer.addSublayer(videoSublayer)
      }
    }
  }

  func showAlertRetry(title: String, message: String) {
    let alert = UIAlertController(title: title, message: message, preferredStyle: UIAlertControllerStyle.alert)
    alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.default, handler: { action in
      self.startPublishing()
    }))
    self.present(alert, animated: true, completion: nil)
  }

  func showAlertGoBackToMain() {
    let alert = UIAlertController(title: "The publisher stopped", message:"", preferredStyle: .alert)
    var isDismissed = false
    self.renderer?.stop()
    self.streamIdLabel.text = ""
    alert.addAction(UIAlertAction(title: "OK", style: .default) {
      _ in
      isDismissed = true
      self.goBackToMain()
    })
    DispatchQueue.main.async {
      self.present(alert, animated: true)
    }
    Utilities.executeOnMainQueueAfterDelay(seconds: 3.0) {
      if isDismissed { return }
      alert.dismiss(animated: true, completion: nil)
      isDismissed = true
      self.goBackToMain()
    }
    self.isEndedByEnterBackground = false
  }

  func goBackToMain() {
    self.subscribeView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    self.selectedStreamId = nil
    self.subscribeLayer = nil
    self.isSubscribing = false
    self.updateParabolaPath()
  }

  func updateParabolaPath() {
    if self.isPublishing {
      self.parabolaType = self.isSubscribing ? .All : .Publish
    } else {
      self.parabolaType = self.isSubscribing ? .Subscribe: .None
    }
    self.drawParabola(path: self.parabolaType)
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
    popController.popoverPresentationController?.sourceView = self.buttonSubscribeOption
    popController.popoverPresentationController?.sourceRect = self.buttonSubscribeOption.bounds
    popController.popoverPresentationController?.backgroundColor = UIColor(red: 0, green: 0, blue: 0, alpha: 0.5)
    popController.preferredContentSize = CGSize(width: 200, height: 120)

    // present the popover
    self.present(popController, animated: true)
  }

  // MARK: Parabola animating
  func drawParabola(path: PathType) {
    self.animationView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    if path == .none {
      return
    }
    let publishPath = Utilities.parabolaPath(pathType: path, viewToDraw: self.animationView, flatness: 0.1)
    parabolaLayer = CAShapeLayer()
    parabolaLayer.path = publishPath.cgPath
    parabolaLayer.fillColor = UIColor.clear.cgColor
    parabolaLayer.strokeColor = PhenixColor.Gray.cgColor
    parabolaLayer.lineWidth = 5.0
    parabolaLayer.strokeEnd = 0.0
    DispatchQueue.main.async {
      self.animationView.layer.addSublayer(self.parabolaLayer)
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
    parabolaLayer.strokeEnd = 1.0

    // Commit animation
    parabolaLayer.add(animation, forKey: "animateParabola")
  }
}

extension ViewController: CapabilityDelegate {
  // delegate to get data back from pop-over
  func selectCapability(selection: CapabilitySelection) {
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
}

final class StreamIdTableViewCell: UITableViewCell {
  @IBOutlet weak var idLabel: UILabel!
  @IBOutlet weak var idGradientView: UIView!
}
