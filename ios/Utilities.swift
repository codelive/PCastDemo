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
import SceneKit

public enum PathType {
  case Publish
  case Subscribe
  case All
  case None
}

final class Utilities {
  // Prevent object construction
  @available(*, unavailable, message: "'Utilities' cannot be constructed because it has no accessible initializers")
  init() {}

  //Define 2D path for the parabola shape
  public static func parabolaPath(pathType: PathType, viewToDraw: UIView, flatness: CGFloat) -> UIBezierPath {
    let path = UIBezierPath()
    let viewWidth = viewToDraw.bounds.size.width
    let viewHeight = viewToDraw.bounds.size.height
    let startPoint = CGPoint(x: 16, y: viewHeight)
    let endPoint = CGPoint(x: viewWidth - 16, y: viewHeight)
    let controlPoint = CGPoint(x: viewWidth * 0.5, y: viewHeight * -1)
    switch pathType {
    case .All:
      path.move(to: startPoint)
      path.addQuadCurve(to: endPoint, controlPoint: controlPoint)
    case .Publish:
      path.move(to: startPoint)
      path.addQuadCurve(to: CGPoint(x: viewWidth * 0.5, y: 0), controlPoint: CGPoint(x: viewWidth * 0.3, y: viewHeight * -0.1))
    case .Subscribe:
      path.move(to: CGPoint(x: viewWidth * 0.5, y: 0))
      path.addQuadCurve(to: endPoint, controlPoint: CGPoint(x: viewWidth - (viewWidth * 0.3), y: viewHeight * -0.1))
    case .None:
      break
    }
    path.flatness = flatness

    return path
  }

  public static func createGradientLayer(frame: CGRect, colors: [CGColor]) -> CAGradientLayer {
    let layer = CAGradientLayer()
    layer.frame = frame
    layer.colors = colors
    return layer
  }

  public static func executeOnMainQueueAfterDelay(seconds: Double, closure: @escaping ()->()) {
    DispatchQueue.main.asyncAfter(deadline: .now() + seconds) {
      closure()
    }
  }

  public static func publishButtonAnimation() -> CAAnimationGroup {
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
}
