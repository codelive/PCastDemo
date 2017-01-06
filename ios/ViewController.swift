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

enum PublishingOption {
  case audio        // Audio only
  case video        // Video only
  case all          // Audio & Video
  case share        // Screen Share
}

final class ViewController : UIViewController, UITableViewDataSource, UITableViewDelegate, UIPopoverPresentationControllerDelegate {

  // outlets and actions
  @IBOutlet weak var selectStreamHomeView:UIView!
  @IBOutlet weak var previewVideoView:UIView!
  @IBOutlet weak var previewVideoViewSmall:UIView!
  @IBOutlet weak var publishingOptionView:UIView!
  @IBOutlet weak var progress:UIProgressView!
  @IBOutlet weak var status:UILabel!
  @IBOutlet weak var versionNumber: UILabel!
  @IBOutlet weak var birdLogo: UIImageView!
  @IBOutlet weak var idTableView: UITableView!
  @IBOutlet weak var changePreviewSize: UIButton!
  @IBOutlet weak var changePublishState: UIButton!
  @IBOutlet weak var buttonSwitch: UIButton!
  @IBOutlet weak var optionStack: UIStackView!
  @IBOutlet weak var publishStatusStack: UIStackView!
  @IBOutlet weak var optionStackHeightConstraint: NSLayoutConstraint!
  @IBOutlet weak var switchButtonToBottomParent: NSLayoutConstraint!
  @IBOutlet weak var stopPublishTrailingToParent: NSLayoutConstraint!
  @IBOutlet weak var previewProporsionalToParentHeight: NSLayoutConstraint!
  @IBOutlet weak var previewProporsionalToParentWidth: NSLayoutConstraint!
  @IBOutlet weak var animationView: UIView!

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
  var publishingAnimation = CAAnimationGroup()
  let NUM_STEPS = 9
  var steps = 0
  var selectedStreamId:String?
  var phenixLayer: CALayer?
  var pulseTimer = Timer()
  var updateStreamListTimer = Timer()
  var streamIdList = Array<String?>()
  var previewRecognizer:UIGestureRecognizer?
  var isUsingFrontCamera = false
  var indicator = UIActivityIndicatorView(activityIndicatorStyle:.whiteLarge)
  var renderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
  var isFullScreen : Bool = false
  var isPublishing: Bool = true {
    didSet {
      DispatchQueue.main.async {
        self.buttonSwitch.isHidden = !self.isPublishing
        self.publishStatusStack.isHidden = !self.isPublishing
        self.changePreviewSize.isHidden = !self.isPublishing
        self.changePublishState.isHidden = !self.isPublishing
        self.publishingOptionView.isHidden = self.isPublishing
        self.animationView.isHidden = !self.isPublishing
      }
    }
  }
  var showSplashAnimation: Bool = true {
    didSet {
      if showSplashAnimation == true {
        if self.pulseTimer.isValid {
          self.pulseTimer.invalidate()
        }
        self.pulseTimer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(addPulse), userInfo: nil, repeats: true)
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

  // for loopback, the same stream ID is used for both publish and subscribe. But the stream tokens must be different.
  var streamIdThisPhone:String?

  deinit {
    NotificationCenter.default.removeObserver(self)
    self.updateStreamListTimer.invalidate()
    self.pulseTimer.invalidate()
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    self.addObserve()
    self.setupSubviews()
    self.restartProgress()
    self.startPublishing()
    self.navigationController?.navigationBar.setBackgroundImage(UIImage(), for: UIBarMetrics.default)
    self.navigationController?.navigationBar.shadowImage = UIImage()
    self.navigationController?.navigationBar.isTranslucent = true
    self.idTableView.delegate = self
    self.idTableView.dataSource = self
  }

  override func viewWillAppear(_ animated:Bool) {
    super.viewWillAppear(animated)
    if let publishingStreamId = Phenix.shared.streamId {
      self.streamIdThisPhone = publishingStreamId
      self.idTableView.reloadData()
    }
  }

  override func viewDidAppear(_ animated:Bool) {
    self.drawParabola()
    if self.isPublishing {
      self.renderPreview()
    }
    self.updateStreamListRequest()
    self.updateStreamListTimer = Timer.scheduledTimer(timeInterval: 5.0, target: self, selector: #selector(updateStreamListRequest), userInfo: nil, repeats: true)
    self.enterFullScreen(isFullScreen: self.isFullScreen)
  }

  override func viewWillDisappear(_ animated: Bool) {
    super.viewWillDisappear(animated)
    self.phenixLayer = nil
    self.previewVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    self.updateStreamListTimer.invalidate()
  }

  override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
    super.viewWillTransition(to: size, with: coordinator)
    coordinator.animate(alongsideTransition: nil, completion: {
      _ in
      self.drawParabola()
      if UIDevice.current.orientation.isLandscape {
        self.optionStack.axis = .horizontal
        self.optionStackHeightConstraint = self.optionStackHeightConstraint.setMultiplier(multiplier: 0.5)
      } else {
        self.optionStack.axis = .vertical
        self.optionStackHeightConstraint = self.optionStackHeightConstraint.setMultiplier(multiplier: 0.66)
      }
      self.indicator.frame = CGRect(x: 0, y: 0, width: 50, height: 50)
      self.indicator.center = self.view.center
      self.view.addSubview(self.indicator)
      self.enterFullScreen(isFullScreen: self.isFullScreen)
      self.idTableView.reloadData()
    })
  }

  func addObserve() {
    NotificationCenter.default.addObserver(self, selector: #selector(ViewController.handleAppEnterBackground(notification:)), name: Notification.Name.UIApplicationDidEnterBackground, object: nil)

    NotificationCenter.default.addObserver(self, selector: #selector(ViewController.handleAppEnterForeground(notification:)), name: Notification.Name.UIApplicationWillEnterForeground, object: nil)
  }

  func addPulse() {
    let pulse = Pulsing(numberOfPulses: 1, radius: 200, position: self.birdLogo.center)
    pulse.backgroundColor = UIColor.gray.cgColor
    self.view.layer.insertSublayer(pulse, below: self.birdLogo.layer)
  }

  func updateStreamListRequest() {
    self.animateParabola(duration: 4.8)
    do {
      try Backend.shared.listStreams(done:updateStreamList)
    } catch {
      print("Could not get streams list \(error)")
    }
  }

  func updateStreamList(streamList:Array<String>) {
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

  func setupSubviews() {
    var versionString = ""

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
  }

  func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
    return UIModalPresentationStyle.none
  }

  func restartProgress() {
    self.showSplashAnimation = true
    self.steps = 0
    self.progress.setProgress(0.0, animated:false)
    self.progress.setNeedsDisplay()
  }

  func login(name:String, password:String) {
    do {
      try Backend.shared.login(name:name, password:password, done:{ [weak self] success in
        self?.reportStatus(step:.Login, success:success)
        if success {
          if let authToken = Backend.shared.authToken, let callback = self?.authCallback {
            Phenix.shared.connectAndAuthenticate(authToken:authToken, authCallback:callback)
            return
          }
        } else {
          self?.isViewProgressing = false
        }
      })
    } catch {
      self.reportStatus(step:.Login, success:false)
    }
  }

  func authCallback(sessionId:String?) {
    self.reportStatus(step:.Auth, success:sessionId != nil)
    if let authSession = sessionId {
      Phenix.shared.authSession = authSession
      Phenix.shared.getLocalUserMedia(mediaOption: .all, mediaReady:mediaReady)
    }
  }

  func mediaReady(success:Bool) {
    self.reportStatus(step:.Media, success:success)
    if success {
      self.renderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
      self.startPublishing()
    }
  }

  func publishTokenCallback(publishToken:String?) {
    if let token = publishToken, let stream = Phenix.shared.userMediaStream?.mediaStream {
      Phenix.shared.getPublishStreamID(publishStreamToken:token, stream:stream, publishStreamIDCallback:publishStreamIDCallback)
    } else {
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
      self.renderPreview()
      DispatchQueue.main.async {
        self.showSplashAnimation = false
        self.idTableView.reloadData()
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

  func startPublishing() {
    if let auSession = Phenix.shared.authSession {
      do {
        let arrCap = ["streaming", "archive"]
        try Backend.shared.createStreamToken(sessionId:auSession, originStreamId:nil, capabilities:arrCap, done:publishTokenCallback)
      } catch {
        self.reportStatus(step:.PublishToken, success:false)
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

  func handleAppEnterBackground(notification: Notification) {
    self.renderer?.stop()
    Phenix.shared.authSession = nil
    Phenix.shared.stop()
  }

  func handleAppEnterForeground(notification: Notification) {
    // Post notification to StreamingVC
    NotificationCenter.default.post(name: Notification.Name(PhenixName.BackToHomeNotification), object: nil)
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
    self.previewVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    var f = CGRect()
    if isFullScreen {
      self.previewProporsionalToParentWidth = self.previewProporsionalToParentWidth.setMultiplier(multiplier: 1.0)
      self.previewProporsionalToParentHeight = self.previewProporsionalToParentHeight.setMultiplier(multiplier: 1.0)
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
          self.previewProporsionalToParentWidth = self.previewProporsionalToParentWidth.setMultiplier(multiplier: 0.25)
          self.previewProporsionalToParentHeight = self.previewProporsionalToParentHeight.setMultiplier(multiplier: 0.25)
          self.previewVideoView.layer.borderWidth = 2.0
          self.previewVideoView.layer.cornerRadius = 5.0
          self.previewVideoView.layer.borderColor = PhenixColor.Gray.cgColor
        })
      }
      self.switchButtonToBottomParent.constant = f.size.height - 27
      self.stopPublishTrailingToParent.constant = f.size.width - 25
    }
    DispatchQueue.main.async {
      if let layer = self.phenixLayer {
        if isFullScreen {
          layer.frame = self.selectStreamHomeView.bounds
        } else {
          layer.frame = self.previewVideoViewSmall.bounds
        }
        self.previewVideoView.layer.addSublayer(layer)
      }
      self.previewVideoView.layer.masksToBounds = true
      self.view.updateConstraintsIfNeeded()
      self.view.layoutIfNeeded()
    }
  }

  func renderPreview() {
    self.renderer?.stop()
    self.previewVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    self.renderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
    if self.isPublishing {
      self.publishingAnimation = Utilities.publishButtonAnimation()
      DispatchQueue.main.async {
        self.changePublishState.layer.add(self.publishingAnimation, forKey: "pulse")
      }
      self.renderer?.setRenderSurfaceReadyCallback({ (renderer, layer) in
        if let previewLayer = layer {
          self.phenixLayer = previewLayer
          let f = self.previewVideoView.frame
          previewLayer.frame = CGRect(x:0,y:0, width:f.width, height:f.height)
          DispatchQueue.main.async {
            self.previewVideoView.layer.addSublayer(previewLayer)
            let bgImage = self.isUsingFrontCamera ? #imageLiteral(resourceName: "icon-camera-front") : #imageLiteral(resourceName: "icon-camera-rear")
            self.buttonSwitch.setBackgroundImage(bgImage, for: .normal)
          }
        }
      })

      // Listen to and display Viewer stream quality
      self.renderer?.setDataQualityChangedCallback({ (renderer, qualityStatus, qualityReason) in
        var barTye = StatusBarType.max
        if qualityStatus == .noData || !self.isPublishing {
          barTye = .none
        } else if qualityStatus == .all  {
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
        DispatchQueue.main.async {
          self.updateSatusBar(barTye: barTye)
        }
      })

      let status = self.renderer?.start()
      if status == nil {
        return
      }
      if status == .ok {
        print("Renderer start status .ok")
      } else if status == .conflict {
        // Conflict due to previous renderer is available, no need to start a new one.
        print("Renderer start status .conflict")
      } else {
        let statusString = ("\(status)")
        let alert = UIAlertController(title: "Renderer failed", message:statusString, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default) {
          _ in
        })
        self.present(alert, animated: true)
      }
    }
  }

  func updateSatusBar(barTye: StatusBarType) {
    var barColor = UIColor.clear
    switch barTye {
    case .min: barColor = PhenixColor.Red
    case .half: barColor = PhenixColor.Orange
    case .third, .max: barColor = PhenixColor.Blue
    case .none: barColor = .clear
    }

    let currentMaxBarTag = 2210 + barTye.rawValue
    for barNumber in 1...4 {
      // Set the bars visible and colors by tag
      // 2211..2214: Publish image bars
      let imageBarTag = 2210 + barNumber
      if let imageBar = self.view.viewWithTag(imageBarTag) as? UIImageView {
        if imageBarTag <= currentMaxBarTag {
          imageBar.backgroundColor = barColor
        } else {
          imageBar.backgroundColor = .clear
        }
      }
    }
  }

  func updateCameraSetting(isFront: Bool) {
    let gumOptions = PhenixUserMediaOptions()
    let bgImage = isFront ? #imageLiteral(resourceName: "icon-camera-front") : #imageLiteral(resourceName: "icon-camera-rear")
    self.buttonSwitch.setBackgroundImage(bgImage, for: .normal)
    gumOptions.video.facingMode = isFront ? .user : .environment
    Phenix.shared.userMediaStream?.apply(gumOptions)
  }

  // MARK: IBAction
  @IBAction func startPublishingTouched(_ sender: AnyObject) {
    self.isViewProgressing = true
    if self.isPublishing {
      self.isViewProgressing = false
      return
    }
    switch sender.tag {
    case 1001: Phenix.shared.getLocalUserMedia(mediaOption: .audioOnly, mediaReady: mediaReady)
    case 1002: Phenix.shared.getLocalUserMedia(mediaOption: .videoOnly, mediaReady: mediaReady)
    case 1003: Phenix.shared.getLocalUserMedia(mediaOption: .all, mediaReady: mediaReady)
    default: break
    }
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
      self.phenixLayer = nil
      Phenix.shared.stopPublish()
      Phenix.shared.stopRenderVideo()
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

  // MARK: UITableViewDelegate
  func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
    if let streamIdFullString = self.streamIdList[indexPath.row] {
      self.selectedStreamId = streamIdFullString
      self.performSegue(withIdentifier: PhenixSegue.StreamSegue, sender: nil)
    }
  }

  // MARK: Segue
  override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
    if segue.identifier == PhenixSegue.StreamSegue {
      let streamingVC = (segue.destination as! StreamingVC)
      streamingVC.streamId = self.selectedStreamId
      streamingVC.isPublishing = self.isPublishing
      streamingVC.isUsingFrontCamera = self.isUsingFrontCamera
    }
  }

  // MARK: Parabola animating
  var paraLayer: CAShapeLayer!
  func drawParabola() {
    self.animationView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    let publishPath = Utilities.parabolaPath(pathType: .publish, viewToDraw: self.animationView, flatness: 0.1)
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
    paraLayer.add(animation, forKey: "animateParabola")
  }
}

class Pulsing: CALayer {

  var animationGroup = CAAnimationGroup()
  var initialPulseScale:Float = 0
  var nextPulseAfter:TimeInterval = 2.0
  var animationDuration:TimeInterval = 2.5
  var radius:CGFloat = 200
  var numberOfPulses:Float = Float.infinity

  override init(layer: Any) {
    super.init(layer: layer)
  }

  required init?(coder aDecoder: NSCoder) {
    super.init(coder: aDecoder)
  }

  init (numberOfPulses:Float = Float.infinity, radius:CGFloat, position:CGPoint) {
    super.init()

    self.backgroundColor = UIColor.black.cgColor
    self.contentsScale = UIScreen.main.scale
    self.opacity = 0
    self.radius = radius
    self.numberOfPulses = numberOfPulses
    self.position = position
    self.bounds = CGRect(x: 0, y: 0, width: radius * 2, height: radius * 2)
    self.cornerRadius = radius

    DispatchQueue.global(qos: DispatchQoS.QoSClass.default).async {
      self.setupAnimationGroup()
      DispatchQueue.main.async {
        self.add(self.animationGroup, forKey: "pulse")
      }
    }
  }

  func createScaleAnimation () -> CABasicAnimation {
    let scaleAnimation = CABasicAnimation(keyPath: "transform.scale.xy")
    scaleAnimation.fromValue = NSNumber(value: initialPulseScale)
    scaleAnimation.toValue = NSNumber(value: 1)
    scaleAnimation.duration = animationDuration

    return scaleAnimation
  }

  func createOpacityAnimation() -> CAKeyframeAnimation {
    let opacityAnimation = CAKeyframeAnimation(keyPath: "opacity")
    opacityAnimation.duration = animationDuration
    opacityAnimation.values = [0.4, 0.8, 0]
    opacityAnimation.keyTimes = [0, 0.2, 1]

    return opacityAnimation
  }

  func setupAnimationGroup() {
    self.animationGroup = CAAnimationGroup()
    self.animationGroup.duration = animationDuration + nextPulseAfter
    self.animationGroup.repeatCount = numberOfPulses
    let defaultCurve = CAMediaTimingFunction(name: kCAMediaTimingFunctionDefault)
    self.animationGroup.timingFunction = defaultCurve
    self.animationGroup.animations = [createScaleAnimation(), createOpacityAnimation()]
  }
}

class StreamIdTableViewCell: UITableViewCell {
  @IBOutlet weak var idLabel: UILabel!
  @IBOutlet weak var idGradientView: UIView!
}
