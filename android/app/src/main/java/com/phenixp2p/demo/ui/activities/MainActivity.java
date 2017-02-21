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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.phenixp2p.demo.BuildConfig;
import com.phenixp2p.demo.Capabilities;
import com.phenixp2p.demo.CaptureHelper;
import com.phenixp2p.demo.Constants;
import com.phenixp2p.demo.PhenixApplication;
import com.phenixp2p.demo.PhenixService;
import com.phenixp2p.demo.R;
import com.phenixp2p.demo.RxBus;
import com.phenixp2p.demo.UriMenu;
import com.phenixp2p.demo.events.Events;
import com.phenixp2p.demo.presenters.MainPresenter;
import com.phenixp2p.demo.presenters.inter.IMainPresenter;
import com.phenixp2p.demo.ui.fragments.BaseFragment;
import com.phenixp2p.demo.ui.fragments.MainFragment;
import com.phenixp2p.demo.ui.view.IMainActivityView;
import com.phenixp2p.demo.utils.DialogUtil;
import com.phenixp2p.demo.utils.TokenUtil;
import com.phenixp2p.demo.utils.Utilities;
import com.phenixp2p.environment.android.AndroidContext;
import com.phenixp2p.pcast.DataQualityReason;
import com.phenixp2p.pcast.DataQualityStatus;
import com.phenixp2p.pcast.FacingMode;
import com.phenixp2p.pcast.FlashMode;
import com.phenixp2p.pcast.MediaStream;
import com.phenixp2p.pcast.MediaType;
import com.phenixp2p.pcast.PCast;
import com.phenixp2p.pcast.PCastFactory;
import com.phenixp2p.pcast.PCastInitializeOptions;
import com.phenixp2p.pcast.Publisher;
import com.phenixp2p.pcast.RequestStatus;
import com.phenixp2p.pcast.SourceDeviceInfo;
import com.phenixp2p.pcast.SourceDeviceType;
import com.phenixp2p.pcast.UserMediaOptions;
import com.phenixp2p.pcast.UserMediaStream;
import com.phenixp2p.pcast.android.AndroidPCastFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static com.phenixp2p.demo.Constants.APP_TAG;
import static com.phenixp2p.demo.Constants.CREATE_SCREEN_CAPTURE;
import static com.phenixp2p.demo.Constants.NUM_HTTP_RETRIES;
import static com.phenixp2p.demo.Constants.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS;
import static com.phenixp2p.demo.Constants.REQUEST_CODE_SECRET_URL;
import static com.phenixp2p.demo.Constants.SESSION_ID;
import static com.phenixp2p.demo.Constants.STREAM_ID;
import static com.phenixp2p.demo.Constants.STREAM_TOKEN;
import static com.phenixp2p.demo.Constants.TIME_TO_TAP;
import static com.phenixp2p.demo.Constants.NUMBER_TOUCHES;
import static com.phenixp2p.demo.utils.Utilities.handleException;
import static com.phenixp2p.demo.utils.Utilities.hasInternet;

public final class MainActivity extends AppCompatActivity implements IMainActivityView,
        Publisher.DataQualityChangedCallback,
        View.OnTouchListener {

  private static final String TAG = MainActivity.class.getSimpleName();

  private PCast pcast;
  private UserMediaStream publishMedia;
  private PowerManager.WakeLock wakeLock;
  private String sessionId;
  private String streamId;
  private PulsatorLayout pulsator;
  private Publisher publisher;
  private ProgressBar progressBar;
  private TextView textViewVersion;
  private boolean isStarted = false;
  private IMainPresenter presenter;
  private DataQualityReason dataQualityReason;
  private DataQualityStatus dataQualityStatus;
  private String screenCaptureDeviceId;
  private boolean isShare = false;

  private CompositeSubscription subscriptions;
  private boolean isEnableBackPress = false;
  private UserMediaOptions gumOptions;
  private boolean isError = false;
  private int onError = 0;
  private long startMillis = 0;
  private long count = 0;
  private boolean isVideo = false;
  private boolean isOnlyVideoOrAudio = false;
  private PhenixApplication phenixApplication;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    this.phenixApplication = ((PhenixApplication) getApplication());
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    this.pulsator = (PulsatorLayout) findViewById(R.id.pulsator);
    this.progressBar = (ProgressBar) findViewById(R.id.prog);
    this.textViewVersion = (TextView) findViewById(R.id.textViewVersion);
    String version = BuildConfig.VERSION_NAME.concat(" (" + String.valueOf(BuildConfig.VERSION_CODE) + ")");
    this.textViewVersion.setText(getResources().getString(R.string.version, version));
    this.textViewVersion.setOnTouchListener(this);
    this.presenter = new MainPresenter(this);
    if (!PhenixService.isReady()) {
      this.checkPermissions();
      this.pulsator.setVisibility(View.VISIBLE);
    } else {
      this.isStarted = true;
      this.pulsator.setVisibility(View.GONE);
      this.onGetMediaShareScreen(false);
      Bundle bundle = new Bundle();
      bundle.putString(SESSION_ID, TokenUtil.getSessionIdLocal(this));
      bundle.putString(STREAM_ID, TokenUtil.getStreamIdLocal(this));
      BaseFragment.openFragment(this, getSupportFragmentManager(), MainFragment.class, null, bundle,
        R.id.content_content, null);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case CREATE_SCREEN_CAPTURE:
        if (resultCode != RESULT_OK) {
          this.getUserMedia();
          this.phenixApplication.setProjectionManager(null);
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
      ActivityCompat.requestPermissions(this,
              permissionsList.toArray(new String[permissionsList.size()]),
              REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
      return;
    }
    this.commenceSession();
  }

  private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
    new AlertDialog.Builder(this)
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
          Toast.makeText(this, getResources().getString(R.string.permissions_denied), Toast.LENGTH_SHORT).show();
        }
      }
      break;
      default:
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private boolean addPermission(List<String> permissionsList, String permission) {
    if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
      permissionsList.add(permission);
      // Check for Rationale Option
      if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void onPause() {
    if (!this.isShare) {
      this.phenixApplication.setStopPublish(false);
      this.phenixApplication.setBackground(false);
      this.onClear();
      if (this.presenter != null) {
        this.presenter.onDestroy();
      }
      this.progressBar.setVisibility(View.GONE);
      if (this.subscriptions != null) {
        this.subscriptions.clear();
        this.subscriptions.unsubscribe();
        this.subscriptions = null;
      }
      System.gc();
    }
    super.onPause();
  }

  public void onClear() {
    // Stop pcast when we exit the app.
    if (this.publisher != null) {
      this.publisher.stop("exit-app");
      if (!this.publisher.isClosed()) {
        Utilities.close(this, this.publisher);
        this.publisher = null;
      }
    }

    if (this.publishMedia != null) {
      //not call close(), because when reopen will error
      this.publishMedia = null;
    }

    if (this.pcast != null) {
      this.pcast.stop();
      this.pcast.shutdown();
      if (!pcast.isClosed()) {
        Utilities.close(this, this.pcast);
      }
      this.phenixApplication.setPCast(null);
      this.pcast = null;
    }
  }

  private void onClearSecretUrl() {
    if (this.phenixApplication != null) {
      this.phenixApplication.setPcastAddress(null);
      this.phenixApplication.setServerAddress(null);
      this.phenixApplication.setPositionUriMenu(0);
    }
  }

  @Override
  protected void onResume() {
    this.phenixApplication = ((PhenixApplication) getApplication());
    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    this.wakeLock.acquire();
    if (this.phenixApplication.isBackground()) {
      this.isShare = true;
    }
    if (!this.isShare) {
      if (this.sessionId != null) {
        this.presenter = new MainPresenter(this);
        this.login();
      }
      this.addSubscription(subscribeEvents());
    }
    if (this.isEnableBackPress) {
      onBackPressed();
    } else {
      if (this.isShare && this.phenixApplication.isBackground()) {
        Fragment fragmentMain = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
        if (fragmentMain != null && fragmentMain.isVisible()) {
          ((MainFragment) fragmentMain).callReload(this.sessionId, this.streamId);
        }
      }
      this.isEnableBackPress = false;
    }
    super.onResume();
  }

  @Override
  public void onBackPressed() {
    Fragment fragmentMain = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
    if (fragmentMain != null && fragmentMain.isVisible()) {
      ((MainFragment) fragmentMain).onBackButtonPressed();
    }
    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
      PhenixApplication context = this.phenixApplication;
      context.setStopPublish(false);
      context.setLandscape(false);
      this.onClearSecretUrl();
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    PhenixApplication context = this.phenixApplication;
    if (keyCode == KeyEvent.KEYCODE_BACK && !context.isShare()) {
      context.setBackground(false);
      this.isShare = false;
    } else {
      context.setBackground(true);
    }
    return super.onKeyDown(keyCode, event);
  }

  // 0. Commence sequence
  private void commenceSession() {
    if (this.sessionId == null) {
      this.login();
    }
  }

  // 1. REST API: authenticate with the app-maker's own server. The app talks to a Phenix demo server, but you could also use the node.js server provided in this repo.
  public void login() {
    // Check the connection to the internet.
    if (hasInternet(this)) {
      Log.d(APP_TAG, "1. REST API: authenticate");
      this.presenter.login("demo-user", "demo-password", this.phenixApplication.getServerAddress());
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
    this.start(authenticationToken);
  }

  //error when login
  @Override
  public void onError(final String error) {
    this.onError++;
    if (this.onError == NUM_HTTP_RETRIES) { // inform user that we can't connect
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          MainActivity.this.pulsator.stop();
          MainActivity.this.pulsator.setVisibility(View.GONE);
          MainActivity.this.isStarted = true;
          Bundle bundle = new Bundle();
          bundle.putBoolean(Constants.BUNDLE_ERROR, true);
          BaseFragment.openFragment(MainActivity.this,
                  getSupportFragmentManager(),
                  MainFragment.class,
                  null,
                  bundle,
                  R.id.content_content,
                  null);
        }
      });
      this.onError = 0;
      return;
    }
    this.onClear();
    this.isError = true;
    this.presenter.onDestroy();
  }

  @Override
  public void showProgress() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (MainActivity.this.pulsator.getVisibility() == View.GONE) {
          MainActivity.this.pulsator.setVisibility(View.VISIBLE);
        }
        MainActivity.this.pulsator.start();
      }
    });
  }

  @Override
  public void hideProgress() {
    this.goneAnimation();
  }

  // 2. PCast SDK API: start.
  private void start(final String authenticationToken) {
    String pcastUrl = this.phenixApplication.getPcastAddress();
    Log.d(APP_TAG, "2. PCast SDK API: start [" + (pcastUrl == null ? "" : pcastUrl) + "]");
    if (pcastUrl == null) {
      this.pcast = AndroidPCastFactory.createPCast(this);
    } else {
      AndroidContext.setContext(this);
      this.pcast = PCastFactory.createPCast(pcastUrl);
    }
    this.pcast.initialize(new PCastInitializeOptions(false, true));
    this.pcast.start(authenticationToken, new PCast.AuthenticationCallback() {
        public void onEvent(PCast var1, RequestStatus status, final String sessionId) {
          if (status == RequestStatus.OK) {
            MainActivity.this.phenixApplication.setPCast(MainActivity.this.pcast);
            MainActivity.this.sessionId = sessionId;
            if (MainActivity.this.isOnlyVideoOrAudio) {
              MainActivity.this.onlyVideoOrAudio(MainActivity.this.isVideo);
            } else {
              MainActivity.this.getUserMedia();
            }
          } else {
            MainActivity.this.onTryAfterError(getResources().getString(R.string.render_error, status.name()));
          }
        }
      },
      new PCast.OnlineCallback() {
        public void onEvent(PCast var1) {
          Log.d(APP_TAG, "SDK online");
        }
      },
      new PCast.OfflineCallback() {
        public void onEvent(PCast var1) {
          Log.d(APP_TAG, "SDK offline");
        }
      });
  }

  // 3. Get user publishMedia from SDK.
  private void getUserMedia() {
    Log.d(APP_TAG, "3. Get user publishMedia from SDK");
    UserMediaOptions gumOptions = new UserMediaOptions();
    gumOptions.getAudioOptions().setEnabled(true);
    gumOptions.getVideoOptions().setFacingMode(FacingMode.ENVIRONMENT);
    gumOptions.getVideoOptions().setFlashMode(FlashMode.AUTOMATIC);
    onUserMedia(gumOptions);
  }

  private void onUserMedia(UserMediaOptions gumOptions) {
    try {
      if (this.pcast != null) {
        this.pcast.getUserMedia(gumOptions, new PCast.UserMediaCallback() {
          public void onEvent(PCast p, RequestStatus status, UserMediaStream media) {
            if (status == RequestStatus.OK) {
              if (media != null) {
                MainActivity.this.publishMedia = media;
                MainActivity.this.getPublishToken();
              } else {
                MainActivity.this.onTryAfterError(getResources().getString(R.string.media_null));
              }
            } else {
              MainActivity.this.onTryAfterError(getResources().getString(R.string.render_error, status.name()));
            }
          }
        });
      }
    } catch (Exception e) {
      handleException(this, e);
    }
  }

  private void onTryAfterError(final String title) {
    if (this.phenixApplication.isShare()) {
      if (PhenixService.isReady()) {
        this.stopService(new Intent(this, PhenixService.class));
      }
    }
    this.onClear();
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        DialogUtil.showDialog(title, "Please try again", new DialogUtil.ActionDialog() {
          @Override
          public AppCompatActivity getContext() {
            return MainActivity.this;
          }

          @Override
          public void buttonYes() {
            MainActivity.this.login();
          }

          @Override
          public void autoDismiss(AlertDialog alertDialog) {}
        });
      }
    });
  }

  private String getSessionId() {
    return this.sessionId == null ? TokenUtil.getSessionIdLocal(this) : this.sessionId;
  }

  // 4. Get publish token from REST admin API.
  private void getPublishToken() {
    Log.d(APP_TAG, "4. Get publish token from REST admin API");
    presenter.createStreamToken(this.phenixApplication.getServerAddress(),
            this.getSessionId(),
            null,
            new String[]{Capabilities.ARCHIVE.getValue(), Capabilities.STREAMING.getValue()},
            new MainPresenter.IStreamer() {
        @Override
        public void hereIsYourStreamToken(String streamToken) {
          if (streamToken != null) {
            MainActivity.this.publishStream(streamToken);
          } else {
            MainActivity.this.getPublishToken();
          }
        }

        @Override
        public void isError(int count) {
          if (count == NUM_HTTP_RETRIES) {
            MainActivity.this.setGoneVersion();
          }
        }
      });
  }

  // 5. Publish streamToken with SDK.
  private void publishStream(String publishStreamToken) {
    if (this.pcast != null && this.publishMedia != null ) {
      Log.d(APP_TAG, "5. Publish streamToken with SDK");
      MediaStream mediaStream = this.publishMedia.getMediaStream();
      if (mediaStream == null) {
        return;
      }
      this.pcast.publish(publishStreamToken, mediaStream, new PCast.PublishCallback() {
        public void onEvent(PCast p, final RequestStatus status, Publisher publisher) {
          if (status == RequestStatus.OK) {
            didPublishStream(publisher);
          } else {
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                if (MainActivity.this.progressBar != null) {
                  MainActivity.this.progressBar.setVisibility(View.GONE);
                }
                MainActivity.this.onTryAfterError(getResources().getString(R.string.render_error, status.name()));
              }
            });
          }
        }
      });
    } else {
      this.onTryAfterError("Media is null");
    }
  }

  private void didPublishStream(Publisher publisher) {
    if (publisher.hasEnded() && !publisher.isClosed()) {
      publisher.stop("close");
      Utilities.close(this, publisher);
    }
    this.publisher = publisher;
    this.streamId = publisher.getStreamId();
    this.publisher.setDataQualityChangedCallback(this);
    this.getSubscribeToken();
  }

  // 6. Get streamToken token from REST admin API.
  private void getSubscribeToken() {
    Log.d(APP_TAG, "6. Get streamToken token from REST admin API");
    presenter.createStreamToken(this.phenixApplication.getServerAddress(),
            this.getSessionId(),
            this.streamId,
            null,
            new MainPresenter.IStreamer() {

        @Override
        public void hereIsYourStreamToken(final String streamToken) {
          MainActivity.this.progressBar.setVisibility(View.GONE);
          MainActivity.this.textViewVersion.setVisibility(View.GONE);
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
              R.id.content_content, null);
          } else {
            Fragment fragmentMain = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
            if (fragmentMain != null && fragmentMain.isVisible() && !isError) {
              ((MainFragment) fragmentMain).callReload(MainActivity.this.sessionId, MainActivity.this.streamId);
            }
          }
        }

        @Override
        public void isError(int count) {}
      });
  }

  private void onShareScreen() {
    if (this.pcast == null) {
      this.pcast = this.phenixApplication.getPCast();
    }
    this.pcast.enumerateSourceDevices(
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
    this.showProgressBar();
    this.isShare = true;
  }

  public void onlyVideoOrAudio(boolean isVideo) {
    this.isOnlyVideoOrAudio = true;
    this.showProgressBar();
    UserMediaOptions gumOptions = new UserMediaOptions();
    if (isVideo) {
      this.isVideo = true;
      gumOptions.getAudioOptions().setEnabled(false);
      gumOptions.getVideoOptions().setEnabled(true);
      gumOptions.getVideoOptions().setFacingMode(FacingMode.ENVIRONMENT);
      gumOptions.getVideoOptions().setFlashMode(FlashMode.AUTOMATIC);
    } else {
      this.isVideo = false;
      gumOptions.getAudioOptions().setEnabled(true);
      gumOptions.getVideoOptions().setEnabled(false);
    }
    this.onUserMedia(gumOptions);
  }

  @Override
  protected void onDestroy() {
    if (!this.isShare) {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      if (this.wakeLock != null && this.wakeLock.isHeld()) {
        this.wakeLock.release();
      }
    }
    this.isOnlyVideoOrAudio = false;
    this.isVideo = false;
    super.onDestroy();
  }

  protected Subscription subscribeEvents() {
    return RxBus.getInstance().toObservable()
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext(new Action1<Object>() {
        @Override
        public void call(Object objectEvent) {
          if (objectEvent instanceof Events.ChangeCamera) {
            MainActivity.this.changeCamera(objectEvent);
          }
          // stop streamToken with click preview
          if (objectEvent instanceof Events.OnStopStream) {
            MainActivity.this.onEventStopStream();
          }
          //restart streamToken with click preview is stop
          if (objectEvent instanceof Events.OnRestartStream) {
            MainActivity.this.showProgressBar();
            MainActivity.this.getUserMedia();
          }

          if (objectEvent instanceof Events.OnShareScreen) {
            if (((Events.OnShareScreen) objectEvent).isStart) {
              MainActivity.this.onGetMediaShareScreen(true);
              MainActivity.this.phenixApplication.setShare(true);
            }
          }
        }
      }).subscribe(RxBus.defaultSubscriber());
  }

  private void changeCamera(Object objectEvent) {
    if (this.gumOptions == null) {
      this.gumOptions = new UserMediaOptions();
    }
    this.gumOptions.getAudioOptions().setEnabled(true);
    this.gumOptions.getVideoOptions().setFlashMode(FlashMode.AUTOMATIC);
    if (((Events.ChangeCamera) objectEvent).isChange) {
      this.gumOptions.getVideoOptions().setFacingMode(FacingMode.USER);
      if (this.publishMedia != null) {
        this.publishMedia.applyOptions(gumOptions);
      }
      this.onChangeIconCamera(gumOptions);
    } else {
      this.gumOptions.getVideoOptions().setFacingMode(FacingMode.ENVIRONMENT);
      if (this.publishMedia != null) {
        this.publishMedia.applyOptions(gumOptions);
      }
      this.onChangeIconCamera(gumOptions);
    }
  }

  public void onEventStopStream() {
    if (this.publisher != null) {
      if (!this.publisher.isClosed()) {
        this.publisher.stop("close");
        Utilities.close(this, this.publisher);
      }
      this.publisher = null;
    }

    if (this.publishMedia != null) {
      if (!this.publishMedia.isClosed()) {
        Utilities.close(this, this.publishMedia);
      }
      this.publishMedia = null;
    }

    if (this.isShare) {
      this.stopService(new Intent(this, PhenixService.class));
      this.screenCaptureDeviceId = null;
      this.isShare = false;
      this.phenixApplication.setShare(false);
    }
    this.presenter.onDestroy();
  }

  private void onChangeIconCamera(UserMediaOptions gumOptions) {
    Fragment fragmentMain = getSupportFragmentManager().findFragmentByTag(MainFragment.class.getName());
    if (fragmentMain != null && fragmentMain.isVisible()) {
      ((MainFragment) fragmentMain).onChangeIconCamera(gumOptions.getVideoOptions().getFacingMode());
    }
  }

  private void onGetMediaShareScreen(final boolean isShare) {
    this.onShareScreen();
    UserMediaOptions gumOptions = new UserMediaOptions();
    gumOptions.getVideoOptions().setDeviceId(this.screenCaptureDeviceId);
    if (this.pcast == null) {
      this.pcast = this.phenixApplication.getPCast();
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
            didGetUserMedia(userMediaStream, isShare);
          } else {
            MainActivity.this.onTryAfterError(getResources().getString(R.string.render_error, status.name()));
          }
        }
      });
  }

  private void didGetUserMedia(UserMediaStream userMediaStream, boolean isShare) {
    if (userMediaStream == null) {
      this.onTryAfterError(getResources().getString(R.string.media_null));
      return;
    }
    if (this.publishMedia != null) {
      if (!this.publishMedia.isClosed()) {
        Utilities.close(this, this.publishMedia);
      }
      this.publishMedia = null;
    }
    this.publishMedia = userMediaStream;
    if (isShare) {
      this.getPublishToken();
    }
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
        if (MainActivity.this.pulsator.getVisibility() == View.GONE) {
          MainActivity.this.progressBar.setVisibility(View.VISIBLE);
        }
      }
    });
  }

  public void onRemove() {
    this.isStarted = false;
  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    switch (view.getId()) {
      case R.id.textViewVersion:
        this.onShowSecretMenu(motionEvent);
        return true;
    }
    return false;
  }

  public void onShowSecretMenu(MotionEvent motionEvent) {
    int eventaction = motionEvent.getAction();
    if (eventaction == MotionEvent.ACTION_DOWN) {
      long time = System.currentTimeMillis();
      if (this.startMillis == 0 || (time - this.startMillis > TIME_TO_TAP)) {
        this.startMillis = time;
        this.count = 1;
      } else {
        this.count++;
      }
      if (this.count == NUMBER_TOUCHES) {
        new UriMenu().onActionSecretUrl(this);
      }
    }
  }

  public void setGoneVersion() {
    this.textViewVersion.setVisibility(View.GONE);
  }
}