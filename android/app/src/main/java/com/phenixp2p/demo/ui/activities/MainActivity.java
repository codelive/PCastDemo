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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.phenixp2p.demo.Capabilities;
import com.phenixp2p.demo.CaptureHelper;
import com.phenixp2p.demo.Constants;
import com.phenixp2p.demo.PhenixApplication;
import com.phenixp2p.demo.PhenixService;
import com.phenixp2p.demo.R;
import com.phenixp2p.demo.RxBus;
import com.phenixp2p.demo.events.Events;
import com.phenixp2p.demo.presenters.MainActivityPresenter;
import com.phenixp2p.demo.presenters.inter.IMainActivityPresenter;
import com.phenixp2p.demo.ui.fragments.BaseFragment;
import com.phenixp2p.demo.ui.fragments.MainFragment;
import com.phenixp2p.demo.ui.fragments.ViewDetailStreamFragment;
import com.phenixp2p.demo.ui.view.IMainActivityView;
import com.phenixp2p.demo.utils.DialogUtils;
import com.phenixp2p.demo.utils.TokenUtils;
import com.phenixp2p.pcast.DataQualityReason;
import com.phenixp2p.pcast.DataQualityStatus;
import com.phenixp2p.pcast.FacingMode;
import com.phenixp2p.pcast.FlashMode;
import com.phenixp2p.pcast.MediaType;
import com.phenixp2p.pcast.PCast;
import com.phenixp2p.pcast.PCastInitializeOptions;
import com.phenixp2p.pcast.Publisher;
import com.phenixp2p.pcast.RequestStatus;
import com.phenixp2p.pcast.SourceDeviceInfo;
import com.phenixp2p.pcast.SourceDeviceType;
import com.phenixp2p.pcast.UserMediaOptions;
import com.phenixp2p.pcast.UserMediaStream;
import com.phenixp2p.pcast.android.AndroidPCastFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static com.phenixp2p.demo.Constants.CREATE_SCREEN_CAPTURE;
import static com.phenixp2p.demo.Constants.ENDPOINT;
import static com.phenixp2p.demo.Constants.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS;
import static com.phenixp2p.demo.Constants.REQUEST_CODE_SECRET_URL;
import static com.phenixp2p.demo.Constants.SESSION_ID;
import static com.phenixp2p.demo.Constants.STREAM_ID;
import static com.phenixp2p.demo.Constants.STREAM_TOKEN;
import static com.phenixp2p.demo.utils.TokenUtils.clearAll;
import static com.phenixp2p.demo.utils.Utilities.handleException;
import static com.phenixp2p.demo.utils.Utilities.hasInternet;

public final class MainActivity extends AppCompatActivity implements
        IMainActivityView,
        Publisher.DataQualityChangedCallback {

  private final static String TAG = "PCast";
  private PCast pcast;
  private UserMediaStream publishMedia;
  private PowerManager.WakeLock wakeLock;
  private String sessionId;
  private String streamId;
  private PulsatorLayout pulsator;
  private Publisher publisher;
  private ProgressBar progressBar;
  private boolean isStarted = false;
  private IMainActivityPresenter presenter;
  private DataQualityReason dataQualityReason;
  private DataQualityStatus dataQualityStatus;
  private String screenCaptureDeviceId;
  private boolean isShare = false;

  private CompositeSubscription subscriptions;
  private boolean isResume = false;
  private boolean isEnableBackPress = false;
  private UserMediaOptions gumOptions;
  private boolean isError = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    this.pulsator = (PulsatorLayout) findViewById(R.id.pulsator);
    this.progressBar = (ProgressBar) findViewById(R.id.prog);
    this.presenter = new MainActivityPresenter(this);
    if (!PhenixService.isReady()) {
      this.checkPermissions();
      this.pulsator.setVisibility(View.VISIBLE);
    } else {
      this.isStarted = true;
      this.pulsator.setVisibility(View.GONE);
      onGetMediaShareScreen(false);
      Bundle bundle = new Bundle();
      bundle.putString(SESSION_ID, TokenUtils.getSessionIdLocal(this));
      bundle.putString(STREAM_ID, TokenUtils.getStreamIdLocal(this));
      BaseFragment.openFragment(MainActivity.this,
              getSupportFragmentManager(),
              MainFragment.class,
              null,
              bundle,
              R.id.content,
              null);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case CREATE_SCREEN_CAPTURE:
        if (resultCode != RESULT_OK) {
          getUserMedia();
          ((PhenixApplication) getApplication()).setProjectionManager(null);
          return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          if (!CaptureHelper.handleActivityResult(this, requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
          }
        }
        break;
      case REQUEST_CODE_SECRET_URL:
        this.checkPermissions();
        break;
    }
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
            ActivityCompat.requestPermissions(MainActivity.this,
                    permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
          }
        });
        return;
      }
      ActivityCompat.requestPermissions(MainActivity.this,
              permissionsList.toArray(new String[permissionsList.size()]),
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
      .setCancelable(false)
      .create()
      .show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    switch (requestCode) {
      case Constants.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
        Map<String, Integer> permissionCodes = new ArrayMap<>();
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
    final PhenixApplication appContext = (PhenixApplication) getApplicationContext();
    if (!this.isShare) {
      appContext.setStopPublish(false);
      appContext.setBackground(false);
      onClear();
      if (this.presenter != null) {
        this.presenter.onDestroy();
      }
      this.progressBar.setVisibility(View.GONE);
      System.gc();
      if (this.subscriptions != null) {
        this.subscriptions.clear();
        this.subscriptions.unsubscribe();
        this.subscriptions = null;
      }
    }
    if (this.wakeLock != null && this.wakeLock.isHeld()) {
      this.wakeLock.release();
    }
    super.onPause();
  }

  private void onClear() {
    // Stop pcast when we exit the app.
    if (this.publisher != null) {
      MainActivity.this.publisher.stop("exit-app");
      if (!MainActivity.this.publisher.isClosed()) {
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

    if (this.pcast != null) {
      this.pcast.stop();
      this.pcast.shutdown();
      if (!this.pcast.isClosed()) {
        try {
          this.pcast.close();
        } catch (IOException e) {
          handleException(this, e);
        }
      }
      ((PhenixApplication) getApplicationContext()).setPCast(null);
      this.pcast = null;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!this.isShare) {
      PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
      this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
      this.wakeLock.acquire();
      if (this.sessionId != null) {
        this.presenter = new MainActivityPresenter(this);
        this.login();
      }
      addSubscription(subscribeEvents());
    }
    this.isResume = true;
    if (this.isEnableBackPress) {
      onBackPressed();
    } else {
      if (this.isShare && ((PhenixApplication) getApplicationContext()).isBackground()) {
        Fragment fragmentMain = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
        if (fragmentMain != null && fragmentMain.isVisible()) {
          ((MainFragment) fragmentMain).callReload(this.sessionId, MainActivity.this.streamId);
        }
      }
      this.isEnableBackPress = false;
    }
  }

  @Override
  public void onBackPressed() {
    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
      ((PhenixApplication) getApplicationContext()).setStopPublish(false);
      ((PhenixApplication) getApplicationContext()).setLandscape(false);
    }
    super.onBackPressed();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK && this.isShare) {
      ((PhenixApplication) getApplicationContext()).setBackground(false);
    } else {
      ((PhenixApplication) getApplicationContext()).setBackground(true);
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    this.isResume = false;
    super.onSaveInstanceState(outState);
  }

  public boolean isSaveInstance() {
    return !this.isResume;
  }

  public void setEnableBackPress(boolean enableBackPress) {
    this.isEnableBackPress = enableBackPress;
  }

  // 0. Commence sequence
  private void commenceSession() {
    if (this.sessionId == null) {
      this.login();
    }
  }

  // 1. REST API: authenticate with the app-maker's own server.
  // The app talks to a Phenix demo server, but you could also use the node.js server provided in this repo.
  private void login() {
    // Check the connection to the internet.
    if (hasInternet(this)) {
      this.presenter.login("demo-user", "demo-password", ENDPOINT);
    } else {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(MainActivity.this).setTitle("No internet")
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
      });
    }
  }

  // Get authentication token when REST APIs
  @Override
  public void authenticationToken(String authenticationToken) {
    start(authenticationToken);
  }

  //error when login
  @Override
  public void onError(final String error) {
    onClear();
    this.isError = true;
    clearAll(this);
    Log.d(TAG, "onError: ");
    this.presenter.onDestroy();
    checkPermissions();
  }

  @Override
  public void showProgress() {
    if (this.pulsator.getVisibility() == View.GONE) {
      this.pulsator.setVisibility(View.VISIBLE);
    }
    this.pulsator.start();
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
            ((PhenixApplication) getApplicationContext()).setPCast(MainActivity.this.pcast);
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
    gumOptions.getVideoOptions().setFacingMode(FacingMode.ENVIRONMENT);
    gumOptions.getVideoOptions().setFlashMode(FlashMode.AUTOMATIC);
    onUserMedia(gumOptions);
  }

  private void onUserMedia(UserMediaOptions gumOptions) {
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
    if (((PhenixApplication) getApplicationContext()).isShare()) {
      if (PhenixService.isReady()) {
        stopService(new Intent(MainActivity.this, PhenixService.class));
      }
    }
    onClear();
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        DialogUtils.showDialog(title, "Please try again", new DialogUtils.ActionDialog() {
          @Override
          public AppCompatActivity getContext() {
            return MainActivity.this;
          }

          @Override
          public void buttonYes() {
            login();
          }

          @Override
          public void autoDismiss(AlertDialog alertDialog) {}
        });
      }
    });
  }

  // 4. Get publish token from REST admin API.
  private void getPublishToken() {
    presenter.createStreamToken(ENDPOINT,
            getLocalSessiondId(),
            null,
            new String[]{Capabilities.ARCHIVE.getValue(), Capabilities.STREAMING.getValue()},
            new MainActivityPresenter.IStreamer() {

        @Override
        public void hereIsYourStreamToken(String streamToken) {
          if (streamToken != null) {
            publishStream(streamToken);
          } else {
            MainActivity.this.getPublishToken();
          }
        }
      });
  }

  // 5. Publish streamToken with SDK.
  private void publishStream(String publishStreamToken) {
    if (pcast != null && this.publishMedia != null && this.publishMedia.getMediaStream() != null) {
      MainActivity.this.pcast.publish(publishStreamToken,
              this.publishMedia.getMediaStream(),
              new PCast.PublishCallback() {
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
            MainActivity.this.publisher.setDataQualityChangedCallback(MainActivity.this);
            getSubscribeToken();
          } else {
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                if (MainActivity.this.progressBar != null) {
                  MainActivity.this.progressBar.setVisibility(View.GONE);
                }
                onTryAfterError(getResources().getString(R.string.render_error, status.name()));
              }
            });
          }
        }
      });
    } else {
      onTryAfterError("Media is null");
    }
  }

  private String getLocalSessiondId() {
    return this.sessionId == null ? TokenUtils.getSessionIdLocal(this) : this.sessionId;
  }

  // 6. Get streamToken token from REST admin API.
  private void getSubscribeToken() {
    presenter.createStreamToken(ENDPOINT,
            getLocalSessiondId(),
            this.streamId,
            null,
            new MainActivityPresenter.IStreamer() {
        @Override
        public void hereIsYourStreamToken(final String streamToken) {
          MainActivity.this.progressBar.setVisibility(View.GONE);
          if (!MainActivity.this.isStarted) {
            MainActivity.this.isStarted = true;
            Bundle bundle = new Bundle();
            bundle.putString(SESSION_ID, MainActivity.this.sessionId);
            bundle.putString(STREAM_TOKEN, streamToken);
            bundle.putString(STREAM_ID, MainActivity.this.streamId);
            BaseFragment.openFragment(MainActivity.this,
                    getSupportFragmentManager(),
                    MainFragment.class,
                    null,
                    bundle,
                    R.id.content,
                    null);
          } else {
            Fragment fragmentViewDetail = getSupportFragmentManager().findFragmentByTag(ViewDetailStreamFragment.class.getName());
            if (fragmentViewDetail != null && fragmentViewDetail.isVisible()) {
              ((ViewDetailStreamFragment) fragmentViewDetail).callReload(MainActivity.this.streamId);
            }
            Fragment fragmentMain = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
            if (fragmentMain != null && fragmentMain.isVisible() && !isError) {
              ((MainFragment) fragmentMain).callReload(sessionId, MainActivity.this.streamId);
            }
          }
        }
      }
    );
  }

  private void onShareScreen() {
    if (this.pcast == null) {
      this.pcast = ((PhenixApplication) getApplicationContext()).getPCast();
    }
    pcast.enumerateSourceDevices(
      new PCast.EnumerateSourceDevicesCallback() {
        @Override
        public void onEvent(PCast pcast, SourceDeviceInfo[] devices) {
          for (SourceDeviceInfo info : devices) {
            if (info.deviceType == SourceDeviceType.SYSTEM_OUTPUT) {
              Log.i("Phenix SDK Example", "Screencasting is available");
              MainActivity.this.screenCaptureDeviceId = info.id;
            }
          }
        }
      },
      MediaType.VIDEO);
  }

  private void goneAnimation() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // animation view not support close
        MainActivity.this.pulsator.stop();
        MainActivity.this.pulsator.clearAnimation();
        MainActivity.this.pulsator.destroyDrawingCache();
        MainActivity.this.pulsator.setVisibility(View.GONE);
      }
    });
  }

  public PCast getPCast() {
    return this.pcast;
  }

  public UserMediaStream getPublishMedia() {
    return this.publishMedia;
  }

  public DataQualityReason getDataQualityReason() {
    return this.dataQualityReason;
  }

  public DataQualityStatus getDataQualityStatus() {
    return this.dataQualityStatus;
  }

  //Data Quality Status For Publishers
  @Override
  public void onEvent(Publisher publisher,
                      DataQualityStatus dataQualityStatus,
                      DataQualityReason dataQualityReason) {
    this.dataQualityReason = dataQualityReason;
    this.dataQualityStatus = dataQualityStatus;
    RxBus.getInstance().post(new Events.OnStateDataQuality(false, dataQualityStatus, dataQualityReason));
  }

  public void onStartShareScreen() {
    showProgressBar();
    this.isShare = true;
  }

  public void onlyVideoOrAudio(boolean isVideo) {
    showProgressBar();
    UserMediaOptions gumOptions = new UserMediaOptions();
    if (isVideo) {
      gumOptions.getAudioOptions().setEnabled(false);
      gumOptions.getVideoOptions().setEnabled(true);
      gumOptions.getVideoOptions().setFacingMode(FacingMode.ENVIRONMENT);
      gumOptions.getVideoOptions().setFlashMode(FlashMode.AUTOMATIC);
    } else {
      gumOptions.getAudioOptions().setEnabled(true);
      gumOptions.getVideoOptions().setEnabled(false);
    }
    onUserMedia(gumOptions);
  }

  @Override
  protected void onDestroy() {
    if (!this.isShare) {
      clearAll(this);
    }
    super.onDestroy();
  }

  protected Subscription subscribeEvents() {
    return RxBus.getInstance().toObservable()
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext(new Action1<Object>() {
        @Override
        public void call(Object objectEvent) {
          if (objectEvent instanceof Events.ChangeCamera) {
            if (MainActivity.this.gumOptions == null) {
              MainActivity.this.gumOptions = new UserMediaOptions();
            }
            MainActivity.this.gumOptions.getAudioOptions().setEnabled(true);
            MainActivity.this.gumOptions.getVideoOptions().setFlashMode(FlashMode.AUTOMATIC);
            if (((Events.ChangeCamera) objectEvent).isChange) {
              MainActivity.this.gumOptions.getVideoOptions().setFacingMode(FacingMode.USER);
              if (MainActivity.this.publishMedia != null) {
                MainActivity.this.publishMedia.applyOptions(MainActivity.this.gumOptions);
              }
              onChangeIconCamera(MainActivity.this.gumOptions);
            } else {
              MainActivity.this.gumOptions.getVideoOptions().setFacingMode(FacingMode.ENVIRONMENT);
              if (MainActivity.this.publishMedia != null) {
                MainActivity.this.publishMedia.applyOptions(MainActivity.this.gumOptions);
              }
              onChangeIconCamera(MainActivity.this.gumOptions);
            }
          }
          // stop streamToken with click preview
          if (objectEvent instanceof Events.OnStopStream) {
            if (MainActivity.this.publisher != null) {
              if (!MainActivity.this.publisher.isClosed()) {
                MainActivity.this.publisher.stop("close");
                try {
                  MainActivity.this.publisher.close();
                } catch (IOException e) {
                  handleException(MainActivity.this, e);
                }
              }
              MainActivity.this.publisher = null;
            }

            if (MainActivity.this.publishMedia != null) {
              if (!MainActivity.this.publishMedia.isClosed()) {
                try {
                  MainActivity.this.publishMedia.close();
                } catch (IOException e) {
                  handleException(MainActivity.this, e);
                }
              }
              MainActivity.this.publishMedia = null;
            }

            if (MainActivity.this.isShare) {
              stopService(new Intent(MainActivity.this, PhenixService.class));
              MainActivity.this.screenCaptureDeviceId = null;
              MainActivity.this.isShare = false;
              ((PhenixApplication) getApplicationContext()).setShare(false);
            }
            MainActivity.this.presenter.onDestroy();
          }
          //restart streamToken with click preview is stop
          if (objectEvent instanceof Events.OnRestartStream) {
            showProgressBar();
            getUserMedia();
          }

          if (objectEvent instanceof Events.OnShareScreen) {
            if (((Events.OnShareScreen) objectEvent).isStart) {
              onGetMediaShareScreen(true);
              ((PhenixApplication) getApplicationContext()).setShare(true);
            }
          }
        }
      }).subscribe(RxBus.defaultSubscriber());
  }

  private void onChangeIconCamera(UserMediaOptions gumOptions) {
    Fragment fragmentMain = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
    if (fragmentMain != null && fragmentMain.isVisible()) {
      ((MainFragment) fragmentMain).onChangeIconCamera(gumOptions.getVideoOptions().getFacingMode());
    }
  }

  private void onGetMediaShareScreen(final boolean isStarted) {
    onShareScreen();
    UserMediaOptions gumOptions = new UserMediaOptions();
    gumOptions.getVideoOptions().setDeviceId(this.screenCaptureDeviceId);
    if (this.pcast == null) {
      this.pcast = ((PhenixApplication) getApplicationContext()).getPCast();
    }
    this.pcast.getUserMedia(
      gumOptions,
      new PCast.UserMediaCallback() {
        @Override
        public void onEvent(
          PCast pcast,
          RequestStatus status,
          UserMediaStream userMediaStream) {
          // Check status and store 'userMediaStream'
          if (status == RequestStatus.OK) {
            if (userMediaStream != null) {
              MainActivity.this.publishMedia = userMediaStream;
              if (isStarted) {
                getPublishToken();
              }
            } else {
              onTryAfterError(getResources().getString(R.string.media_null));
            }
          } else {
            onTryAfterError(getResources().getString(R.string.render_error, status.name()));
          }
        }
      });
  }

  protected void addSubscription(Subscription subscription) {
    if (subscription == null) return;
    if (this.subscriptions == null) {
      this.subscriptions = new CompositeSubscription();
      this.subscriptions.add(subscription);
    }
  }

  public boolean isStopPublish() {
    return this.publisher == null && this.publishMedia == null;
  }

  private void showProgressBar() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        MainActivity.this.progressBar.setVisibility(View.VISIBLE);
      }
    });
  }
}
