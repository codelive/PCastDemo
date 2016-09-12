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

class Phenix {

  static let shared = Phenix()

  var pcast:PhenixPCast?
  var renderer:PhenixRenderer?
  var stream:PhenixMediaStream?
  var streamId:String?

  typealias SuccessCallback = (Bool) -> ()
  typealias MediaReadyCallback = SuccessCallback
  typealias PublishStreamIDCallback = SuccessCallback
  typealias SubscribeCallback = SuccessCallback
  typealias AuthCallback = (String?) -> ()
  typealias RenderReadyCallback = (CALayer?) -> ()
  typealias QualityChangedCallback = ()->()

  init() {
    if let p = PhenixPCastFactory.createPCast() {
      self.pcast = p
      p.initialize()
    }
  }

  func connectAndAuthenticate(authToken:String, authCallback:@escaping AuthCallback) {
    let pcastAuthCallback = Phenix.pcastAuthCallback(authCallback:authCallback)
    self.pcast?.start(authToken,
              pcastAuthCallback,
              onlineCallback,
              offlineCallback)
  }

  func getLocalUserMedia(mediaReady:@escaping MediaReadyCallback) {
    var gumOptions = PhenixUserMediaOptions()
    PhenixPCastFactory.initializeDefaultUserMediaOptions(&gumOptions)
    gumOptions.video.facingMode = .environment
    self.pcast?.getUserMedia(&gumOptions, Phenix.pcastMediaCallback(mediaCallback:mediaReady))
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

  func viewStream(stream:PhenixMediaStream, renderReady:@escaping RenderReadyCallback, qualityChanged:@escaping QualityChangedCallback) {
    if let r = stream.createRenderer() {
      self.renderer = r
      r.setDataQualityChangedCallback(Phenix.pcastQualityChangedCallback(qualityChanged:qualityChanged))
      r.setRenderSurfaceReadyCallback(Phenix.pcastRenderReadyCallback(renderReadyCallback:renderReady))
      let status = r.start()
      print("renderer start status = \(status)")
    }
  }

  func stop() {
    self.renderer?.stop()
    self.stream?.stop()
    self.pcast?.stop()
  }

  // callbacks

  static func pcastAuthCallback(authCallback:@escaping AuthCallback) -> ((_ pcast:PhenixPCast?, _ status:PhenixRequestStatus, _ sessionId:String?) -> ()) {
    return { pcast, status, sessionId in authCallback(sessionId) }
  }

  static func pcastMediaCallback(mediaCallback:@escaping MediaReadyCallback) -> ((_ pcast:PhenixPCast?, _ status:PhenixRequestStatus, _ stream:PhenixUserMediaStream?) -> ()) {
    return { pcast, status, stream in
      Phenix.shared.stream = stream?.mediaStream
      mediaCallback(status == .ok)
    }
  }

  static func pcastPublishStreamIDCallback(publishStreamIDCallback:@escaping PublishStreamIDCallback) -> ((_ pcast:PhenixPCast?, _ status:PhenixRequestStatus, _ publisher:PhenixPublisher?) -> ()) {
    return { pcast, status, publisher in
      Phenix.shared.streamId = publisher?.streamId
      publishStreamIDCallback(publisher?.streamId != nil)
    }
  }

  static func pcastSubscribeCallback(subscribeCallback:@escaping SubscribeCallback) -> ((_ pcast:PhenixPCast?, _ status:PhenixRequestStatus, _ stream:PhenixMediaStream?) -> ()) {
    return { pcast, status, stream in
      if status == PhenixRequestStatus.ok {
        Phenix.shared.stream = stream
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
