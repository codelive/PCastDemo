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
import CoreMotion

// todo:use promises, futures
// todo:popup errors instead of print

class ViewController:UIViewController, UITableViewDataSource, UITableViewDelegate {

  // outlets and actions
  @IBOutlet weak var selectStreamHomeView:UIView!
  @IBOutlet weak var previewVideoView:UIView!
  @IBOutlet weak var previewVideoViewSmall:UIView!
  @IBOutlet weak var progress:UIProgressView!
  @IBOutlet weak var status:UILabel!
  @IBOutlet weak var versionNumber: UILabel!
  @IBOutlet weak var currentStreamId:UILabel!
  @IBOutlet weak var birdLogo: UIImageView!
  @IBOutlet weak var switchCamera: UIImageView!
  @IBOutlet weak var idTableView: UITableView!
  @IBOutlet weak var changePreviewSize: UIButton!
  @IBOutlet weak var changePublishState: UIButton!
  @IBOutlet var publishButtonSpaceToBottom: NSLayoutConstraint!
  @IBOutlet var changeCameraButtonTrailing: NSLayoutConstraint!

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
  var isUsingFrontCamera = true
  var isPublishing = false
  var indicator = UIActivityIndicatorView(activityIndicatorStyle:.whiteLarge)
  var pRenderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
  var isFullScreen : Bool = false
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
        DispatchQueue.main.async  {
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
  }

  override func viewDidAppear(_ animated:Bool) {
    if self.isPublishing {
      self.renderPreview()
    }
    self.updateStreamListRequest()
    self.updateStreamListTimer = Timer.scheduledTimer(timeInterval: 5.0, target: self, selector: #selector(updateStreamListRequest), userInfo: nil, repeats: true)
  }

  override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
    super.viewWillTransition(to: size, with: coordinator)
    coordinator.animate(alongsideTransition: nil, completion: {
      _ in
      self.indicator.frame = CGRect(x: 0, y: 0, width: 50, height: 50)
      self.indicator.center = self.view.center
      self.view.addSubview(self.indicator)
      self.enterFullScreen(isFullScreen: self.isFullScreen)
    })
  }

  func addObserve() {
    NotificationCenter.default.addObserver(self, selector: #selector(ViewController.handleAppEnterBackground(notification:)), name: Notification.Name.UIApplicationDidEnterBackground, object: nil)

    NotificationCenter.default.addObserver(self, selector: #selector(ViewController.handleAppEnterForeground(notification:)), name: Notification.Name.UIApplicationWillEnterForeground, object: nil)
  }

  func addPulse(){
    let pulse = Pulsing(numberOfPulses: 1, radius: 200, position: self.birdLogo.center)
    pulse.backgroundColor = UIColor.gray.cgColor
    self.view.layer.insertSublayer(pulse, below: self.birdLogo.layer)
  }

  func updateStreamListRequest() {
    do {
      try Backend.shared.listStreams(done:updateStreamList)
    } catch {
    }
  }

  func updateStreamList(streamList:Array<String>) {
    self.streamIdList = streamList.sorted()
    if isPublishing {
      var i = 0;
      // swap self-Id to first index
      for streamIdFullString in self.streamIdList {
        if streamIdFullString == self.streamIdThisPhone {
          let element = self.streamIdList.remove(at: i)
          self.streamIdList.insert(element, at: 0)
        }
        i += 1
      }
    }
    DispatchQueue.main.async {
      self.idTableView.reloadData()
    }
  }

  func setupSubviews() {
    DispatchQueue.main.async {
      let f = self.previewVideoViewSmall.frame
      self.publishButtonSpaceToBottom.constant = f.size.height - 20
      self.changeCameraButtonTrailing.constant = f.size.width - 20
      self.selectStreamHomeView.layoutIfNeeded()
    }

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
    self.previewVideoView.layer.borderWidth = 1.0
    self.previewVideoView.layer.cornerRadius = 5.0
    self.previewVideoView.layer.masksToBounds = true
    self.previewVideoView.layer.borderColor = UIColor.gray.cgColor
    self.selectStreamHomeView.isHidden = true
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
      Phenix.shared.getLocalUserMedia(mediaReady:mediaReady)
    }
  }

  func mediaReady(success:Bool) {
    self.reportStatus(step:.Media, success:success)
    if success {
      self.pRenderer = Phenix.shared.userMediaStream?.mediaStream.createRenderer()
        if let auSession = Phenix.shared.authSession {
          do {
            try Backend.shared.createStreamToken(sessionId:auSession, originStreamId:nil, capabilities:"streaming", done:publishTokenCallback)
          } catch {
            self.reportStatus(step:.PublishToken, success:false)
          }
        } else {
          // Fail to get Authorization Token, re-login to get the token
          self.login(name:"demo-user", password:"demo-password")
        }
    }
  }

  func publishTokenCallback(publishToken:String?) {
    if let token = publishToken, let stream = Phenix.shared.stream {
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
        self.changePublishState.setBackgroundImage(UIImage(named: "ic-record"), for: UIControlState.normal)
        self.showSplashAnimation = false
        self.idTableView.reloadData()
      }
    }
  }

  func recordButtonAnimation() -> CAAnimationGroup {
    let pulse1 = CASpringAnimation(keyPath: "transform.scale")
    pulse1.duration = 0.6
    pulse1.fromValue = 1.0
    pulse1.toValue = 1.12
    pulse1.autoreverses = true
    pulse1.repeatCount = 1
    pulse1.initialVelocity = 0.5
    pulse1.damping = 0.8
    let animationGroup = CAAnimationGroup()
    animationGroup.duration = 0.6
    animationGroup.repeatCount = Float.infinity
    animationGroup.animations = [pulse1]
    return animationGroup
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
    if Phenix.shared.authSession != nil {
      Phenix.shared.getLocalUserMedia(mediaReady:mediaReady)
    } else {
      self.login(name:"demo-user", password:"demo-password")
    }
  }

  override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
    get {
      return UIInterfaceOrientationMask.all
    }
  }

  func handleAppEnterBackground(notification: Notification) {
    Phenix.shared.authSession = nil
    Phenix.shared.stop()
  }

  func handleAppEnterForeground(notification: Notification) {
    // after background->foreground, camera environment will change to default (.user) automatically
    self.isUsingFrontCamera = true

    // Post notification to StreamingVC
    NotificationCenter.default.post(name: Notification.Name("NotificationBackToHome"), object: nil)

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
      f = self.selectStreamHomeView.bounds
      self.changePreviewSize.setImage(UIImage(named: "ic-full-screen-exit"), for: UIControlState.normal)
      DispatchQueue.main.async {
        self.publishButtonSpaceToBottom.constant = f.size.height - 80
        self.changeCameraButtonTrailing.constant = f.size.width - 30
        self.selectStreamHomeView.layoutIfNeeded()
        self.previewVideoView.frame = f
        self.previewVideoView.layer.borderWidth = 0
        self.previewVideoView.layer.cornerRadius = 0
      }
    } else {
      f = self.previewVideoViewSmall.frame
      self.changePreviewSize.setImage(UIImage(named: "ic-full-screen-enter"), for: UIControlState.normal)
      DispatchQueue.main.async {
        UIView.animate(withDuration: 0.3, animations: {
          self.publishButtonSpaceToBottom.constant = f.size.height - 20
          self.changeCameraButtonTrailing.constant = f.size.width - 20
          self.selectStreamHomeView.layoutIfNeeded()
          self.previewVideoView.frame = f
          self.previewVideoView.layer.borderWidth = 1.0
          self.previewVideoView.layer.cornerRadius = 5.0
          self.previewVideoView.layer.borderColor = UIColor.gray.cgColor
        })
      }
    }

    self.previewVideoView.layer.masksToBounds = true
    if let layer = self.phenixLayer {
      if isFullScreen {
        layer.frame = self.selectStreamHomeView.bounds
      } else {
        layer.frame = self.previewVideoViewSmall.bounds
      }
      self.previewVideoView.layer.addSublayer(layer)
    }
  }

  func renderPreview() {
    self.pRenderer?.stop()
    if self.isPublishing {
      self.publishingAnimation = self.recordButtonAnimation()
      self.pRenderer?.setRenderSurfaceReadyCallback({ (renderer, layer) in
        if let previewLayer = layer {
          self.phenixLayer = previewLayer
          let f = self.previewVideoView.frame
          previewLayer.frame = CGRect(x:0,y:0, width:f.width, height:f.height)
          DispatchQueue.main.async {
            self.switchCamera.image = UIImage(named: "ic_camera_front")
            self.previewVideoView.layer.addSublayer(previewLayer)
            self.changePublishState.layer.add(self.publishingAnimation, forKey: "pulse")
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

      if self.pRenderer?.isAudioMuted == true {
        self.pRenderer?.unmuteAudio()
      }
    }
  }

  // MARK: IBAction
  @IBAction func switchCamera(_ sender: AnyObject) {
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

  @IBAction func changePreviewSizeClicked(_ sender: AnyObject) {
    self.isFullScreen = !self.isFullScreen
    self.enterFullScreen(isFullScreen: self.isFullScreen)
  }

  @IBAction func changePublishStateClicked(_ sender: AnyObject) {
    if self.isPublishing {
      self.isPublishing = false
      self.isFullScreen = false
      self.phenixLayer = nil
      Phenix.shared.userMediaStream = nil
      Phenix.shared.stopPublish()
      Phenix.shared.stopRenderVideo()
      self.changePublishState.setBackgroundImage(UIImage(named: "ic-play"), for: UIControlState.normal)
      self.enterFullScreen(isFullScreen: self.isFullScreen)
      self.changePublishState.layer.removeAllAnimations()
      self.previewVideoView.layer.sublayers?.forEach { $0.removeFromSuperlayer() }
    } else {
      self.isViewProgressing = true
      self.startPublishing()
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
    let gradient = CAGradientLayer()
    let startColor = UIColor(red: 202/255, green: 42/255, blue: 116/255, alpha: 1.0).cgColor
    let endColor = UIColor(red: 137/255, green: 43/255, blue: 96/255, alpha: 1.0).cgColor
    gradient.startPoint = CGPoint(x:0, y:0)
    gradient.endPoint = CGPoint(x:1.0, y:0)
    gradient.frame = CGRect(x: 0.0, y: 0.0, width: tableView.bounds.width, height: cell.contentView.bounds.size.height)
    gradient.colors = [startColor, endColor]
    cell.idGradientView.layer.cornerRadius = 5.0
    cell.idGradientView.layer.masksToBounds = true
    cell.idGradientView.layer.insertSublayer(gradient, at: 0)

    return cell
  }

  // MARK: UITableViewDelegate
  func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
    if let streamIdFullString = self.streamIdList[indexPath.row] {
      self.selectedStreamId = streamIdFullString
      self.performSegue(withIdentifier: "StreamSegue", sender: nil)
    }
  }

  // MARK: Segue
  override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
    if segue.identifier == "StreamSegue" {
      let streamingVC = (segue.destination as! StreamingVC)
      streamingVC.streamId = self.selectedStreamId
      streamingVC.isUsingFrontCamera = self.isUsingFrontCamera
    }
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
