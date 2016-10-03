This demo has an iOS app, and an Android app, which talk to the PCast&trade; server.

This demo also has a node.js server that talks to the PCast&trade; server, as  [documented in the official PCast&trade; Platform Documentation](https://phenixp2p.com/docs). The apps are initially configured to use the PCast&trade; server directly, but can be used with this node.js server instead.

The demo apps use git-submodules and git-lfs to get large binary libraries out of other, private repos. So to get all the necessary code, you will need to perform the following steps:  
1. install git-lfs. This needs to be done only once on your computer. Instructions are [here](https://git-lfs.github.com). For Mac, it's just "brew install git-lfs".  
2. after you git-clone this repo, you will need to initialize git-submodule once. To initialize git-submodule, run the script `git-submodule-setup.sh`.

Then you should have all files, both from this repo, as well as the large binaries from the private repos.

To run the iOS app:  
1. in the ios/ directory: open "Phenix Demo.xcodeproj" in Xcode8 (required for Swift3)  
2. in Xcode8, run the app in a physical device, not the simulator.  

To run the Android app:  
1. In Android Studio, open the project in the android/ directory, and run the app in a physical device, not the emulator.  

If you want to run the node.js server (optional)  
1. install [node.js](https://nodejs.org)  
2. in the node/ directory: npm install  
3. to make the app use your node.js server, update the server address in the iOS or Android app code from "https://demo.phenixp2p.com/demoApp/" to your server address.  


