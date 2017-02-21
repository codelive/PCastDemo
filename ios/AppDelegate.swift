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
import SystemConfiguration
import ReachabilitySwift
import Crashlytics
import Fabric

var timestamp = Date()

func timeElapsed() -> String {
  let now = Date()
  let elapsed = now.timeIntervalSince(timestamp)
  timestamp = now
  return String(Int((elapsed) * 1000)) + "ms elapsed"
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

  let isPhenixFirstLaunch = UserDefaults.isPhenixFirstLaunch()
  var window: UIWindow?
  let reachability = Reachability()!
  var isAlertShowing = false

  func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions:[UIApplicationLaunchOptionsKey : Any]?) -> Bool {
    Fabric.with([Crashlytics.self])
    UIApplication.shared.isIdleTimerDisabled = true

    if !reachability.isReachable {
      notifyNetworkNotReachability()
    }

    NotificationCenter.default.addObserver(self, selector: #selector(AppDelegate.reachabilityChanged), name: ReachabilityChangedNotification,object: reachability)
    do {
      try reachability.startNotifier()
    } catch{
      print("could not start reachability notifier")
      print(error.localizedDescription)
      abort()
    }
    return true
  }

  func applicationDidEnterBackground(_ application: UIApplication) {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    reachability.stopNotifier()
  }

  func applicationWillEnterForeground(_ application: UIApplication) {
    // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    do {
      try reachability.startNotifier()
    } catch {
      print("could not start reachability notifier")
      print(error.localizedDescription)
      abort()
    }
  }

  func applicationWillTerminate(_ application: UIApplication) {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    Phenix.shared.shutdown()
    NotificationCenter.default.removeObserver(self)
    reachability.stopNotifier()
  }

  func notifyNetworkNotReachability() {
    let alert = UIAlertController(title: "Phenix", message: "Please connect to the internet", preferredStyle: UIAlertControllerStyle.alert)

    alert.addAction(UIAlertAction(title: "Ok", style: UIAlertActionStyle.default, handler: { [weak self] (alert) in
      self?.window?.rootViewController?.dismiss(animated: true, completion: nil)
      self?.isAlertShowing = false
    }))

    // show the alert
    window?.rootViewController?.present(alert, animated: true, completion: nil)
    isAlertShowing = true
  }

  func reachabilityChanged(note: NSNotification) {
    if reachability.isReachable {
      if isAlertShowing {
        window?.rootViewController?.dismiss(animated: true, completion: nil)
        isAlertShowing = false
      }
    } else {
      notifyNetworkNotReachability()
    }
  }
}

