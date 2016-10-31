/**
 * Copyright 2016 PhenixP2P Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0(the "License");
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
package com.phenixp2p.demo.ui.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.phenixp2p.demo.HTTPtask;
import com.phenixp2p.demo.R;
import com.phenixp2p.demo.presenters.MainPresenter;
import com.phenixp2p.demo.presenters.inter.IMainPresenter;
import com.phenixp2p.demo.ui.view.IMainView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.phenixp2p.demo.utils.Utilities.hasInternet;

public class MainActivity extends AppCompatActivity implements IMainView{

  private final static String serverAddress = "https://demo.phenixp2p.com/demoApp/";
  private final static String TAG = "PCast";

  private PCast pcast;
  private String sessionId;
  private UserMediaStream media;
  private String streamId;
  private ProgressBar progress;
  private TextView status;
  private SurfaceView surfaceView;

  private static final int NUM_STEPS = 8;
  private int steps = 0;

  private IMainPresenter presenter;
  private View decorView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    decorView = getWindow().getDecorView();
    showSystemUI();
    setContentView(R.layout.activity_main);
    // Hide UI first
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
    this.status = (TextView) this.findViewById(R.id.status);
    this.progress = (ProgressBar) this.findViewById(R.id.progress);
    this.surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);
    surfaceView.setZOrderOnTop(true); // to set the video background white
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
    presenter = new MainPresenter(this);
    this.checkPermissions();
  }

  final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

  private void checkPermissions() {
    List<String> permissionsNeeded = new ArrayList<String>();

    final List<String> permissionsList = new ArrayList<String>();
    if (!addPermission(permissionsList, Manifest.permission.CAMERA)) {
      permissionsNeeded.add("access the camera");
    }
    if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO)) {
      permissionsNeeded.add("access the microphone");
    }

    if (permissionsList.size() > 0) {
      if (permissionsNeeded.size() > 0) {
        String message = "You need to grant access to " + permissionsNeeded.get(0);
        for (int i = 1; i < permissionsNeeded.size(); i++)
          message = message + ", " + permissionsNeeded.get(i);
          showMessageOKCancel(message,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(MainActivity.this, permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
              }
            }
          );
        return;
      }

      ActivityCompat.requestPermissions(MainActivity.this, permissionsList.toArray(new String[permissionsList.size()]),
              REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
      return;
    }

    this.commenceSession();
  }

  private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
    new AlertDialog.Builder(MainActivity.this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
      {
        Map<String, Integer> permissionCodes = new HashMap<String, Integer>();
        // Initial
        permissionCodes.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_DENIED);
        permissionCodes.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_DENIED);
        // Fill with results
        for (int i = 0; i < permissions.length; i++)
          permissionCodes.put(permissions[i], grantResults[i]);
        // Check for ACCESS_FINE_LOCATION
        if (permissionCodes.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && permissionCodes.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
          // All Permissions Granted
          this.commenceSession();
        } else {
          // Permission Denied
          Toast.makeText(MainActivity.this, "Some permissions were denied", Toast.LENGTH_SHORT).show();
        }
      }
      break;
      default:
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private boolean addPermission(List<String> permissionsList, String permission) {
    if (ActivityCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
      permissionsList.add(permission);
      // Check for Rationale Option
      if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission))
        return false;
    }
    return true;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
  super.onConfigurationChanged(newConfig);
    // Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      hideSystemUI();
      surfaceView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
          showSystemUI();
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              hideSystemUI();
            }
          }, 3000);
        }
      });
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
      showSystemUI();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Stop pcast when we exit the app.
    presenter.stopRendering();
    if (pcast != null) {
      pcast.stop();
      pcast.shutdown();
      pcast = null;
      steps = 0;
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

  // 0. Commence sequence
  private void commenceSession() {
    if (this.sessionId == null) {
      this.login();
    }
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
    gumOptions.getAudioOptions().setEnabled(true);

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
          presenter.startRendering(media.createRenderer());
        } else {
          MainActivity.this.reportStatus("Subscribe", false);
        }
      }
    });
  }

  // 8. View stream.
  @Override
  public void viewStream(Renderer renderer) {
    SurfaceHolder surface = surfaceView.getHolder();
    if (renderer.start(new AndroidVideoRenderSurface(surface)) != RendererStartStatus.OK) {
      this.reportStatus("View", false);
    } else {
      this.reportStatus("View", true);
    }
  }

  // This snippet hides the system bars.
  private void hideSystemUI() {
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
    WindowManager.LayoutParams.FLAG_FULLSCREEN);
    decorView.setSystemUiVisibility(
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE);
  }

  // This snippet shows the system bars. It does this by removing all the flags
  // except for the ones that make the content appear under the system bars.
  private void showSystemUI() {
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    decorView.setSystemUiVisibility(
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
  }
}
