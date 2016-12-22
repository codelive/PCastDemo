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

const listenPort = 8081

var argv = require('yargs')
    .usage('Usage: npm start -- --application-id=<your-application-id> --secret=<your-secret>')
    .demand(['application-id', 'secret'])
    .argv;

var appId = argv.applicationId, secret = argv.secret;
console.log('You entered: application-id=' + appId + ' ,secret=' + secret);

var request = require('request');
var express = require('express');
var bodyParser = require('body-parser');
var backoff = require('backoff');
var publicIp = require('public-ip');
require('log-timestamp');

var app = express();
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

var adminURI = 'http://pcast.phenixp2p.com/pcast/';
var validCredentials = {'demo-user': 'demo-password'};

// to test from the command line: curl --data "name=demo-username&password=demo-password" 127.0.0.1:8081/login
app.post('/login', function (req, res) {
    console.log('POST login');
    var name = req.body.name;
    var password = req.body.password;
    if (validCredentials[name] !== password) {
        console.log('\tinvalid credentials: ' + Object.keys(req.body));
        res.sendStatus(403);
    } else {
        requestWithBackoff(adminAuth, req, res);
    }
});

function requestWithBackoff(requester, req, res) {
    var call = backoff.call(requester, req, res, function(err, res) {
        console.log('Num retries: ' + call.getNumRetries());
        if (err) {
            res.sendStatus(err.status);
        } else {
            console.log('Status: ' + res.statusCode);
        }
    });
    call.retryIf(function(err) { return err.status >= 500 && err.status != 503; });
    call.setStrategy(new backoff.FibonacciStrategy({initialDelay: 1000}));
    call.failAfter(3);
    call.start();
}

function adminAuth(req, res) {
    request({
        url: adminURI + 'auth',
        method: 'POST',
        json: {
            'applicationId': appId,
            'secret': secret
        }
    }, makeHandler(res));
}

function makeHandler(res) {
    var handler = (function() {
        var responseToSend = res;
        return function(error, response, body) {
            if (!error && response.statusCode === 200) {
                console.log('\tsuccess');
                responseToSend.send(body);
            } else if (response === undefined) {
                console.log('\tno response from server');
                responseToSend.sendStatus(500);
            } else {
                console.log('\tfail:' + response.statusCode);
                responseToSend.sendStatus(response.statusCode);
            }
        };
    })();
    return handler;
}

app.post('/stream', function (req, res) {
    console.log('POST pcast/stream');
    requestWithBackoff(adminStream, req, res);
});

function adminStream(req, res) {
    request({
        url: adminURI + 'stream',
        method: 'POST',
        json: {
            'applicationId': appId,
            'secret': secret,
            'sessionId': req.body.sessionId,
            'originStreamId': req.body.originStreamId,
            'capabilities': req.body.capabilities
        },
    }, makeHandler(res));
}

app.get('/streams', function (req, res) {
    res.send(JSON.stringify(Array.from(activeStreams)));
});

function listenForStreams(port) {
    publicIp.v4().then(ip => {
        console.log('My public IP is ' + ip +':'+ port);
        request({
            url: adminURI + 'app/callback',
            method: 'PUT',
            json: {
                'applicationId': appId,
                'secret': secret,
                'callback': {'host': ip, 'port': port, 'path': '/notification'}
            }
        }, function (err, response, body) {
            if (err) {
               return console.error('Setup notification failed:', err);
            } else if (response.statusCode != 200) {
                return console.error('Setup notification failed:', response.statusCode, body);
            } else {
                console.log('Setup notification succeeded, server responded with:', body);
            }
        });
    });
}

var activeStreams = new Set();
app.post('/notification', function(req, res) {
    console.log('POST notification ' + req.body.entity +','+ req.body.what +','+ req.body.data.streamId);
    if (req.body.entity === 'stream') {
        var streamId = req.body.data.streamId;
        if (req.body.what === 'started') {
            activeStreams.add(streamId);
        } else if (req.body.what === 'ended') {
            activeStreams.delete(streamId);
        }
        console.log('active streams (' + activeStreams.size +'): '+ JSON.stringify(Array.from(activeStreams)))
    }
    res.sendStatus(200);
});

function listenForClients(port) {
    app.listen(port, function () {
       console.log('Listening for clients on port ' + port);
    });
}

listenForStreams(listenPort);
listenForClients(listenPort);
