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

package com.phenixp2p.demo.ui.fragments;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;
import com.phenixp2p.demo.Capabilities;
import com.phenixp2p.demo.CaptureHelper;
import com.phenixp2p.demo.PhenixApplication;
import com.phenixp2p.demo.R;
import com.phenixp2p.demo.RxBus;
import com.phenixp2p.demo.events.Events;
import com.phenixp2p.demo.presenters.MainActivityPresenter;
import com.phenixp2p.demo.presenters.inter.IMainActivityPresenter;
import com.phenixp2p.demo.ui.ArcImage;
import com.phenixp2p.demo.ui.QualityStatusView;
import com.phenixp2p.demo.ui.activities.MainActivity;
import com.phenixp2p.demo.ui.view.IDetailStreamView;
import com.phenixp2p.demo.utils.DialogUtils;
import com.phenixp2p.demo.utils.TokenUtils;
import com.phenixp2p.pcast.DataQualityReason;
import com.phenixp2p.pcast.DataQualityStatus;
import com.phenixp2p.pcast.MediaStream;
import com.phenixp2p.pcast.PCast;
import com.phenixp2p.pcast.Renderer;
import com.phenixp2p.pcast.RendererStartStatus;
import com.phenixp2p.pcast.RequestStatus;
import com.phenixp2p.pcast.StreamEndedReason;
import com.phenixp2p.pcast.android.AndroidVideoRenderSurface;

import java.io.IOException;

import io.fabric.sdk.android.Fabric;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static com.phenixp2p.demo.Constants.ANIMATION_DURATION;
import static com.phenixp2p.demo.Constants.ANIMATOR_VALUE_RANGE;
import static com.phenixp2p.demo.Constants.ENDPOINT;
import static com.phenixp2p.demo.Constants.IS_AUDIO;
import static com.phenixp2p.demo.Constants.IS_CAMERA_FRONT;
import static com.phenixp2p.demo.Constants.IS_PUBLISH_STOPPED;
import static com.phenixp2p.demo.Constants.NULL_STREAM_TOKEN;
import static com.phenixp2p.demo.Constants.SESSION_ID;
import static com.phenixp2p.demo.Constants.STOP_PUBLISHER;
import static com.phenixp2p.demo.Constants.STREAM_ID_FROM_LIST;
import static com.phenixp2p.demo.Constants.VIEW_STREAM;
import static com.phenixp2p.demo.utils.DialogUtils.showDialog;
import static com.phenixp2p.demo.utils.DialogUtils.showToast;
import static com.phenixp2p.demo.utils.LayoutUtils.animateView;
import static com.phenixp2p.demo.utils.LayoutUtils.setLayout;
import static com.phenixp2p.demo.utils.LayoutUtils.setPreviewDimensions;
import static com.phenixp2p.demo.utils.TokenUtils.getStreamId;
import static com.phenixp2p.demo.utils.Utilities.handleException;
import static com.phenixp2p.pcast.DataQualityReason.DOWNLOAD_LIMITED;
import static com.phenixp2p.pcast.DataQualityReason.NETWORK_LIMITED;
import static com.phenixp2p.pcast.DataQualityReason.NONE;
import static com.phenixp2p.pcast.DataQualityReason.PUBLISHER_LIMITED;
import static com.phenixp2p.pcast.DataQualityReason.UPLOAD_LIMITED;

public final class ViewDetailStreamFragment extends BaseFragment implements MediaStream.StreamEndedCallback,
  IDetailStreamView, CompoundButton.OnCheckedChangeListener,
  RadioGroup.OnCheckedChangeListener, View.OnClickListener,
  ValueAnimator.AnimatorUpdateListener,
  Renderer.DataQualityChangedCallback, Handler.Callback {

  private static final String TAG = ViewDetailStreamFragment.class.getSimpleName();
  private static final int DELAY = 3000;
  private String streamId;
  private String sessionId;
  private boolean first = false;
  private Capabilities capability;
  private boolean isStreamEnded = false;
  private MediaStream mediaStream;
  private IMainActivityPresenter presenter;

  private TextView tvStreamId;
  private SurfaceView renderSurface, previewLocal;
  private ViewGroup viewLocal, buttonVideoAudio, viewState;
  private RadioButton filterRealTime, filterBroadcast, filterLive;
  private PopupWindow popupWindow;
  private RelativeLayout goBack;
  private boolean isStop = true;
  private boolean isCameraFront;
  private SurfaceHolder renderHolder, previewLocalHolder;
  private ToggleButton toggleCamera;
  private View viewStop;
  private Renderer renderer, renderPreView;
  private ArcImage arcImage;
  private ValueAnimator animator;
  private ImageView imageLoad, buttonStop, buttonAudio, buttonVideo, buttonShareScreen, imageAudio, imageVideo, imageType;
  private Handler handler;
  private boolean isStart = false;
  private QualityStatusView qualityPublisher, qualityRenderer;
  private boolean isPublishStopped;
  private boolean isAudio = false;
  private int count = 0;
  private long startMillis = 0;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle args = getArguments();
    if (args != null) {
      this.streamId = args.getString(STREAM_ID_FROM_LIST);
      this.sessionId = args.getString(SESSION_ID);
      this.isCameraFront = args.getBoolean(IS_CAMERA_FRONT);
      this.isPublishStopped = args.getBoolean(IS_PUBLISH_STOPPED);
      this.isAudio = args.getBoolean(IS_AUDIO);
    }
    presenter = new MainActivityPresenter(this);
  }

  @Override
  protected int getFragmentLayout() {
    return R.layout.fragment_view_stream;
  }

  @Override
  protected void bindEventHandlers(View view) {
    this.handler = new Handler(this);
    this.renderSurface = (SurfaceView) view.findViewById(R.id.surfaceView);
    this.previewLocal = (SurfaceView) view.findViewById(R.id.previewLocal);
    this.tvStreamId = (TextView) view.findViewById(R.id.tvStreamId);
    Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
    this.toggleCamera = (ToggleButton) view.findViewById(R.id.toggleCamera);
    this.goBack = (RelativeLayout) view.findViewById(R.id.back);
    RelativeLayout menu = (RelativeLayout) view.findViewById(R.id.menu);
    this.viewStop = view.findViewById(R.id.viewStop);
    this.imageLoad = (ImageView) view.findViewById(R.id.imageLoad);
    this.qualityPublisher = (QualityStatusView) view.findViewById(R.id.qualityPublisher);
    this.qualityRenderer = (QualityStatusView) view.findViewById(R.id.qualityRenderer);
    this.buttonAudio = (ImageView) view.findViewById(R.id.audio);
    this.buttonVideo = (ImageView) view.findViewById(R.id.video);
    this.imageAudio = (ImageView) view.findViewById(R.id.imageAudio);
    this.imageVideo = (ImageView) view.findViewById(R.id.imageVideo);
    this.buttonShareScreen = (ImageView) view.findViewById(R.id.share);
    this.buttonVideoAudio = (LinearLayout) view.findViewById(R.id.play);
    this.imageType = (ImageView) view.findViewById(R.id.imageType);
    this.viewState = (LinearLayout) view.findViewById(R.id.viewState);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      this.viewLocal = (CardView) view.findViewById(R.id.viewLocal);
    } else {
      this.viewLocal = (RelativeLayout) view.findViewById(R.id.viewLocal);
      this.previewLocal.setZOrderOnTop(false);
      this.previewLocal.setZOrderMediaOverlay(true);
    }
    this.toggleCamera.setChecked(this.isCameraFront);
    if (!this.isCameraFront) {
      this.toggleCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(),
              R.drawable.ic_camera_rear),
              null, null, null);
    } else {
      this.toggleCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(),
              R.drawable.ic_camera_front),
              null, null, null);
    }
    this.toggleCamera.setOnCheckedChangeListener(this);
    this.renderSurface.setOnClickListener(this);
    this.buttonShareScreen.setOnClickListener(this);
    this.buttonVideoAudio.setOnClickListener(this);
    this.buttonVideo.setOnClickListener(this);
    this.buttonAudio.setOnClickListener(this);
    this.goBack.setOnClickListener(this);
    menu.setOnClickListener(this);
    AppCompatActivity activity = (AppCompatActivity) getActivity();
    activity.setSupportActionBar(toolbar);
    if (activity.getSupportActionBar() != null) {
      activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
      activity.getSupportActionBar().setTitle("");
    }
    setPreviewDimensions(getActivity(), this.viewLocal);
    animateView(this.imageLoad);
    this.arcImage = (ArcImage) view.findViewById(R.id.arcImage);
    LayoutInflater layoutInflater = (LayoutInflater) getActivity()
      .getSystemService(LAYOUT_INFLATER_SERVICE);
    @SuppressLint("InflateParams")
    View popupView = layoutInflater.inflate(R.layout.popup_select, null);
    this.popupWindow = new PopupWindow(popupView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    RadioGroup radioGroup = (RadioGroup) popupView.findViewById(R.id.radioGroup);
    this.filterBroadcast = (RadioButton) popupView.findViewById(R.id.filterBroadcast);
    this.filterRealTime = (RadioButton) popupView.findViewById(R.id.filterRealTime);
    this.filterLive = (RadioButton) popupView.findViewById(R.id.filterLive);
    this.buttonStop = (ImageView) view.findViewById(R.id.record);
    animateView(buttonStop);
    this.buttonStop.setOnClickListener(this);
    radioGroup.setOnCheckedChangeListener(this);
    this.renderHolder = this.renderSurface.getHolder();
    this.previewLocalHolder = this.previewLocal.getHolder();
    this.tvStreamId.setText(getStreamId(this.streamId));
    if (this.isPublishStopped) {
      this.qualityPublisher.setVisibility(View.GONE);
    } else {
      this.qualityPublisher.setVisibility(View.VISIBLE);
    }
    if (this.capability == null) {
      this.capability = Capabilities.REAL_TIME;
    }
    if (!this.isStart && getMainActivity().getDataQualityReason() != null
            && getMainActivity().getDataQualityReason() != null) {
      setViewQuality(false, getMainActivity().getDataQualityStatus(), getMainActivity().getDataQualityReason());
    }
    getSubscribeToken(this.sessionId, this.streamId, this.capability.getValue());
  }

  @Override
  protected void reloadWhenOpened() {
  }

  @Override
  public void onPause() {
    this.first = true;
    if (this.isStop) {
      stopRender();
    }
    onStopPreview();
    this.buttonStop.setVisibility(View.GONE);
    if (this.popupWindow != null && popupWindow.isShowing()) {
      this.popupWindow.dismiss();
    }
    this.goBack.setVisibility(View.GONE);
    this.toggleCamera.setVisibility(View.GONE);
    if (this.toggleCamera.isChecked()) {
      this.toggleCamera.setChecked(false);
    }
    this.imageLoad.clearAnimation();
    setHasOptionsMenu(false);
    this.animator.cancel();
    this.animator.end();
    this.arcImage.clearAnimation();
    this.arcImage.setAnimation(null);
    if (this.handler != null) {
      handler = null;
    }
    this.isStart = false;
    super.onPause();
  }

  // Stop render stream, and render preview local
  private void stopRender() {
    if (this.mediaStream != null && !this.mediaStream.isClosed()) {
      this.mediaStream.stop();
      try {
        this.mediaStream.close();
      } catch (IOException e) {
        handleException(getActivity(), e);
      }
      this.mediaStream = null;
    }

    if (this.renderer != null && !this.renderer.isClosed()) {
      this.renderer.stop();
      try {
        this.renderer.close();
      } catch (IOException e) {
        handleException(getActivity(), e);
      }
      this.renderer = null;
    }

    onStopPreview();
    this.renderHolder = null;
    this.previewLocalHolder = null;
  }

  private void onStopPreview() {
    if (this.renderPreView != null && !this.renderPreView.isClosed()) {
      this.renderPreView.stop();
      try {
        this.renderPreView.close();
      } catch (IOException e) {
        handleException(getActivity(), e);
      }
      this.renderPreView = null;
    }
  }

  public void callReload(String streamId) {
    if (this.first) {
      this.renderHolder = this.renderSurface.getHolder();
      this.previewLocalHolder = this.previewLocal.getHolder();
      if (this.capability == null) {
        this.capability = Capabilities.REAL_TIME;
      }
      getSubscribeToken(this.sessionId, this.streamId, this.capability.getValue());
      this.tvStreamId.setText(getStreamId(streamId));
      if (this.handler == null) {
        this.handler = new Handler(this);
      }
    } else {
      if (this.renderPreView == null && this.previewLocalHolder != null) {
        this.renderPreView = getMainActivity().getPublishMedia().getMediaStream().createRenderer();
        RendererStartStatus rendererStartStatus = this.renderPreView.start(new AndroidVideoRenderSurface(this.previewLocalHolder));
        if (this.isAudio) {
          this.imageType.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_audio_gray));
          this.imageType.setVisibility(View.VISIBLE);
          this.viewStop.setVisibility(View.VISIBLE);
        } else {
          if (((PhenixApplication) getActivity().getApplicationContext()).isShare()) {
            this.imageType.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_share_src_gray));
            this.imageType.setVisibility(View.VISIBLE);
          } else {
            this.imageType.setVisibility(View.GONE);
          }
          this.viewStop.setVisibility(View.GONE);
        }
        if (this.qualityPublisher.getVisibility() == View.GONE) {
          this.qualityPublisher.setVisibility(View.VISIBLE);
        }
        if (rendererStartStatus != RendererStartStatus.OK) {
          Toast.makeText(getActivity(), rendererStartStatus.name(), Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  @Override
  public void onResume() {
    if (getActivity() != null) {
      getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
      getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    this.animator = ValueAnimator.ofInt(0, ANIMATOR_VALUE_RANGE);
    this.animator.setDuration(ANIMATION_DURATION);
    this.animator.setRepeatCount(ValueAnimator.INFINITE);
    this.animator.addUpdateListener(this);
    this.animator.start();
    if (((PhenixApplication) getActivity().getApplicationContext()).isStopPublish()) {
      onViewAction();
      this.arcImage.setTypeNomal(false);
      this.toggleCamera.setVisibility(View.GONE);
      this.qualityPublisher.setVisibility(View.GONE);
    } else {
      this.qualityPublisher.setVisibility(View.VISIBLE);
    }
    super.onResume();
  }

  @Override
  public void onDestroyView() {
    this.presenter.onDestroy();
    Activity activity = getActivity();
    if (activity != null) {
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    this.first = false;
    this.renderHolder = null;
    this.previewLocalHolder = null;
    try {
      this.renderSurface.getHolder().getSurface().release();
      this.previewLocal.getHolder().getSurface().release();
    } catch (Exception e) {
      if (Fabric.isInitialized()) {
        Crashlytics.log(Log.ERROR, TAG, e.getMessage());
      }
    }
    this.renderSurface = null;
    this.previewLocal = null;
    super.onDestroyView();
  }

  // 6. Get streamToken token from REST admin API.
  private void getSubscribeToken(String sessionId, String streamId, String capability) {
    this.presenter.createStreamToken(sessionId, streamId, ENDPOINT, new String[]{capability}, new MainActivityPresenter.Streamer() {
      @Override
      public void hereIsYourStreamToken(String streamToken) {
        subscribeStream(streamToken);
      }
    });
  }

  // 7. Subscribe to streamToken with SDK.
  private void subscribeStream(String subscribeStreamToken) {
    if (getMainActivity() == null && getMainActivity().getPCast() == null && isAdded()) {
      return;
    }
    final MainActivity activity = getMainActivity();
    activity.getPCast().subscribe(subscribeStreamToken, new PCast.SubscribeCallback() {
      public void onEvent(PCast p, final RequestStatus status, final MediaStream media) {
        if (status == RequestStatus.OK) {
          ViewDetailStreamFragment.this.mediaStream = media;
          ViewDetailStreamFragment.this.mediaStream.setStreamEndedCallback(ViewDetailStreamFragment.this);
          // Prepare the player with the streaming source.
          viewStream(ViewDetailStreamFragment.this.mediaStream.createRenderer());
        } else {
          getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              showDialog(status.name(), getString(R.string.stop_pub), new DialogUtils.ActionDialog() {
                @Override
                public AppCompatActivity getContext() {
                  return activity;
                }

                @Override
                public void buttonYes() {
                  activity.getSupportFragmentManager().popBackStack();
                }

                @Override
                public void autoDismiss(final AlertDialog alertDialog) {
                  new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      if (getMainActivity() != null && isVisible()) {
                        alertDialog.dismiss();
                        getMainActivity().getSupportFragmentManager().popBackStack();
                      }
                    }
                  }, DELAY);
                }
              });
            }
          });
        }
      }
    });
  }

  // 8. View streamToken.
  private void viewStream(final Renderer newRenderer) {
    this.renderer = newRenderer;
    if (this.handler != null) {
      Message message = Message.obtain(handler, VIEW_STREAM, this.renderer);
      message.sendToTarget();
    }
  }

  //Called by the system when the activity changes orientation
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    arcImage.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.with_parabol);
    // Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      setLayout(getActivity(),
              true,
              this.buttonAudio,
              this.buttonVideo,
              this.buttonVideoAudio,
              this.buttonShareScreen,
              this.imageAudio,
              this.imageVideo);
      ((PhenixApplication) getActivity().getApplicationContext()).setLandscape(true);
      setPreviewDimensions(getActivity(), this.viewLocal);
      hideSystemUI();
      this.renderSurface.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
          showSystemUI();
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              hideSystemUI();
            }
          }, DELAY);
        }
      });
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
      setLayout(getActivity(),
              false,
              this.buttonAudio,
              this.buttonVideo,
              this.buttonVideoAudio,
              this.buttonShareScreen,
              this.imageAudio,
              this.imageVideo);
      ((PhenixApplication) getActivity().getApplicationContext()).setLandscape(false);
      setPreviewDimensions(getActivity(), this.viewLocal);
      showSystemUI();
    }
  }

  // This snippet hides the system bars.
  private void hideSystemUI() {
    Activity activity = getMainActivity();
    if (activity != null && isVisible() && isAdded()) {
      getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
  }

  // This snippet shows the system bars. It does this by removing all the flags
  // except for the ones that make the content appear under the system bars.
  private void showSystemUI() {
    Activity activity = getMainActivity();
    if (activity != null && isVisible() && isAdded()) {
      getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
  }

  // Call back event for when the publisher stopped
  @Override
  public void onEvent(MediaStream mediaStream, final StreamEndedReason streamEndedReason, final String s) {
    this.isStop = false;
    if (this.handler != null) {
      Message message = Message.obtain(handler, STOP_PUBLISHER, streamEndedReason);
      message.sendToTarget();
    }
  }

  // View error on createStreamToken token from REST admin API
  @Override
  public void onError(String error) {
    final AppCompatActivity activity = getMainActivity();
    if (activity != null && isVisible() && isAdded()) {
      if (error.equals("HTTP 410 ")) {
        showDialog("Notification", getString(R.string.stop_pub), new DialogUtils.ActionDialog() {
          @Override
          public AppCompatActivity getContext() {
            return (AppCompatActivity) getActivity();
          }

          @Override
          public void buttonYes() {
            stopRender();
            activity.getSupportFragmentManager().popBackStack();
          }

          @Override
          public void autoDismiss(final AlertDialog alertDialog) {
            new Handler().postDelayed(new Runnable() {
              @Override
              public void run() {
                if (getMainActivity() != null && isVisible()) {
                  alertDialog.dismiss();
                  getMainActivity().getSupportFragmentManager().popBackStack();
                }
              }
            }, DELAY);
          }
        });
      } else {
        showToast(activity, error);
      }
    }
  }

  //return if api null stream token
  @Override
  public void onNullStreamToken() {
    if (this.handler != null) {
      Message message = Message.obtain(this.handler, NULL_STREAM_TOKEN);
      message.sendToTarget();
    }
  }

  @Override
  public void onCheckedChanged(final CompoundButton compoundButton, boolean isChecked) {
    switch (compoundButton.getId()) {
      case R.id.toggleCamera:
        try {
          if (isChecked) {
            compoundButton.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_front), null, null, null);
            RxBus.getInstance().post(new Events.ChangeCamera(true));
          } else {
            compoundButton.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_rear), null, null, null);
            RxBus.getInstance().post(new Events.ChangeCamera(false));
          }
        } catch (NullPointerException e) {
          showToast(getActivity(), e.getMessage());
        }
        break;
    }
  }

  @Override
  public void onCheckedChanged(RadioGroup radioGroup, final int idChecked) {
    Capabilities capability;
    switch (idChecked) {
      case R.id.filterRealTime:
        capability = Capabilities.REAL_TIME;
        break;
      case R.id.filterLive:
        capability = Capabilities.STREAMING;
        break;
      case R.id.filterBroadcast:
        capability = Capabilities.BROADCAST;
        break;
      default:
        capability = null;
    }
    if (capability != null) {
      if (this.capability != capability) {
        stopRender();
        this.isStreamEnded = true;
        this.capability = capability;
        this.renderHolder = renderSurface.getHolder();
        this.previewLocalHolder = previewLocal.getHolder();
        getSubscribeToken(sessionId, streamId, this.capability.getValue());
        if (this.popupWindow != null) {
          this.popupWindow.dismiss();
        }
      }
      switch (this.capability) {
        case REAL_TIME:
          this.filterRealTime.setChecked(true);
          this.filterLive.setChecked(false);
          this.filterBroadcast.setChecked(false);
          break;
        case STREAMING:
          this.filterRealTime.setChecked(false);
          this.filterLive.setChecked(true);
          this.filterBroadcast.setChecked(false);
          break;
        case BROADCAST:
          this.filterRealTime.setChecked(false);
          this.filterLive.setChecked(false);
          this.filterBroadcast.setChecked(true);
          break;
      }
    }
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.surfaceView:
        if (this.popupWindow != null && this.popupWindow.isShowing()) {
          this.popupWindow.dismiss();
        }
        break;
      case R.id.back:
        stopRender();
        if (getMainActivity() != null) {
          getMainActivity().getSupportFragmentManager().popBackStack();
        }
        break;
      case R.id.menu:
        this.popupWindow.showAsDropDown(view, 50, - 80);
        break;
      case R.id.record:
        this.arcImage.setTypeNomal(false);
        this.imageType.setVisibility(View.GONE);
        onStopPreview();
        onViewAction();
        RxBus.getInstance().post(new Events.OnStopStream());
        this.buttonStop.setVisibility(View.GONE);
        this.viewStop.setVisibility(View.VISIBLE);
        this.toggleCamera.setVisibility(View.GONE);
        this.qualityPublisher.setVisibility(View.GONE);
        ((PhenixApplication) getActivity().getApplicationContext()).setStopPublish(true);
        break;
      case R.id.audio:
        this.isAudio = true;
        this.imageType.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_audio_gray));
        this.imageType.setVisibility(View.VISIBLE);
        ((MainActivity) getActivity()).onlyVideoOrAudio(false);
        onResumePublish();
        this.toggleCamera.setVisibility(View.GONE);
        break;
      case R.id.video:
        this.imageType.setVisibility(View.GONE);
        onResumePublish();
        ((MainActivity) getActivity()).onlyVideoOrAudio(true);
        break;
      case R.id.share:
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
          this.imageType.setVisibility(View.VISIBLE);
          this.imageType.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_share_src_gray));
          onResumePublish();
          TokenUtils.saveSessionIdIntoLocal(getActivity(), sessionId);
          TokenUtils.saveStreamIdIntoLocal(getActivity(), streamId);
          this.isAudio = false;
          onResumePublish();
          ((MainActivity) getActivity()).onStartShareScreen();
          CaptureHelper.fireScreenCaptureIntent(getActivity());
        } else {
          Toast.makeText(getActivity(), "Support Device ANDROID 5.0 OR LATER", Toast.LENGTH_SHORT).show();
        }
        break;
      case R.id.play:
        this.imageType.setVisibility(View.GONE);
        RxBus.getInstance().post(new Events.OnRestartStream());
        onResumePublish();
        break;
    }
  }

  private void onViewAction() {
    this.buttonStop.setVisibility(View.GONE);
    this.buttonAudio.setVisibility(View.VISIBLE);
    this.buttonVideo.setVisibility(View.VISIBLE);
    this.buttonShareScreen.setVisibility(View.VISIBLE);
    this.buttonVideoAudio.setVisibility(View.VISIBLE);
  }

  @Override
  public void onAnimationUpdate(ValueAnimator animation) {
    this.arcImage.setProcess((Integer) animation.getAnimatedValue());
  }

  //Data Quality Status For Viewers
  @Override
  public void onEvent(Renderer renderer, DataQualityStatus dataQualityStatus, final DataQualityReason dataQualityReason) {
    RxBus.getInstance().post(new Events.OnStateDataQuality(true, dataQualityStatus, dataQualityReason));
  }

  @Override
  public boolean handleMessage(final Message message) {
    if (message.what == VIEW_STREAM) {
      onViewStream((Renderer) message.obj);
    }

    if (message.what == STOP_PUBLISHER) {
      onStopStream(message);
    }

    if (message.what == NULL_STREAM_TOKEN) {
      showDialog("Notification", getString(R.string.stop_pub), new DialogUtils.ActionDialog() {
        @Override
        public AppCompatActivity getContext() {
          return (AppCompatActivity) getActivity();
        }

        @Override
        public void buttonYes() {
          stopRender();
          if (getMainActivity() != null) {
            getMainActivity().getSupportFragmentManager().popBackStack();
          }
        }

        @Override
        public void autoDismiss(AlertDialog alertDialog) {}
      });
    }
    return false;
  }

  private void onViewStream(Renderer renderer) {
    RendererStartStatus rendererStartStatus;
    if (renderer != null) {
      renderer.setDataQualityChangedCallback(ViewDetailStreamFragment.this);
      rendererStartStatus = renderer.start(new AndroidVideoRenderSurface(renderHolder));
      if (rendererStartStatus != RendererStartStatus.OK) {
        showDialog(getResources().getString(R.string.render_error, rendererStartStatus.name()),
          getString(R.string.please_try), new DialogUtils.ActionDialog() {
            @Override
            public AppCompatActivity getContext() {
              return (AppCompatActivity) getActivity();
            }

            @Override
            public void buttonYes() {
              if (getMainActivity() != null) {
                getMainActivity().getSupportFragmentManager().popBackStack();
              }
            }

            @Override
            public void autoDismiss(AlertDialog alertDialog) {

            }
          });
      } else {
        this.imageLoad.clearAnimation();
        if (this.imageLoad.getVisibility() == View.VISIBLE)
          this.imageLoad.setVisibility(View.GONE);
        setHasOptionsMenu(true);
        this.goBack.setVisibility(View.VISIBLE);
      }
    }

    if (this.previewLocalHolder != null && !this.isPublishStopped) {
      if (getMainActivity() != null && getMainActivity().getPublishMedia() == null) {
        this.viewStop.setVisibility(View.VISIBLE);
        return;
      }
      if (this.isPublishStopped) {
        onViewAction();
      } else {
        this.buttonStop.setVisibility(View.VISIBLE);
        this.buttonAudio.setVisibility(View.GONE);
        this.buttonVideo.setVisibility(View.GONE);
        this.buttonShareScreen.setVisibility(View.GONE);
        this.buttonVideoAudio.setVisibility(View.GONE);
      }
      this.qualityPublisher.setVisibility(View.VISIBLE);
      this.renderPreView = getMainActivity().getPublishMedia().getMediaStream().createRenderer();
      rendererStartStatus = renderPreView.start(new AndroidVideoRenderSurface(previewLocalHolder));
      if (rendererStartStatus != RendererStartStatus.OK) {
        showDialog(getResources().getString(R.string.render_error, rendererStartStatus.name()),
          getString(R.string.please_try), new DialogUtils.ActionDialog() {
            @Override
            public AppCompatActivity getContext() {
              return (AppCompatActivity) getActivity();
            }

            @Override
            public void buttonYes() {
              getMainActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void autoDismiss(AlertDialog alertDialog) {

            }
          });
      } else {
        setHasOptionsMenu(true);
        this.goBack.setVisibility(View.VISIBLE);
        this.renderPreView.muteAudio();
        this.viewStop.setVisibility(View.GONE);
        if (this.isAudio) {
          this.imageType.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_audio_gray));
          this.imageType.setVisibility(View.VISIBLE);
          this.toggleCamera.setVisibility(View.GONE);
        } else {
          if (((PhenixApplication) getActivity().getApplicationContext()).isShare()) {
            this.imageType.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_share_src_gray));
            this.imageType.setVisibility(View.VISIBLE);
          } else {
            this.imageType.setVisibility(View.GONE);
          }
          toggleCamera.setVisibility(View.VISIBLE);
        }
      }
    } else {
      this.toggleCamera.setVisibility(View.GONE);
      this.viewStop.setVisibility(View.VISIBLE);
      this.qualityPublisher.setVisibility(View.GONE);
    }
  }

  //stop stream when publish stop
  private void onStopStream(Message message) {
    StreamEndedReason endedReason = ((StreamEndedReason) message.obj);
    if ((endedReason == StreamEndedReason.ENDED || endedReason == StreamEndedReason.CUSTOM ||
      endedReason == StreamEndedReason.APP_BACKGROUND || endedReason == StreamEndedReason.FAILED)
      && !this.isStreamEnded) {
      if (getActivity() != null && isVisible()) {
        showDialog("Notification", getString(R.string.stop_pub), new DialogUtils.ActionDialog() {
          @Override
          public AppCompatActivity getContext() {
            return (AppCompatActivity) getActivity();
          }

          @Override
          public void buttonYes() {
            stopRender();
            if (getMainActivity() != null) {
              getMainActivity().getSupportFragmentManager().popBackStack();
            }
          }

          @Override
          public void autoDismiss(final AlertDialog alertDialog) {
            if (getMainActivity() != null && isVisible()) {
              new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                  if (getMainActivity() != null && isVisible()) {
                    stopRender();
                    alertDialog.dismiss();
                    if (getMainActivity().isSaveInstance()) {
                      getMainActivity().setEnableBackPress(true);
                    } else {
                      getMainActivity().onBackPressed();
                    }
                  }
                }
              }, DELAY);

            }
          }
        });
        this.tvStreamId.setVisibility(View.GONE);
      }
    } else {
      this.isStreamEnded = false;
    }
  }

  //set view data quality publish and subscribe
  private void setViewQuality(final boolean isRender, final DataQualityStatus dataQualityStatus, final DataQualityReason dataQualityReason) {
    final Activity activity = getActivity();
    if (activity != null && isAdded()) {
      if (isRender) {
        //view data quality subscribe
        switch (dataQualityStatus) {
          case NO_DATA:
            if (dataQualityReason == NONE || dataQualityReason == DOWNLOAD_LIMITED || dataQualityReason == PUBLISHER_LIMITED
              || dataQualityReason == NETWORK_LIMITED) {
              qualityRenderer.setStatusShow(0);
            }
            break;
          case ALL:
            if (dataQualityReason == NONE) {
              this.qualityRenderer.setStatusShow(4);
            } else if (dataQualityReason == DOWNLOAD_LIMITED) {
              this.qualityRenderer.setStatusShow(2);
            } else if (dataQualityReason == PUBLISHER_LIMITED || dataQualityReason == NETWORK_LIMITED) {
              this.qualityRenderer.setStatusShow(3);
            }
            break;
          case AUDIO_ONLY:
            if (dataQualityReason == NONE) {
              this.qualityRenderer.setStatusShow(4);
            } else if (dataQualityReason == DOWNLOAD_LIMITED || dataQualityReason == PUBLISHER_LIMITED
              || dataQualityReason == NETWORK_LIMITED) {
              this.qualityRenderer.setStatusShow(1);
            }
            break;
        }
      } else {
        //view data quaity publish
        switch (dataQualityStatus) {
          case NO_DATA:
            if (dataQualityReason == NONE || dataQualityReason == UPLOAD_LIMITED) {
              this.qualityPublisher.setStatusShow(0);
            }
            break;
          case ALL:
            if (dataQualityReason == NONE) {
              this.qualityPublisher.setStatusShow(4);
            } else if (dataQualityReason == UPLOAD_LIMITED) {
              this.qualityPublisher.setStatusShow(2);
            } else if (dataQualityReason == NETWORK_LIMITED) {
              this.qualityPublisher.setStatusShow(3);
            }
            break;
          case AUDIO_ONLY:
            if (dataQualityReason == NONE) {
              this.qualityPublisher.setStatusShow(4);
            } else if (dataQualityReason == UPLOAD_LIMITED || dataQualityReason == NETWORK_LIMITED) {
              this.qualityPublisher.setStatusShow(1);
            }
            break;
        }
      }
    }
  }

  //Subscribe events
  @Override
  protected Subscription subscribeEvents() {
    return RxBus.getInstance().toObservable()
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext(new Action1<Object>() {
        @Override
        public void call(Object objectEvent) {
          if (objectEvent instanceof Events.OnStateDataQuality) {
            ViewDetailStreamFragment.this.isStart = true;
            setViewQuality(((Events.OnStateDataQuality) objectEvent).isStateRender,
              ((Events.OnStateDataQuality) objectEvent).dataQualityStatus,
              ((Events.OnStateDataQuality) objectEvent).dataQualityReason);
          }
        }
      }).subscribe(RxBus.defaultSubscriber());
  }

  private void onResumePublish() {
    this.arcImage.setTypeNomal(true);
    this.animator.start();
    this.buttonStop.setVisibility(View.VISIBLE);
    this.buttonVideoAudio.setVisibility(View.GONE);
    this.buttonAudio.setVisibility(View.GONE);
    this.buttonVideo.setVisibility(View.GONE);
    this.buttonShareScreen.setVisibility(View.GONE);
    this.toggleCamera.setVisibility(View.VISIBLE);
    ((PhenixApplication) getActivity().getApplicationContext()).setStopPublish(false);
  }
}
