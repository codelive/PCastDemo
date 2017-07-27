This is a sample backend server. It internally uses the **PCast™** [admin API](https://phenixp2p.com/docs/#admin-api) and is providing REST paths for authentication, obtaining of stream tokens, and listing of active streams.

For it to be fully functional, you must ensure that port 8081 is publicly visible and properly forwarded to your backend server, so that it can subscribe to **PCast™** stream notifications.

To run, install node.js and then:
$ npm install
$ npm start -- -application-id=\<your-application-id> -secret=\<your-secret>
