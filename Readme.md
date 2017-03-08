# Phenix™

[![Phenix logo](http://phenixp2p.com/public/images/connect-2400x1600.jpg)](http://phenixp2p.com/#realtime)

This repository is part of the source code of Phenix. You can find more information at [phenixp2p.com](http://phenixp2p.com/) or by contacting info@phenixp2p.com.

You can find the published source code at [github.com/PhenixP2P](https://github.com/PhenixP2P).

This demo has an iOS app, and an Android app, which talk to the **PCast™** server.

This demo also has a node.js server that talks to the **PCast™** server, as [documented in the official PCast™ Platform Documentation](https://phenixp2p.com/docs). The apps are initially configured to use the PCast™ server directly, but can be used with this node.js server instead.

The demo apps use **git-submodules** and **git-lfs** to get large binary libraries out of other, private repos. So to get all the necessary code, you will need to perform the following steps:
# Step 1

## 1. install git-lfs. This only needs to be done once per computer.
Instructions are [here](https://git-lfs.github.com/)
- For Mac, it's just `brew install git-lfs`.
- For Windows, navigate to [git-lfs.github.com](https://git-lfs.github.com/) and click Download.
  - On your computer, locate the downloaded file
  - Double click on the file called git-lfs-windows-1.X.X.exe, where 1.X.X is replaced with the Git LFS version you downloaded. When you open this file Windows will run a setup wizard to install Git LFS.
  - Open Git Bash.
  - Verify that the installation was successful: `git lfs install`

## 2. after you git-clone this repo, you will need to initialize git-submodule once
To initialize git-submodule, run the script `git-submodule-setup.sh`.
Then you should have all files, both from this repo, as well as the large binaries from the private repos.

# Step 2

## Prerequisites
In order to build **PCast™ Demo** for Android locally, it is necessary to install the following tools on the local machine:
- JDK 8
- Android SDK
In order to build **PCast™ Demo** for IOS
- Swift 3.0
- Xcode 8.0 or later

## To run the Android app:
1. In Android Studio, open the project in the android/ directory, and run the app on a physical device, not the emulator.
_Note: please press the button "**Sync Project with Gradles Files**" on Android Studio menu bar, in case of build error._

## To run the iOS app:
1. One time only: Ensure Cocoapods is installed via gem: "sudo gem install cocoapods"
2. In the ios/ directory: run "pod install" and then open "PCastDemo.xcworkspace" in Xcode8
3. In Xcode8, run the app in a physical device, not the simulator.

## If you want to run the node.js server (optional)
1. Install [node.js](https://nodejs.org/)
2. In the node/ directory: npm install
3. To make the app use your node.js server, update the server address in the iOS or Android app code from "[https://demo.phenixp2p.com/demoApp/](https://demo.phenixp2p.com/demoApp/)" to your server address.

