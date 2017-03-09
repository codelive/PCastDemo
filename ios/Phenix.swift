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

final class Phenix {

  static let shared = Phenix()
  var serverOption = 1
  var userMediaLayer: CALayer?
  var publisher: PhenixPublisher?
  var pcast: PhenixPCast?
  var renderer: PhenixRenderer?
  var subscribeStream: PhenixMediaStream?
  var userMediaStream: PhenixUserMediaStream?
  var streamId: String?
  var authSession: String?
  var phenixInfo: ServerLocation!
  var phenixPublishingOption = PublishOption.All

  typealias SuccessCallback = (Bool) -> ()
  typealias MediaReadyCallback = SuccessCallback
  typealias PublishStreamIDCallback = SuccessCallback
  typealias SubscribeCallback = SuccessCallback
  typealias AuthCallback = (String?) -> ()
  typealias RenderStatusCallback = (String?) -> ()
  typealias RenderReadyCallback = (CALayer?) -> ()
  typealias QualityChangedCallback = ()->()

  init() {
    let dataArray = PhenixServerList
    if dataArray.count > 0 {
      let dataDict = dataArray[0]
      phenixInfo = ServerLocation.init(name: dataDict["name"]!, uri: dataDict["uri"]!, http: dataDict["http"]!)
    }
    self.initPcast()
  }

  func initPcast() {
    if self.pcast != nil {
      Phenix.shared.shutdown()
    }
    if let p = PhenixPCastFactory.createPCast(phenixInfo.uri) {
      self.pcast = p
      p.initialize()
    } else {
      PhenixAssert.assert(condition: false, "Failed to init Pcast")
    }
  }

  func connectAndAuthenticate(authToken:String, authCallback:@escaping AuthCallback) {
    let pcastAuthCallback = Phenix.pcastAuthCallback(authCallback:authCallback)
    self.pcast?.start(authToken,
              pcastAuthCallback,
              onlineCallback,
              offlineCallback)
  }

  func getLocalUserMedia(mediaOption: PublishOption, mediaReady:@escaping MediaReadyCallback) {
    let gumOptions = PhenixUserMediaOptions()
    switch mediaOption {
    case .AudioOnly:
      gumOptions.video.enabled = false
    case .VideoOnly:
      gumOptions.audio.enabled = false
    case .None:
      gumOptions.video.enabled = false
      gumOptions.audio.enabled = false
    case .All, .ShareScreen: break
    }
    gumOptions.video.facingMode = .environment
    gumOptions.video.flashMode = .automatic
    self.pcast?.getUserMedia(gumOptions, Phenix.pcastMediaCallback(mediaCallback:mediaReady))
  }

  // Publishes the stream, and gets the streamID
  func getPublishStreamID(publishStreamToken:String, stream:PhenixMediaStream, publishStreamIDCallback:@escaping PublishStreamIDCallback) {
    self.pcast?.publish(
      publishStreamToken,
      stream,
      Phenix.pcastPublishStreamIDCallback(publishStreamIDCallback:publishStreamIDCallback))
  }

  func subscribeToStream(streamId:String, subscribeStreamToken:String, subscribeCallback:@escaping SubscribeCallback) {
    self.pcast?.subscribe(subscribeStreamToken, Phenix.pcastSubscribeCallback(subscribeCallback:subscribeCallback))
  }

  func viewStream(stream:PhenixMediaStream, renderReady:@escaping RenderReadyCallback, qualityChanged:@escaping QualityChangedCallback, renderStatus:@escaping RenderStatusCallback) {
    if let r = stream.createRenderer() {
      self.renderer = r
      r.setDataQualityChangedCallback(Phenix.pcastQualityChangedCallback(qualityChanged:qualityChanged))
      r.setRenderSurfaceReadyCallback(Phenix.pcastRenderReadyCallback(renderReadyCallback:renderReady))
      let status = r.start()
      if status != .ok {
        print("Renderer start status = \(status)")
        let statusString = "\(status)"
        renderStatus(statusString)
      } else {
        renderStatus(nil)
      }
    }
  }

  func stopPublish() {
    self.publisher?.stop("ended")
  }

  func stopSubscribe() {
    self.subscribeStream?.stop()
  }

  func stopRenderVideo() {
    self.renderer?.stop()
  }

  func stop() {
    self.publisher?.stop("ended")
    self.subscribeStream?.stop()
    self.pcast?.stop()
  }

  func shutdown() {
    self.stop()
    self.pcast?.shutdown()
    self.pcast = nil
  }

  // callbacks

  static func pcastAuthCallback(authCallback:@escaping AuthCallback) -> ((_ pcast:PhenixPCast?, _ status:PhenixRequestStatus, _ sessionId:String?) -> ()) {
    return { pcast, status, sessionId in
      if status == PhenixRequestStatus.ok {
        authCallback(sessionId)
      } else {
        authCallback(nil)
      }
    }
  }

  static func pcastMediaCallback(mediaCallback:@escaping MediaReadyCallback) -> ((_ pcast:PhenixPCast?, _ status:PhenixRequestStatus, _ stream:PhenixUserMediaStream?) -> ()) {
    return { pcast, status, stream in
      Phenix.shared.userMediaStream = stream
      mediaCallback(status == .ok)
    }
  }

  static func pcastPublishStreamIDCallback(publishStreamIDCallback:@escaping PublishStreamIDCallback) -> ((_ pcast:PhenixPCast?, _ status:PhenixRequestStatus, _ publisher:PhenixPublisher?) -> ()) {
    return { pcast, status, publisher in
      if status == PhenixRequestStatus.ok {
        Phenix.shared.publisher = publisher
        Phenix.shared.streamId = publisher?.streamId
        publishStreamIDCallback(publisher?.streamId != nil)
        Phenix.shared.publisher?.setPublisherEndedCallback({ (publisher, endedReason, endedReasonDescription) in
          var reasonDescription = "none"
          if let reason = endedReasonDescription {
            reasonDescription = reason
          }
          print("Publish stream ended with reason " + reasonDescription)
        })
      } else {
        publishStreamIDCallback(false)
      }
    }
  }

  static func pcastSubscribeCallback(subscribeCallback:@escaping SubscribeCallback) -> ((_ pcast:PhenixPCast?, _ status:PhenixRequestStatus, _ stream:PhenixMediaStream?) -> ()) {
    return { pcast, status, stream in
      if status == PhenixRequestStatus.ok {
        Phenix.shared.subscribeStream = stream
        subscribeCallback(true)
      } else {
        subscribeCallback(false)
      }
    }
  }

  static func pcastRenderReadyCallback(renderReadyCallback:@escaping RenderReadyCallback) -> ((_ renderer:PhenixRenderer?, _ renderSurface:CALayer?) -> ()) {
    return { renderer, renderSurface in renderReadyCallback(renderSurface) }
  }

  static func pcastQualityChangedCallback(qualityChanged:@escaping QualityChangedCallback) -> ((_ renderer:PhenixRenderer?, _ quality:PhenixDataQualityStatus?, _ reason:PhenixDataQualityReason) -> ()) {
    return { renderer, quality, reason in qualityChanged() }
  }

  func onlineCallback(pcast:PhenixPCast?) {
    print("online")
  }

  func offlineCallback(pcast:PhenixPCast?) {
    print("offline")
  }
}
