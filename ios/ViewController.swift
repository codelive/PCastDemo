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
import MessageUI
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
  @IBOutlet weak var previewProportionalToParentHeight: NSLayoutConstraint!
  @IBOutlet weak var previewProportionalToParentWidth: NSLayoutConstraint!
  @IBOutlet weak var parabolViewProportionalToIdLabelWidth: NSLayoutConstraint!

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
  var popover = SecretUrlPopoverViewController()
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
  var previewRenderer: PhenixRenderer?
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
          if self.isPublishing {
            self.changePreviewSize.isHidden = self.isSubscribing
          }
        }, completion: nil)
      }
    }
  }
  var isPublishing: Bool = true {
    didSet {
      DispatchQueue.main.async {
        if Phenix.shared.phenixPublishingOption == .AudioOnly {
          self.buttonSwitch.isHidden = true
          self.changePreviewSize.isHidden = true
        } else {
          self.buttonSwitch.isHidden = !self.isPublishing
          if self.isSubscribing {
            self.changePreviewSize.isHidden = true
          } else {
            self.changePreviewSize.isHidden = !self.isPublishing
          }
        }
        self.publishStatusStack.isHidden = !self.isPublishing
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

  override var preferredStatusBarStyle: UIStatusBarStyle {
    return .lightContent
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
    self.updateParabolaPath()
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
    self.updateStreamListTimer.invalidate()
  }

  override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
    super.viewWillTransition(to: size, with: coordinator)
    coordinator.animate(alongsideTransition: nil, completion: {
      _ in
      self.updatePopoverFrame()
      if UIDevice.current.orientation.isLandscape {
        self.optionStack.axis = .horizontal
        self.optionStackHeightConstraint = self.optionStackHeightConstraint.newConstraint(multiplier: 0.5)
        self.parabolViewProportionalToIdLabelWidth = self.parabolViewProportionalToIdLabelWidth.newConstraint(multiplier: 0.5)
      } else {
        self.optionStack.axis = .vertical
        self.optionStackHeightConstraint = self.optionStackHeightConstraint.newConstraint(multiplier: 0.66)
        self.parabolViewProportionalToIdLabelWidth = self.parabolViewProportionalToIdLabelWidth.newConstraint(multiplier: 0.8)
      }
      self.indicator.frame = CGRect(x: 0, y: 0, width: 50, height: 50)
      self.indicator.center = self.view.center
      self.view.addSubview(self.indicator)
      self.enterFullScreen(isFullScreen: self.isFullScreen)
      self.idTableView.reloadData()
      self.view.layoutIfNeeded()
      self.updateParabolaPath()
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
    self.previewRenderer?.stop()
    self.previewRenderer = nil
    Phenix.shared.authSession = nil
    Phenix.shared.stopAll()
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
      var i = 0
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
      }, completion: nil)
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
    self.streamIdLabel.layer.cornerRadius = self.streamIdLabel.frame.height / 2
    self.streamIdLabel.layer.masksToBounds = true
    let versionString = getVersionString()
    for buttonTag in 101...102 {
      if let menuButton = self.view.viewWithTag(buttonTag) {
        menuButton.layer.borderColor = UIColor.rgb(red: 200, green: 200, blue: 200, alpha: 1.0).cgColor
        menuButton.layer.cornerRadius = 38/2
        menuButton.layer.borderWidth = 1.0
      }
    }

    self.versionNumber.text = versionString
    self.indicator.frame = CGRect(x: 0, y: 0, width: 50, height: 50)
    self.indicator.center = self.view.center
    self.view.addSubview(self.indicator)
    self.previewVideoView.layer.borderWidth = 2.0
    self.previewVideoView.layer.cornerRadius = 5.0
    self.previewVideoView.layer.masksToBounds = true
    self.previewVideoView.layer.borderColor = UIColor.rgb(red: 200, green: 200, blue: 200, alpha: 1.0).cgColor
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
    self.popover = storyboard?.instantiateViewController(withIdentifier: PhenixStoryboardID.SecretUrlPopover) as! SecretUrlPopoverViewController
    self.popover.secretPopoverDelegate = self
    self.popover.modalPresentationStyle = UIModalPresentationStyle.popover
    self.popover.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    self.updatePopoverFrame()
    self.present(popover, animated: true, completion: nil)
  }

  func updatePopoverFrame() {
    self.popover.popoverPresentationController?.delegate = self
    self.popover.popoverPresentationController?.sourceView = self.view
    self.popover.popoverPresentationController?.sourceRect = self.view.bounds
    self.popover.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection(rawValue: 0)
    self.popover.preferredContentSize = CGSize(width: self.view.bounds.width, height: 350.0)
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
            DispatchQueue.main.async {
              Phenix.shared.connectAndAuthenticate(authToken:authToken, authCallback:self.authCallback)
            }
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
      Phenix.shared.getLocalUserMedia(
          mediaOption: Phenix.shared.phenixPublishingOption,
          facingMode: self.isUsingFrontCamera ? .user : .environment,
          mediaReady:mediaReady)
      self.getStreamList()
    }
  }

  func mediaReady(success:Bool) {
    self.reportStatus(step:.Media, success:success)
    if success {
      self.previewRenderer = nil
      self.previewRenderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
      if Phenix.shared.phenixPublishingOption != .none {
        self.startPublishing()
      }
    }
  }

  func publishTokenCallback(publishToken:String?) {
    if let token = publishToken, let stream = Phenix.shared.userMediaStream?.mediaStream {
      DispatchQueue.main.async {
        Phenix.shared.getPublishStreamID(publishStreamToken:token, stream:stream, publishStreamIDCallback:self.publishStreamIDCallback)
      }
    } else {
      self.isAbleToConnect = false
      self.reportStatus(step:.PublishToken, success:false)
    }
  }

  func publishStreamIDCallback(success:Bool) {
    DispatchQueue.main.async {
      self.reportStatus(step:.PublishStream, success:success)
      if success {
        if let publishingStreamId = Phenix.shared.streamId {
          self.streamIdThisPhone = publishingStreamId
        }
        self.isPublishing = true
        self.isViewProgressing = false
        self.handlePreview()
        self.updateParabolaPath()
        self.showSplashAnimation = false
        self.idTableView.reloadData()
      }
    }
  }

  func publisherDataQualityFeedback() {
    if let phenixPublisher = Phenix.shared.publisher {
      DispatchQueue.main.async {
        phenixPublisher.setDataQualityChangedCallback({ (publisher, qualityStatus, qualityReason) in
          self.imageAudioOnly.isHidden = (qualityStatus == .audioOnly) ? false : true
          self.handleFeedback(qualityReason: qualityReason, qualityStatus: qualityStatus, feedbackType: .Publisher)
        })
      }
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
      DispatchQueue.main.async {
        do {
          var capabilities: [String] = []
          if UIDevice.current.supportsH264Encode {
            capabilities = ["streaming", "fhd", "prefer-h264"]
          } else {
            capabilities = ["streaming"]
          }
          try Backend.shared.createStreamToken(sessionId:auSession,
                                               originStreamId:nil,
                                               capabilities:capabilities,
                                               done:self.publishTokenCallback)
        } catch {
          self.isAbleToConnect = false
          self.reportStatus(step:.PublishToken, success:false)
          print(error.localizedDescription)
          self.showAlertRetry(title: "Could not create stream token", message: "Try again.")
        }
      }
    } else {
      // No authorization token, re-login to get the token
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
    self.previewRenderer?.stop()
    Phenix.shared.authSession = nil
    Phenix.shared.stopAll()
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
      self.previewProportionalToParentWidth = self.previewProportionalToParentWidth.newConstraint(multiplier: 1.0)
      self.previewProportionalToParentHeight = self.previewProportionalToParentHeight.newConstraint(multiplier: 1.0)
      f = self.selectStreamHomeView.bounds
      self.changePreviewSize.setBackgroundImage(UIImage(named: "icon-full-screen-exit"), for: .normal)
      DispatchQueue.main.async {
        self.previewVideoView.layer.borderWidth = 0
        self.previewVideoView.layer.cornerRadius = 0
      }
      self.switchButtonToBottomParent.constant = f.size.height - 50
      self.stopPublishTrailingToParent.constant = f.size.width - 27
    } else {
      f = self.previewVideoViewSmall.frame
      self.changePreviewSize.setBackgroundImage(UIImage(named: "icon-full-screen-enter"), for: .normal)
      DispatchQueue.main.async {
        UIView.animate(withDuration: 0.3, animations: {
          self.previewProportionalToParentWidth = self.previewProportionalToParentWidth.newConstraint(multiplier: 0.25)
          self.previewProportionalToParentHeight = self.previewProportionalToParentHeight.newConstraint(multiplier: 0.25)
          self.previewVideoView.layer.borderWidth = 2.0
          self.previewVideoView.layer.cornerRadius = 5.0
          self.previewVideoView.layer.borderColor = PhenixColor.Gray.cgColor
        })
      }
      self.switchButtonToBottomParent.constant = f.size.height - 27
      self.stopPublishTrailingToParent.constant = f.size.width - 23
    }
  }

  func handlePreview() {
    if Phenix.shared.phenixPublishingOption != .none, let renderer = self.previewRenderer {
      self.publishingAnimation = Utilities.publishButtonAnimation()
      DispatchQueue.main.async {
        self.changePublishState.layer.add(self.publishingAnimation, forKey: "pulse")

        // Listen to and display Viewer stream quality
        renderer.setDataQualityChangedCallback({ (renderer, qualityStatus, qualityReason) in
          self.handleFeedback(qualityReason: qualityReason, qualityStatus: qualityStatus, feedbackType: .Subscriber)
        })

        let status = renderer.start(self.previewVideoView.layer)
        if status == .ok {
          print("Renderer start status .ok")
          self.previewVideoView.isHidden = false
          let bgImage = self.isUsingFrontCamera ? #imageLiteral(resourceName: "icon_camera_front") : #imageLiteral(resourceName: "icon_camera_rear")
          self.buttonSwitch.setBackgroundImage(bgImage, for: .normal)
        } else {
          let statusString = ("\(String(describing: status))")
          let alert = UIAlertController(title: "Preview renderer failed", message:statusString, preferredStyle: .alert)
          alert.addAction(UIAlertAction(title: "OK", style: .default) {
            _ in
          })
          self.present(alert, animated: true)
        }
      }
    } else {
      self.isPublishing = false
      self.isFullScreen = false
      self.changePublishState.layer.removeAllAnimations()
      self.previewVideoView.isHidden = true
    }
  }

  func updateCameraSetting(isFront: Bool) {
    let gumOptions = PhenixUserMediaOptions()
    let bgImage = isFront ? #imageLiteral(resourceName: "icon_camera_front") : #imageLiteral(resourceName: "icon_camera_rear")
    self.buttonSwitch.setBackgroundImage(bgImage, for: .normal)
    gumOptions.video.capabilityConstraints[PhenixDeviceCapability.facingMode.rawValue] = [PhenixDeviceConstraint.initWith(isFront ? .user : .environment)]
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
    Phenix.shared.getLocalUserMedia(
        mediaOption: Phenix.shared.phenixPublishingOption,
        facingMode: self.isUsingFrontCamera ? .user : .environment,
        mediaReady: mediaReady)
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
      self.previewRenderer?.stop()
      self.previewRenderer = nil
      self.updateParabolaPath()
      Phenix.shared.stopPublish()
      Phenix.shared.stopUserMedia()
      self.enterFullScreen(isFullScreen: self.isFullScreen)
      self.changePublishState.layer.removeAllAnimations()
      self.previewVideoView.isHidden = true

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
    if self.streamIdList.count > indexPath.row {
      if let streamIdFullString = self.streamIdList[indexPath.row] {
        self.selectedStreamId = streamIdFullString
        self.streamIdLabel.text = self.shortenStreamId(streamId: streamIdFullString)
      }
    } else {
      self.streamIdLabel.text = "Unavailable"
    }
    self.selectCapability(selection: self.currentCapability)
  }

  func changeCapability(capability:String) {
    DispatchQueue.main.async {
      self.showSplashAnimation = false
    }
    self.isViewProgressing = true
    if let publishingSession = Phenix.shared.authSession, let streamIdFullString = self.selectedStreamId {
      DispatchQueue.main.async {
        do {
          let capabilities = [capability]
          try Backend.shared.createStreamToken(sessionId: publishingSession,
                                               originStreamId: streamIdFullString,
                                               capabilities: capabilities,
                                               done: self.subscribeTokenCallback)
        } catch {
          print(error.localizedDescription)
          self.showAlertRetry(title: "Could not create streams list", message: "Try again.")
        }
      }
    } else {
      DispatchQueue.main.async {
        self.isViewProgressing = false
      }
    }
  }

  func subscribeTokenCallback(subscribeToken:String?) {
    if let sid = self.selectedStreamId, let token = subscribeToken {
      DispatchQueue.main.async {
        Phenix.shared.subscribeToStream(streamId:sid,
                                        subscribeStreamToken:token,
                                        subscribeCallback:self.subscribing)
      }
    } else {
      self.isViewProgressing = false
    }
  }

  func subscribing(success:Bool) {
    self.isViewProgressing = false
    DispatchQueue.main.async {
      if success {
        Phenix.shared.stopRenderVideo()
        self.isSubscribing = true
        self.updateParabolaPath()
        if let subscribingStream = Phenix.shared.subscribeStream, Phenix.shared.authSession != nil {
          Phenix.shared.viewStream(stream: subscribingStream,
                                   renderLayer: self.subscribeView.layer,
                                   qualityChanged: self.rendering,
                                   renderStatus: self.renderStatus)
          subscribingStream.setStreamEndedCallback({ (mediaStream,
            phenixStreamEndedReason,
            reasonDescription) in
            DispatchQueue.main.async {
              if !self.isEndedByEnterBackground {
                self.showAlertGoBackToMain()
              }
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
    self.selectedStreamId = nil
    self.isSubscribing = false
    Phenix.shared.stopRenderVideo()
    Phenix.shared.stopSubscribe()
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
    popController.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection(rawValue: 0)
    popController.popoverPresentationController?.sourceView = self.buttonSubscribeOption
    popController.popoverPresentationController?.sourceRect = CGRect.init(x: 0, y: 10, width: 130, height: 200)
    popController.preferredContentSize = CGSize(width: 120, height: 170)

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
    parabolaLayer.lineWidth = 6.0
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

final private class LogItemSource: NSObject, UIActivityItemSource {
  init (_ messages: String, viewController: ViewController) {
    self.viewController = viewController

    super.init()

    self.logFile = URL(fileURLWithPath: NSTemporaryDirectory())
        .appendingPathComponent(self.getPhenixLogsFileName())
        .appendingPathExtension("txt")

    try? messages.write(to: self.logFile!, atomically: true, encoding: String.Encoding.ascii)
  }

  deinit {
    if let logFile = self.logFile {
      DispatchQueue.global(qos: .utility).async { [logFile] in
        try? FileManager.default.removeItem(at: logFile)
      }
    }
  }

  func activityViewControllerPlaceholderItem(_ activityViewController: UIActivityViewController) -> Any {
    return "Phenix log"
  }

  func activityViewController(
      _ activityViewController: UIActivityViewController, itemForActivityType activityType: UIActivityType) -> Any? {
    return self.logFile
  }

  func activityViewController(
      activityViewController: UIActivityViewController, subjectForActivityType activityType: String?) -> String {
    return "Logs from \(self.viewController.getAppName()) - \(self.viewController.getVersionString())"
  }

  private let viewController: ViewController
  private var logFile: URL? = nil

  private func getPhenixLogsFileName() -> String {
    return "\(self.viewController.getAppName()) - \(self.viewController.getVersionString()) - Phenix Log"
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

extension ViewController: SecretUrlPopoverDelegate, MFMailComposeViewControllerDelegate {
  func shareLogFile() {
    var paths = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)
    let documentsDirectory = paths[0]
    let fileName = PhenixNameForLogFile
    let logFilePath = (documentsDirectory as NSString).appendingPathComponent(fileName)
    let logFileUrl = NSURL(fileURLWithPath: logFilePath);

    let activityViewController = UIActivityViewController(
        activityItems: [LogItemSource(getPhenixLogs(), viewController: self), logFileUrl], applicationActivities: nil);
    self.present(activityViewController, animated: true, completion: nil);
  }

  func getAppName() -> String {
    return Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String ?? "Unknown app"
  }

  func getVersionString() -> String {
    let infoDictionary = Bundle.main.infoDictionary
    var versionString = ""
    if let version = infoDictionary?["CFBundleShortVersionString"] as? String {
      versionString = "v" + version
    }

    if let build = infoDictionary?["CFBundleVersion"] as? String {
      versionString += " (\(build))"
    }

    return versionString
  }

  private func getPhenixLogs() -> String {
    var messagesString = ""
    guard let pcast = Phenix.shared.pcast else {
      return messagesString
    }

    let dispatchGroup = DispatchGroup();
    dispatchGroup.enter()
    pcast.collectLogMessages { (pcast, status, messages) in
      if status == .ok, let messages = messages {
        messagesString += messages
      }
      dispatchGroup.leave()
    }
    dispatchGroup.wait()

    return messagesString
  }
}

final class StreamIdTableViewCell: UITableViewCell {
  @IBOutlet weak var idLabel: UILabel!
  @IBOutlet weak var idGradientView: UIView!
}
