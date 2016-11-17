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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.phenixp2p.demo.HTTPtask;
import com.phenixp2p.demo.R;
import com.phenixp2p.demo.animation.PulsatorLayout;
import com.phenixp2p.demo.presenters.MainPresenter;
import com.phenixp2p.demo.presenters.inter.IMainPresenter;
import com.phenixp2p.demo.ui.view.IMainView;
import com.phenixp2p.pcast.FacingMode;
import com.phenixp2p.pcast.MediaStream;
import com.phenixp2p.pcast.PCast;
import com.phenixp2p.pcast.PCastInitializeOptions;
import com.phenixp2p.pcast.Publisher;
import com.phenixp2p.pcast.Renderer;
import com.phenixp2p.pcast.RequestStatus;
import com.phenixp2p.pcast.UserMediaOptions;
import com.phenixp2p.pcast.UserMediaStream;
import com.phenixp2p.pcast.android.AndroidPCastFactory;
import com.phenixp2p.pcast.android.AndroidVideoRenderSurface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.phenixp2p.demo.utils.Utilities.hasInternet;

public class MainActivity extends AppCompatActivity implements IMainView{

  private final static String serverAddress = "https://demo.phenixp2p.com/demoApp/";
  private final static String TAG = "PCast";
  private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

  private PCast pcast;
  private String sessionId;
  private UserMediaStream media;
  private String streamId;
  private SurfaceView surfaceView;

  private IMainPresenter presenter;
  private View decorView;
  private SurfaceHolder surfaceHolder;
  private PulsatorLayout mPulsator;
  private Button btnStart;
  private float tranY = -1;
  private float y = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    decorView = getWindow().getDecorView();
    showSystemUI();
    setContentView(R.layout.activity_main);
    mPulsator = (PulsatorLayout) findViewById(R.id.pulsator);
    btnStart = (Button) findViewById(R.id.btnStart);
    this.surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);
    surfaceHolder = surfaceView.getHolder();
    presenter = new MainPresenter(this);
    this.checkPermissions();
  }


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
        for (int i = 1; i < permissionsNeeded.size(); i++) {
          message = message + ", " + permissionsNeeded.get(i);
        }
        showMessageOKCancel(message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              ActivityCompat.requestPermissions(MainActivity.this, permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            }
        });
        return;
      }

      ActivityCompat.requestPermissions(MainActivity.this, permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
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
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
        Map<String, Integer> permissionCodes = new HashMap<>();
        // Initial
        permissionCodes.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_DENIED);
        permissionCodes.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_DENIED);
        // Fill with results
        for (int i = 0; i < permissions.length; i++) {
          permissionCodes.put(permissions[i], grantResults[i]);
        }
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
      if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
        return false;
      }
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
    mPulsator.animate().cancel();
    btnStart.setVisibility(View.GONE);
    if (pcast != null) {
      pcast.stop();
      pcast.shutdown();
      pcast = null;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (sessionId != null) {
      mPulsator.setAlpha(1f);
      mPulsator.setVisibility(View.VISIBLE);
      if (tranY != -1) {
        mPulsator.setTranslationY(tranY);
        tranY = -1;
      }

      if (y != -1) {
        mPulsator.setY(y);
        y = -1;

      }
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

  // 0. Commence sequence
  private void commenceSession() {
    if (this.sessionId == null) {
      this.login();
    }
  }

  // 1. REST API: authenticate with the app-maker's own server. The app talks to a Phenix demo server, but you could also use the node.js server provided in this repo.
  private void login() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mPulsator.start();
      }
    });
    // Check the connection to the internet.
    if (hasInternet(this)) {
      try {
        JSONObject params = new JSONObject();
        params.put("name", "demo-user");
        params.put("password", "demo-password");
        new HTTPtask(params, new HTTPtask.Caller() {
          public void callback(JSONObject result) {
            try {
              String authenticationToken = result.getString("authenticationToken");
              MainActivity.this.start(authenticationToken);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }).execute(serverAddress + "login");
      } catch (Exception e) {
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
              }).show();
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
      // Uncomment "streaming" to enable HLS/Dash live streaming instead of real-time:
      params.put("capabilities", new JSONArray(new String[]{/*"streaming"*/}));
      if (originStreamId != null) {
        params.put("originStreamId", originStreamId);
      }
      new HTTPtask(params, new HTTPtask.Caller() {
        public void callback(JSONObject result) {
          try {
            String streamToken = result.getString("streamToken");
            streamer.hereIsYourStreamToken(streamToken);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }).execute(serverAddress + "stream");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // 2. PCast SDK API: start.
  private void start(String authenticationToken) {
    this.pcast = AndroidPCastFactory.createPCastAdmin(this);
    this.pcast.initialize(new PCastInitializeOptions(false, true));
    this.pcast.start(authenticationToken, new PCast.AuthenticationCallback() {
      public void onEvent(PCast var1, RequestStatus status, String sessionId) {
        if (status == RequestStatus.OK) {
          MainActivity.this.sessionId = sessionId;
          MainActivity.this.getUserMedia();
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
    });
  }

  // 3. Get user media from SDK.
  private void getUserMedia() {
    UserMediaOptions gumOptions = new UserMediaOptions();
    gumOptions.getVideoOptions().setFacingMode(FacingMode.ENVIRONMENT);
    gumOptions.getAudioOptions().setEnabled(true);

    this.pcast.getUserMedia(gumOptions, new PCast.UserMediaCallback() {
      public void onEvent(PCast p, RequestStatus status, UserMediaStream media) {
        MainActivity.this.media = media;
        MainActivity.this.getPublishToken();
      }
    });
  }

  // 4. Get publish token from REST admin API.
  private void getPublishToken() {
    this.stream(this.sessionId, null, new Streamer() {
      public void hereIsYourStreamToken(String streamToken) {
        MainActivity.this.publishStream(streamToken);
      }
    });
  }

  // 5. Publish stream with SDK.
  private void publishStream(String publishStreamToken) {
    this.pcast.publish(publishStreamToken, this.media.getMediaStream(), new PCast.PublishCallback() {
      public void onEvent(PCast p, RequestStatus status, Publisher publisher) {
        if (status == RequestStatus.OK) {
          MainActivity.this.streamId = publisher.getStreamId();
          MainActivity.this.getSubscribeToken();
        }
      }
    });
  }

  // 6. Get stream token from REST admin API.
  private void getSubscribeToken() {
    this.stream(this.sessionId, this.streamId, new Streamer() {
      public void hereIsYourStreamToken(final String streamToken) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            tranY = mPulsator.getTranslationY();
            y = mPulsator.getY();
            mPulsator.animate()
                .translationY(mPulsator.getHeight())
                .alpha(0.0f)
                .setDuration(600)
                .setListener(new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mPulsator.setVisibility(View.GONE);
                  }
                });

            btnStart.animate()
                .alpha(1f)
                .setDuration(700)
                .setListener(new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    btnStart.setVisibility(View.VISIBLE);
                  }
                });
          }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            MainActivity.this.subscribeStream(streamToken);
          }
        });

      }
    });
  }

  // 7. Subscribe to stream with SDK.
  private void subscribeStream(String subscribeStreamToken) {
    this.pcast.subscribe(subscribeStreamToken, new PCast.SubscribeCallback() {
      public void onEvent(PCast p, RequestStatus status, final MediaStream media) {
        if (status == RequestStatus.OK) {
          // Prepare the player with the streaming source.
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              presenter.startRendering(media.createRenderer());
            }
          });
        }
      }
    });
  }

  // 8. View stream.
  @Override
  public void viewStream(Renderer renderer) {
    btnStart.setVisibility(View.GONE);
    renderer.start(new AndroidVideoRenderSurface(surfaceHolder));
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
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  }
}
