/**
 * Copyright 2016 PhenixP2P Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0(the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phenixp2p.demo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.phenixp2p.pcast.FacingMode;
import com.phenixp2p.pcast.MediaStream;
import com.phenixp2p.pcast.PCast;
import com.phenixp2p.pcast.PCastInitializeOptions;
import com.phenixp2p.pcast.Publisher;
import com.phenixp2p.pcast.Renderer;
import com.phenixp2p.pcast.RendererStartStatus;
import com.phenixp2p.pcast.RequestStatus;
import com.phenixp2p.pcast.UserMediaOptions;
import com.phenixp2p.pcast.UserMediaStream;
import com.phenixp2p.pcast.android.AndroidPCastFactory;
import com.phenixp2p.pcast.android.AndroidVideoRenderSurface;

import org.json.JSONObject;

import static com.phenixp2p.demo.Utilities.hasInternet;

public class MainActivity extends AppCompatActivity {

  private final static String serverAddress = "https://demo.phenixp2p.com/demoApp/";
  private final static String TAG = "PCast";

  private PCast pcast;
  private String sessionId;
  private UserMediaStream media;
  private String streamId;
  private ProgressBar progress;
  private TextView status;
  private SurfaceView surfaceView;

  private static final int REQUEST_CAMERA = 0;
  private static final int NUM_STEPS = 8;
  private int steps = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    this.status = (TextView)this.findViewById(R.id.status);
    this.progress = (ProgressBar)this.findViewById(R.id.progress);
    this.surfaceView = (SurfaceView)this.findViewById(R.id.surfaceView);
    surfaceView.setZOrderOnTop(true); // to set the video background white
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);

    ActivityCompat.requestPermissions(MainActivity.this,
      new String[] {Manifest.permission.CAMERA},
      REQUEST_CAMERA);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                      @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CAMERA) {
      // Received permission result for camera permission.
      Log.i(TAG, "Received response for Camera permission request.");

      // Check if the only required permission has been granted.
      if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Camera permission has been granted, preview can be displayed.
        if (sessionId == null) {
          this.login();
        } else {
          Log.d(TAG, "sessionId is not null");
        }
      }
    }
    Log.d(TAG, "a: onRequestPermissionsResult: ");
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Stop pcast when we exit the app.
    if (pcast != null) {
      pcast.stop();
      pcast.shutdown();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (sessionId != null) {
      this.login();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    try {
      surfaceView.getHolder().getSurface().release();
    } catch (Throwable ignored) {}
    surfaceView = null;
  }

  private void reportStatus(final String step, final Boolean success) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        MainActivity.this.steps++;
        MainActivity.this.status.setText(step + ": " +(success ? "success" : "failed"));
        MainActivity.this.progress.setProgress(100 * MainActivity.this.steps / NUM_STEPS);
        Log.d(TAG, "run: " + 100 * MainActivity.this.steps / NUM_STEPS);
        if (step.equals("view") && progress.getProgress() >= 100) {
          status.setVisibility(View.GONE);
          progress.setVisibility(View.GONE);
        }
      }
    });
  }

  // 1. REST API: authenticate with the app-maker's own server. The app talks to a Phenix demo server, but you could also use the node.js server provided in this repo.
  private void login() {
    // Check the connection to the internet.
    if (hasInternet(this)) {
      try {
        JSONObject params = new JSONObject();
        params.put("name", "demo-user");
        params.put("password", "demo-password");
        new HTTPtask(serverAddress + "login", params, new HTTPtask.Caller() {
          public void callback(JSONObject result) {
            try {
              MainActivity.this.reportStatus("Login", true);
              String authenticationToken = result.getString("authenticationToken");
              MainActivity.this.start(authenticationToken);
            } catch (Exception e) {
              MainActivity.this.reportStatus("Login", false);
              e.printStackTrace();
            }
          }
        }).execute();
      } catch (Exception e) {
        this.reportStatus("Login", false);
        e.printStackTrace();
      }
    } else {
      new AlertDialog.Builder(this).setTitle("No internet")
        .setMessage("Please connect to the internet")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
            startActivity(intent);

            dialogInterface.dismiss();
          }
        }
      ).show();
    }
  }

  private interface Streamer {
    void hereIsYourStreamToken(String streamToken);
  }

  // Gets a stream token from the REST API.
  // Called for both publish and subscribe.
  private void stream(String sessionId, String originStreamId, final Streamer streamer) {
    try {
      JSONObject params = new JSONObject();
      params.put("sessionId", sessionId);
      if (originStreamId != null) {
        params.put("originStreamId", originStreamId);
      }
      new HTTPtask(serverAddress + "stream", params, new HTTPtask.Caller() {
        public void callback(JSONObject result) {
          try {
            String streamToken = result.getString("streamToken");
            streamer.hereIsYourStreamToken(streamToken);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }).execute();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // 2. PCast SDK API: start.
  private void start(String authenticationToken) {
    this.pcast = AndroidPCastFactory.createPCastAdmin(this);
    this.pcast.initialize(new PCastInitializeOptions(false, true));

    this.pcast.start(authenticationToken,
      new PCast.AuthenticationCallback() {
        public void onEvent(PCast var1, RequestStatus status, String sessionId) {
          if (status == RequestStatus.OK) {
            MainActivity.this.reportStatus("Start SDK", true);
            MainActivity.this.sessionId = sessionId;
            MainActivity.this.getUserMedia();
          } else {
            MainActivity.this.reportStatus("Start SDK", false);
          }
        }
      },
      new PCast.OnlineCallback() {
        public void onEvent(PCast var1) {
          Log.d(TAG, "online");
        }
      },
      new PCast.OfflineCallback() {
          public void onEvent(PCast var1) {
            Log.d(TAG, "offline");
          }
        }
    );
  }

  // 3. Get user media from SDK.
  private void getUserMedia() {
    UserMediaOptions gumOptions = new UserMediaOptions();
    gumOptions.getVideoOptions().setFacingMode(FacingMode.ENVIRONMENT);
    gumOptions.getAudioOptions().setEnabled(false);

    this.pcast.getUserMedia(gumOptions, new PCast.UserMediaCallback() {
      public void onEvent(PCast p, RequestStatus status, UserMediaStream media) {
        MainActivity.this.reportStatus("Get user media", true);
        MainActivity.this.media = media;
        MainActivity.this.getPublishToken();
      }
    });
  }

  // 4. Get publish token from REST admin API.
  private void getPublishToken() {
    this.stream(this.sessionId, null, new Streamer() {
      public void hereIsYourStreamToken(String streamToken) {
        MainActivity.this.reportStatus("Get publish token", true);
        MainActivity.this.publishStream(streamToken);
      }
    });
  }

  // 5. Publish stream with SDK.
  private void publishStream(String publishStreamToken) {
    this.pcast.publish(publishStreamToken, this.media.getMediaStream(), new PCast.PublishCallback() {
      public void onEvent(PCast p, RequestStatus status, Publisher publisher) {
        if (status == RequestStatus.OK) {
          MainActivity.this.reportStatus("Publish", true);
        MainActivity.this.streamId = publisher.getStreamId();
          MainActivity.this.getSubscribeToken();
        } else {
          MainActivity.this.reportStatus("Publish", false);
        }
      }
    });
  }

  // 6. Get stream token from REST admin API.
  private void getSubscribeToken() {
    this.stream(this.sessionId, this.streamId, new Streamer() {
      public void hereIsYourStreamToken(String streamToken) {
        MainActivity.this.reportStatus("Get subscribe token", true);
        MainActivity.this.subscribeStream(streamToken);
      }
    });
  }

  // 7. Subscribe to stream with SDK.
  private void subscribeStream(String subscribeStreamToken) {
    this.reportStatus("Subscribe", true);
    this.pcast.subscribe(subscribeStreamToken, new PCast.SubscribeCallback() {
      public void onEvent(PCast p, RequestStatus status, MediaStream media) {
        if (status == RequestStatus.OK) {
          MainActivity.this.reportStatus("Subscribe", true);
          MainActivity.this.viewStream(media.createRenderer());
        } else {
          MainActivity.this.reportStatus("Subscribe", false);
        }
      }
    });
  }

  // 8. View stream.
  private void viewStream(Renderer renderer) {
    SurfaceHolder surface = surfaceView.getHolder();
    if (renderer.start(new AndroidVideoRenderSurface(surface)) != RendererStartStatus.OK) {
      this.reportStatus("View", false);
    } else {
      this.reportStatus("View", true);
    }
  }
}
