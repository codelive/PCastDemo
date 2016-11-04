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

class ViewController:UIViewController {

  // outlets and actions
  @IBOutlet weak var videoView:UIView!
  @IBOutlet weak var startStreamView: UIView!
  @IBOutlet weak var progress:UIProgressView!
  @IBOutlet weak var status:UILabel!
  @IBOutlet weak var birdLogo: UIImageView!

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

  let NUM_STEPS = 9
  var steps = 0
  var authSession:String?
  var phenixLayer: CALayer?
  let motionManager = CMMotionManager()
  var pulseTimer = Timer()
  var showSplashAnimation: Bool = true{
    didSet {
      if showSplashAnimation == true {
        if self.pulseTimer.isValid {
          self.pulseTimer.invalidate()
        }
        self.pulseTimer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(addPulse), userInfo: nil, repeats: true)
        self.birdLogo.isHidden = false
        self.startStreamView.isHidden = true
      } else {
        self.pulseTimer.invalidate()
        self.birdLogo.isHidden = true
        self.startStreamView.isHidden = false
      }
    }
  }
  // for loopback, the same stream ID is used for both publish and subscribe. But the stream tokens must be different.
  var streamId:String?

  deinit {
    NotificationCenter.default.removeObserver(self  )
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    self.addObserve()
    self.setupSubviews()
  }

  override func viewWillAppear(_ animated:Bool) {
    super.viewWillAppear(animated)
    self.restartProgress()
  }

  override func viewDidAppear(_ animated:Bool) {
    self.reportStatus(step: .Launch, success: true)
    self.login(name:"demo-user", password:"demo-password")
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

  func setupSubviews() {
    self.startStreamView.layer.cornerRadius = 5.0;
    self.startStreamView.isHidden = true;
  }

  func restartProgress() {
    self.videoView.isHidden = true
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
        }
        })
    } catch {
      self.reportStatus(step:.Login, success:false)
    }
  }

  func authCallback(sessionId:String?) {
    self.reportStatus(step:.Auth, success:sessionId != nil)
    if let authSession = sessionId {
      self.authSession = authSession
      Phenix.shared.getLocalUserMedia(mediaReady:mediaReady)
    }
  }

  func mediaReady(success:Bool) {
    self.reportStatus(step:.Media, success:success)
    if success {
      do {
        try Backend.shared.streamToken(sessionId:self.authSession!, originStreamId:nil, done:publishTokenCallback)
      } catch {
        self.reportStatus(step:.PublishToken, success:false)
      }
    }
  }

  func publishTokenCallback(publishToken:String?) {
    self.reportStatus(step:.PublishToken, success:publishToken != nil)
    if let token = publishToken, let stream = Phenix.shared.stream {
      Phenix.shared.getPublishStreamID(publishStreamToken:token, stream:stream, publishStreamIDCallback:publishStreamIDCallback)
    }
  }

  func publishStreamIDCallback(success:Bool) {
    self.reportStatus(step:.PublishStream, success:success)
    if success {
      if let publishingStream = Phenix.shared.streamId, let publishingSession = self.authSession {
        self.streamId = publishingStream
        print("stream id: %@", self.streamId)
        do {
          try Backend.shared.streamToken(sessionId:publishingSession, originStreamId:publishingStream, done:subscribeTokenCallback)
        } catch {
          self.reportStatus(step:.SubscribeToken, success:false)
        }
      }
    }
  }

  func subscribeTokenCallback(subscribeToken:String?) {
    self.reportStatus(step:.SubscribeToken, success:subscribeToken != nil)
    if let sid = self.streamId, let token = subscribeToken {
      Phenix.shared.subscribeToStream(streamId:sid, subscribeStreamToken:token, subscribeCallback:subscribing)
    }
  }

  func subscribing(success:Bool) {
    self.reportStatus(step:.Subscribe, success:success)
    if success {
      DispatchQueue.main.async {
        self.showSplashAnimation = false
        self.startStreamView.isHidden = false;
      }
    }
  }

  func viewable(layer:CALayer?) {
    DispatchQueue.main.async {
      if let videoSublayer = layer {
        self.phenixLayer?.removeFromSuperlayer()
        self.phenixLayer = layer
        let f = self.videoView.frame
        videoSublayer.frame = CGRect(x:0,y:0, width:f.width, height:f.height)
        self.videoView.layer.addSublayer(videoSublayer)
        self.reportStatus(step:.View, success:true)
      } else {
        self.reportStatus(step:.View, success:false)
      }
    }
  }

  func rendering() {
    self.reportStatus(step:.Render, success:true)
  }

  func reportStatus(step:Step, success:Bool) {
    DispatchQueue.main.async {
      let ms = timeElapsed()
      let text = step.rawValue + ": " + (success ? "completed" :"failed")
      NSLog("%@", "Step '" + text + "' " + ms)
      self.progress.setProgress(Float(self.steps) / Float(self.NUM_STEPS), animated:true)
      self.status.text = text
      self.steps = self.steps + 1

      if self.progress.progress == 1.0 {
        self.progress.isHidden = true
        self.status.isHidden = true
      }
    }
  }

  override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
    get {
      return UIInterfaceOrientationMask.all
    }
  }

  func handleAppEnterBackground(notification: Notification) {
    Phenix.shared.stop()
  }

  func handleAppEnterForeground(notification: Notification) {
    self.restartProgress()
    self.reportStatus(step: .Launch, success: true)
    self.login(name:"demo-user", password:"demo-password")
  }

  override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
    super.viewWillTransition(to: size, with: coordinator)
    coordinator.animate(alongsideTransition: nil, completion: {
      _ in

      self.phenixLayer?.frame.size = UIScreen.main.bounds.size
    })
  }

  @IBAction func startStreamClicked(_ sender: AnyObject) {
    self.startStreamView.isHidden = true
    self.videoView.isHidden = false
    if let subscribingStream = Phenix.shared.stream {
      Phenix.shared.viewStream(stream:subscribingStream, renderReady:viewable, qualityChanged:rendering)
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
