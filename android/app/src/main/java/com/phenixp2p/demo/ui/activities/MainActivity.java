/*
 * Copyright (c) 2016. PhenixP2P Inc. All Rights Reserved.
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
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.phenixp2p.demo.R;
import com.phenixp2p.demo.RxBus;
import com.phenixp2p.demo.animation.PulsatorLayout;
import com.phenixp2p.demo.api.response.Authentication;
import com.phenixp2p.demo.events.Events;
import com.phenixp2p.demo.presenters.MainActivityPresenter;
import com.phenixp2p.demo.presenters.inter.IMainActivityPresenter;
import com.phenixp2p.demo.ui.fragments.BaseFragment;
import com.phenixp2p.demo.ui.fragments.MainFragment;
import com.phenixp2p.demo.ui.fragments.ViewDetailStreamFragment;
import com.phenixp2p.demo.ui.view.IMainActivityView;
import com.phenixp2p.demo.utils.Utilities;
import com.phenixp2p.pcast.FacingMode;
import com.phenixp2p.pcast.PCast;
import com.phenixp2p.pcast.PCastInitializeOptions;
import com.phenixp2p.pcast.Publisher;
import com.phenixp2p.pcast.RequestStatus;
import com.phenixp2p.pcast.UserMediaOptions;
import com.phenixp2p.pcast.UserMediaStream;
import com.phenixp2p.pcast.android.AndroidPCastFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static com.phenixp2p.demo.ui.fragments.MainFragment.MY_STREAM_ID;
import static com.phenixp2p.demo.ui.fragments.MainFragment.SESSION_ID;
import static com.phenixp2p.demo.ui.fragments.MainFragment.STREAM_TOKEN;
import static com.phenixp2p.demo.ui.fragments.ViewDetailStreamFragment.STREAMING;
import static com.phenixp2p.demo.utils.Utilities.handleException;
import static com.phenixp2p.demo.utils.Utilities.hasInternet;

public class MainActivity extends BaseActivity implements IMainActivityView {
  private PCast pcast;
  private UserMediaStream publishMedia;
  private PowerManager.WakeLock mWakeLock;
  private String sessionId;
  private String streamId;
  private PulsatorLayout mPulsator;
  private Publisher publisher;
  private boolean isStarted = false;
  private IMainActivityPresenter presenter;
  private Bundle bundle;
  private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
  private final static String TAG = "PCast";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mPulsator = (PulsatorLayout) findViewById(R.id.pulsator);
    presenter = new MainActivityPresenter(this);
    bundle = new Bundle();

    this.checkPermissions();
  }

  private void checkPermissions() {
    List<String> permissionsNeeded = new ArrayList<>();
    final List<String> permissionsList = new ArrayList<>();
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
    .setCancelable(false)
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
        // Check for CAMERA
        if (permissionCodes.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        && permissionCodes.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
          // All Permissions Granted
          this.commenceSession();
        } else {
          // Permission Denied
          Toast.makeText(MainActivity.this, getResources().getString(R.string.permissions_denied), Toast.LENGTH_SHORT).show();
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
  protected void onPause() {
    super.onPause();
    // Stop pcast when we exit the app.
    if (publisher != null) {
      MainActivity.this.publisher.stop("exit-app");
      if (! MainActivity.this.publisher.isClosed()) {
        try {
          MainActivity.this.publisher.close();
        } catch (IOException e) {
          handleException(this, e);
        }
        MainActivity.this.publisher = null;
      }

    }
    if (MainActivity.this.publishMedia != null) {
      //not call close(), because when reopen will error
      MainActivity.this.publishMedia = null;
    }

    if (pcast != null) {
      pcast.stop();
      pcast.shutdown();
      if (!pcast.isClosed()) {
        try {
          pcast.close();
        } catch (IOException e) {
          handleException(this, e);
        }
      }
      pcast = null;
    }
    if (presenter != null) {
      presenter.onDestroy();
    }
    if (mWakeLock != null) {
      this.mWakeLock.release();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    mWakeLock.acquire();
    if (sessionId != null) {
      presenter = new MainActivityPresenter(this);
      this.login();
    }
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
      presenter.login("demo-user", "demo-password");
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

  // Get authentication token when REST APIs
  @Override
  public void authenticationToken(Authentication authenticationToken) {
    start(authenticationToken.getAuthenticationToken());
  }

  //error when login
  @Override
  public void onError(final String error) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
      }
    });
    login();
  }

  @Override
  public void showProgress() {
    if (mPulsator.getVisibility() == View.GONE) {
      mPulsator.setVisibility(View.VISIBLE);
    }
    mPulsator.start();
  }

  @Override
  public void hideProgress() {
    goneAnimation();
  }

  // 2. PCast SDK API: start.
  private void start(final String authenticationToken) {
    MainActivity.this.pcast = AndroidPCastFactory.createPCast(MainActivity.this);
    MainActivity.this.pcast.initialize(new PCastInitializeOptions(false, true));

    MainActivity.this.pcast.start(authenticationToken, new PCast.AuthenticationCallback() {
      public void onEvent(PCast var1, RequestStatus status, final String sessionId) {
        if (status == RequestStatus.OK) {
          MainActivity.this.sessionId = sessionId;
          getUserMedia();
        } else {
          onTryAfterError(getResources().getString(R.string.render_error, status.name()));
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

  // 3. Get user publishMedia from SDK.
  private void getUserMedia() {
    UserMediaOptions gumOptions = new UserMediaOptions();
    gumOptions.getAudioOptions().setEnabled(true);
    gumOptions.getVideoOptions().setFacingMode(FacingMode.USER);

    try {
      if (pcast != null) {
        MainActivity.this.pcast.getUserMedia(gumOptions, new PCast.UserMediaCallback() {
          public void onEvent(PCast p, RequestStatus status, UserMediaStream media) {
            if (status == RequestStatus.OK) {
              if (media != null) {
                MainActivity.this.publishMedia = media;
                getPublishToken();
              } else {
                onTryAfterError(getResources().getString(R.string.media_null));
              }
            } else {
              onTryAfterError(getResources().getString(R.string.render_error, status.name()));
            }
          }
        });
      }

    } catch (Exception e) {
      handleException(this, e);
    }
  }

  private void onTryAfterError(final String title) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Utilities.showDialog(title, "Please try again", new Utilities.ActionDialog() {
          @Override
          public Activity getActivity() {
            return MainActivity.this;
          }

          @Override
          public void buttonYes() {
            login();
          }
        });
      }
    });

  }

  // 4. Get publish token from REST admin API.
  private void getPublishToken() {
    presenter.createStreamToken(sessionId, null, STREAMING, new MainActivityPresenter.Streamer() {

      @Override
      public void hereIsYourStreamToken(String streamToken) {
        publishStream(streamToken);
      }
    });
  }

  // 5. Publish streamToken with SDK.
  private void publishStream(String publishStreamToken) {
    MainActivity.this.pcast.publish(publishStreamToken, publishMedia.getMediaStream(), new PCast.PublishCallback() {
      public void onEvent(PCast p, final RequestStatus status, Publisher publisher) {
        if (status == RequestStatus.OK) {
          if (publisher.hasEnded() && !publisher.isClosed()) {
            publisher.stop("close");
            try {
              publisher.close();
            } catch (IOException e) {
              handleException(MainActivity.this, e);
            }
          }
          MainActivity.this.publisher = publisher;
          MainActivity.this.streamId = publisher.getStreamId();
          getSubscribeToken();
        } else {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              onTryAfterError(getResources().getString(R.string.render_error, status.name()));
            }
          });
        }
      }
    });
  }

  // 6. Get streamToken token from REST admin API.
  private void getSubscribeToken() {
    presenter.createStreamToken(sessionId, streamId, null, new MainActivityPresenter.Streamer() {
      @Override
      public void hereIsYourStreamToken(final String streamToken) {
        if (!isStarted) {
          isStarted = true;
          bundle.putString(SESSION_ID, sessionId);
          bundle.putString(STREAM_TOKEN, streamToken);
          bundle.putString(MY_STREAM_ID, streamId);
          BaseFragment.openFragment(MainActivity.this, getSupportFragmentManager(), MainFragment.class, BaseFragment.AnimStyle.FROM_RIGHT, bundle, R.id.content_content, null);
        } else {
          Fragment fragmentViewDetail = getSupportFragmentManager().findFragmentByTag(ViewDetailStreamFragment.class.getName());
          if (fragmentViewDetail != null && fragmentViewDetail.isVisible()) {
            ((ViewDetailStreamFragment) fragmentViewDetail).callReload(sessionId, streamId);
          }
          Fragment fragmentMain = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
          if (fragmentMain != null && fragmentMain.isVisible()) {
            ((MainFragment) fragmentMain).callReload(sessionId, MainActivity.this.streamId);

          } else {
            if (fragmentMain != null) {
              ((MainFragment) fragmentMain).updateNewInfo(sessionId, MainActivity.this.streamId);
            }

          }
        }
      }
    });
  }

  //Subscribe events
  @Override
  protected Subscription subscribeEvents() {
    autoUnsubBus();
    return RxBus.getInstance().toObservable()
    .observeOn(AndroidSchedulers.mainThread())
    .doOnNext(new Action1<Object>() {
      @Override
      public void call(Object eventObject) {
        if (eventObject instanceof Events.ChangeCamera) {
          if (((Events.ChangeCamera) eventObject).isChange) {
            UserMediaOptions gumOptions = new UserMediaOptions();
            gumOptions.getVideoOptions().setFacingMode(FacingMode.USER);
            gumOptions.getAudioOptions().setEnabled(true);
            if (MainActivity.this.publishMedia != null) {
              MainActivity.this.publishMedia.applyOptions(gumOptions);
            }
            RxBus.getInstance().post(new Events.GetFacingMode(gumOptions.getVideoOptions().getFacingMode()));
          } else {
            UserMediaOptions gumOptions = new UserMediaOptions();
            gumOptions.getVideoOptions().setFacingMode(FacingMode.ENVIRONMENT);
            gumOptions.getAudioOptions().setEnabled(true);
            if (MainActivity.this.publishMedia != null) {
              MainActivity.this.publishMedia.applyOptions(gumOptions);
            }
            RxBus.getInstance().post(new Events.GetFacingMode(gumOptions.getVideoOptions().getFacingMode()));
          }
        }
        // stop streamToken with click preview
        if (eventObject instanceof Events.OnStopStream) {
          Fragment fragmentMain = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
          if (fragmentMain != null && fragmentMain.isVisible()) {
            ((MainFragment) fragmentMain).onStopPreview();
          }
          if (MainActivity.this.publishMedia != null && ! MainActivity.this.publishMedia.isClosed()) {
            try {
              MainActivity.this.publishMedia.close();
            } catch (IOException e) {
              handleException(MainActivity.this, e);
            }
            MainActivity.this.publishMedia = null;
          }

          if (MainActivity.this.publisher != null) {
            MainActivity.this.publisher.stop("ended");
            if (! MainActivity.this.publisher.isClosed()) {
              try {
                MainActivity.this.publisher.close();
              } catch (IOException e) {
                handleException(MainActivity.this, e);
              }
            }
            MainActivity.this.publisher = null;
          }
          presenter.onDestroy();
        }
        //restart streamToken with click preview is stop
        if (eventObject instanceof Events.OnRestartStream) {
          showProgress();
          getUserMedia();
        }
      }
    }).subscribe(RxBus.defaultSubscriber());
  }

  private void goneAnimation() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // animation view not support close
        mPulsator.stop();
        mPulsator.clearAnimation();
        mPulsator.setVisibility(View.GONE);
      }
    });
  }

  public PCast getPCast() {
    return pcast;
  }

  public UserMediaStream getPublishMedia() {
    return publishMedia;
  }
}
