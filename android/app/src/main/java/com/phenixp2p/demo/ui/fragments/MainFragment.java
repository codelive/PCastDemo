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
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
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
import com.phenixp2p.demo.BuildConfig;
import com.phenixp2p.demo.Capabilities;
import com.phenixp2p.demo.CaptureHelper;
import com.phenixp2p.demo.Constants;
import com.phenixp2p.demo.PhenixApplication;
import com.phenixp2p.demo.R;
import com.phenixp2p.demo.RxBus;
import com.phenixp2p.demo.events.Events;
import com.phenixp2p.demo.model.ListStreamResponse;
import com.phenixp2p.demo.presenters.MainPresenter;
import com.phenixp2p.demo.presenters.inter.IMainPresenter;
import com.phenixp2p.demo.ui.ArcImage;
import com.phenixp2p.demo.ui.QualityStatusView;
import com.phenixp2p.demo.ui.activities.MainActivity;
import com.phenixp2p.demo.ui.adapter.StreamIdAdapter;
import com.phenixp2p.demo.ui.view.IMainView;
import com.phenixp2p.demo.utils.DialogUtil;
import com.phenixp2p.demo.utils.TokenUtil;
import com.phenixp2p.demo.utils.Utilities;
import com.phenixp2p.pcast.DataQualityReason;
import com.phenixp2p.pcast.DataQualityStatus;
import com.phenixp2p.pcast.FacingMode;
import com.phenixp2p.pcast.MediaStream;
import com.phenixp2p.pcast.PCast;
import com.phenixp2p.pcast.Renderer;
import com.phenixp2p.pcast.RendererStartStatus;
import com.phenixp2p.pcast.RequestStatus;
import com.phenixp2p.pcast.StreamEndedReason;
import com.phenixp2p.pcast.android.AndroidVideoRenderSurface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static com.phenixp2p.demo.Constants.NULL_STREAM_TOKEN;
import static com.phenixp2p.demo.Constants.NUM_HTTP_RETRIES;
import static com.phenixp2p.demo.Constants.SESSION_ID;
import static com.phenixp2p.demo.Constants.STOP_PUBLISHER;
import static com.phenixp2p.demo.Constants.STREAM_ID;
import static com.phenixp2p.demo.Constants.STREAM_LIST_LENGTH;
import static com.phenixp2p.demo.Constants.VIEW_STREAM;
import static com.phenixp2p.demo.StatusQualityValues.ALL_DOWNLOAD;
import static com.phenixp2p.demo.StatusQualityValues.ALL_UPLOAD;
import static com.phenixp2p.demo.StatusQualityValues.AUDIO_ONLY_LIMITED;
import static com.phenixp2p.demo.StatusQualityValues.AUDIO_ONLY_NONE;
import static com.phenixp2p.demo.StatusQualityValues.NO_DATA;
import static com.phenixp2p.demo.utils.DialogUtil.showDialog;
import static com.phenixp2p.demo.utils.LayoutUtil.animateView;
import static com.phenixp2p.demo.utils.LayoutUtil.dpToPx;
import static com.phenixp2p.demo.utils.LayoutUtil.setLayout;
import static com.phenixp2p.demo.utils.LayoutUtil.setLayoutTablet;
import static com.phenixp2p.demo.utils.LayoutUtil.setPreviewDimensions;
import static com.phenixp2p.demo.utils.LayoutUtil.setPreviewDimensionsTablet;
import static com.phenixp2p.demo.utils.TokenUtil.getStreamId;
import static com.phenixp2p.pcast.DataQualityReason.DOWNLOAD_LIMITED;
import static com.phenixp2p.pcast.DataQualityReason.NETWORK_LIMITED;
import static com.phenixp2p.pcast.DataQualityReason.NONE;
import static com.phenixp2p.pcast.DataQualityReason.PUBLISHER_LIMITED;
import static com.phenixp2p.pcast.DataQualityReason.UPLOAD_LIMITED;

public final class MainFragment extends BaseFragment implements View.OnClickListener,
  IMainView, StreamIdAdapter.OnItemClickListener, CompoundButton.OnCheckedChangeListener,
  SwipeRefreshLayout.OnRefreshListener, ValueAnimator.AnimatorUpdateListener, MediaStream.StreamEndedCallback,
  Renderer.DataQualityChangedCallback, Handler.Callback, RadioGroup.OnCheckedChangeListener, View.OnTouchListener {

  private static final String TAG = MainFragment.class.getSimpleName();
  private static final int DELAY = 3000;
  private static final int RENDER_DELAY = 500;
  private static final int VISIBILITY_DELAY = 1000;
  private static final int HALF_ARC = 65;
  private SurfaceView previewLocal, renderSurface;
  private View viewDelay;
  private RecyclerView recyclerView;
  private ImageView buttonStop;
  private ImageView buttonAudio;
  private ImageView buttonVideo;
  private ImageView buttonShareScreen;
  private ImageView imageAudio;
  private ImageView imageVideo;
  private ImageView imageLoad;
  private ImageView imageType;
  private ViewGroup viewList;
  private ViewGroup viewAnimation;
  private ViewGroup buttonVideoAudio;
  private ViewGroup viewLocalR;
  private ViewGroup viewToolBar;
  private ViewGroup viewVersion;
  private CardView viewLocal;
  private ToggleButton viewFull;
  private StreamIdAdapter adapter;
  private List<String> streamIdList = new ArrayList<>();
  private IMainPresenter presenter;
  private Renderer renderPreView, renderer;
  private SurfaceHolder previewLocalHolder, renderHolder;
  private ToggleButton toggleCamera;
  private SwipeRefreshLayout refreshLayout;
  private ArcImage arcImage;
  private ValueAnimator animator;
  private TextView texViewVersion, textViewNull;
  private QualityStatusView qualityPublisher;
  private QualityStatusView qualityRenderer;
  private RadioButton filterRealTime;
  private RadioButton filterBroadcast;
  private RadioButton filterLive;
  private PopupWindow popupWindow;
  private Capabilities capability;
  private RelativeLayout menu;

  private String currentSessionId;
  private String currentStreamId;
  private boolean first = true;
  private boolean isThisPhone = true;
  private boolean isCheckResume = false;
  private boolean isFullScreen = false;
  private boolean isStopPreview = false;
  private boolean isAudio = false;
  private boolean isShare = false;
  private Handler handler;
  private MediaStream mediaStream;
  private Handler handlerCall;
  private boolean isStreamEnded = false;
  private int position = 0;
  private boolean isViewDetail = false;
  private boolean isError = false;
  private boolean isPause = false;
  private boolean isBackground = false;
  private String streamIdByList;
  private Activity activity;
  private PhenixApplication phenixApplication;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    this.activity = this.getActivity();
    this.phenixApplication = ((PhenixApplication) this.activity.getApplication());
    Bundle bundle = getArguments();
    if (bundle != null) {
      if (!(isError = bundle.getBoolean(Constants.BUNDLE_ERROR))) {
        this.currentSessionId = bundle.getString(SESSION_ID);
        this.currentStreamId = bundle.getString(STREAM_ID);
      }
    }
    this.presenter = new MainPresenter(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected int getFragmentLayout() {
    return R.layout.fragment_main;
  }

  @Override
  protected void bindEventHandlers(View view) {
    this.handlerCall = new Handler(this);

    this.findSubViews(view);

    ViewGroup goBack = (RelativeLayout) view.findViewById(R.id.back);
    Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
    AppCompatActivity appCompatActivity = (AppCompatActivity) this.activity;
    appCompatActivity.setSupportActionBar(toolbar);
    if (appCompatActivity.getSupportActionBar() != null) {
      appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
      appCompatActivity.getSupportActionBar().setTitle("");
    }

    if (!this.isError) {
      this.animator = ValueAnimator.ofInt(0, 100);
      this.animator.setDuration(5000);
      this.animator.setRepeatCount(ValueAnimator.INFINITE);
      this.animator.addUpdateListener(this);
      this.animator.start();
    }
    this.refreshLayout.setOnRefreshListener(this);
    this.viewFull.setOnCheckedChangeListener(this);
    this.toggleCamera.setOnClickListener(this);
    this.buttonVideoAudio.setOnClickListener(this);
    this.buttonStop.setOnClickListener(this);
    this.buttonShareScreen.setOnClickListener(this);
    this.buttonVideo.setOnClickListener(this);
    this.buttonAudio.setOnClickListener(this);
    this.menu.setOnClickListener(this);
    this.renderSurface.setOnClickListener(this);
    this.adapter = new StreamIdAdapter(this);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.phenixApplication);
    this.recyclerView.setLayoutManager(layoutManager);
    this.recyclerView.setItemAnimator(new DefaultItemAnimator());
    this.previewLocalHolder = previewLocal.getHolder();
    this.renderHolder = renderSurface.getHolder();
    this.setVersionApp();

    goBack.setOnClickListener(this);
    animateView(this.buttonStop);
    if (!this.isError) {
      this.handler = new Handler();
      this.handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          MainFragment.this.presenter.startRendering();
        }
      }, RENDER_DELAY);
      final MainActivity mainActivity = getMainActivity();
      if (mainActivity.getDataQualityReason() != null && mainActivity.getDataQualityReason() != null) {
        this.setViewQuality(false, mainActivity.getDataQualityStatus(), mainActivity.getDataQualityReason());
      }
      if (this.capability == null) {
        this.capability = Capabilities.REAL_TIME;
      }
    }
  }

  private void findSubViews(View view) {
    this.previewLocal = (SurfaceView) view.findViewById(R.id.previewLocal);
    this.renderSurface = (SurfaceView) view.findViewById(R.id.surfaceViewSub);
    this.recyclerView = (RecyclerView) view.findViewById(R.id.recry);
    this.buttonVideoAudio = (LinearLayout) view.findViewById(R.id.play);
    this.buttonStop = (ImageView) view.findViewById(R.id.record);
    this.buttonAudio = (ImageView) view.findViewById(R.id.audio);
    this.buttonVideo = (ImageView) view.findViewById(R.id.video);
    this.buttonShareScreen = (ImageView) view.findViewById(R.id.share);
    this.imageAudio = (ImageView) view.findViewById(R.id.imageAudio);
    this.imageVideo = (ImageView) view.findViewById(R.id.imageVideo);
    this.viewDelay = view.findViewById(R.id.viewStop);
    this.viewFull = (ToggleButton) view.findViewById(R.id.imageFull);
    this.toggleCamera = (ToggleButton) view.findViewById(R.id.toggleCamera);
    this.texViewVersion = (TextView) view.findViewById(R.id.textViewVersion);
    this.refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipRefresh);
    this.viewList = (RelativeLayout) view.findViewById(R.id.viewList);
    this.viewToolBar = (RelativeLayout) view.findViewById(R.id.viewToolBar);
    this.arcImage = (ArcImage) view.findViewById(R.id.arc);
    this.viewAnimation = (RelativeLayout) view.findViewById(R.id.view);
    this.qualityPublisher = (QualityStatusView) view.findViewById(R.id.qualityPublisher);
    this.qualityRenderer = (QualityStatusView) view.findViewById(R.id.qualityRenderer);
    this.menu = (RelativeLayout) view.findViewById(R.id.menu);
    this.imageType = (ImageView) view.findViewById(R.id.imageType);
    this.textViewNull = (TextView) view.findViewById(R.id.textViewNull);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      this.viewLocal = (CardView) view.findViewById(R.id.viewLocal);
    } else {
      this.viewLocalR = (RelativeLayout) view.findViewById(R.id.viewLocal);
      this.previewLocal.setZOrderOnTop(false);
      this.previewLocal.setZOrderMediaOverlay(true);
    }
    this.imageLoad = (ImageView) view.findViewById(R.id.imageLoad);
    this.viewVersion = (RelativeLayout) view.findViewById(R.id.viewVersion);

    LayoutInflater layoutInflater = (LayoutInflater) this.activity
            .getSystemService(LAYOUT_INFLATER_SERVICE);
    View popupView = layoutInflater.inflate(R.layout.popup_select, new LinearLayout(this.activity), false);
    this.popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    RadioGroup radioGroup = (RadioGroup) popupView.findViewById(R.id.radioGroup);
    radioGroup.setOnCheckedChangeListener(this);
    this.filterBroadcast = (RadioButton) popupView.findViewById(R.id.filter_broadcast);
    this.filterRealTime = (RadioButton) popupView.findViewById(R.id.filter_real_time);
    this.filterLive = (RadioButton) popupView.findViewById(R.id.filter_live);
  }

  private void setVersionApp() {
    this.texViewVersion.setOnTouchListener(this);
    this.arcImage.setAngle(HALF_ARC);
    this.viewVersion.setBackground(null);
    String version = BuildConfig.VERSION_NAME.concat(" (" + String.valueOf(BuildConfig.VERSION_CODE) + ")");
    this.texViewVersion.setText(getResources().getString(R.string.version, version));
    this.texViewVersion.setTextColor(ContextCompat.getColor(getContext(), R.color.blue));
    if (this.isStopPreview) {
      this.arcImage.setTypeNomal(true);
      this.animator.pause();
    }
    this.capability = Capabilities.REAL_TIME;
    if (this.popupWindow != null && this.popupWindow.isShowing()) {
      this.popupWindow.dismiss();
    }
    this.clearMenuCapability();
  }

  @Override
  protected void reloadWhenOpened() {
  }

  public void callReload(String currentSessionId, String currentStreamId) {
    this.first = false;
    this.isStopPreview = false;
    if (this.previewLocalHolder == null) {
      this.previewLocalHolder = this.previewLocal.getHolder();
    }
    this.currentSessionId = currentSessionId;
    this.currentStreamId = currentStreamId;
    this.presenter.startRendering();
    if (this.isViewDetail) {
      this.getSubscribeToken(currentSessionId, this.streamIdByList, this.capability.getValue());
    }
    this.isThisPhone = true;
    onGetListStream(this.isBackground);
  }

  private void onGetListStream(boolean isForeground) {
    if (isForeground) {
      this.presenter.listStreams(STREAM_LIST_LENGTH, this.phenixApplication.getServerAddress());
    }
  }

  private synchronized void onFullScreen() {
    this.arcImage.setVisibility(View.GONE);
    this.activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      this.viewLocal.setLayoutParams(layoutParams);
      this.viewLocal.setContentPadding(0, 0, 0, 0);
      this.viewLocal.setRadius(0);
    } else {
      this.viewLocalR.setLayoutParams(layoutParams);
    }
  }

  @Override
  public void onPause() {
    this.presenter.onDestroy();
    this.onStopRender();
    boolean dontShare = !this.phenixApplication.isShare();
    if (!this.isFullScreen) {
      if (this.viewFull.isChecked()) {
        this.viewFull.setChecked(false);
      }
    }

    if (dontShare) {
      this.disappearSharing();
    } else {
      this.handlerCall = null;
      this.handler = null;
    }
    this.first = true;

    this.destroyArcCircle();

    if (this.isViewDetail) {
      this.imageType.setVisibility(View.GONE);
      this.qualityRenderer.setVisibility(View.GONE);
      this.isPause = true;
    }
    this.isBackground = true;
    super.onPause();
  }

  private void disappearSharing() {
    if (this.animator != null) {
      this.animator.pause();
    }
    this.streamIdList.clear();
    this.previewLocalHolder = null;
    this.viewDelay.setVisibility(View.VISIBLE);
    this.buttonStop.setVisibility(View.GONE);
    this.buttonVideoAudio.setVisibility(View.GONE);
    this.arcImage.setVisibility(View.GONE);
    if (this.recyclerView != null) {
      this.recyclerView.setVisibility(View.GONE);
    }
    this.streamIdList.clear();
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      this.viewLocal.setVisibility(View.GONE);
      if (this.isAudio)
        this.previewLocal.setVisibility(View.GONE);
    } else {
      this.viewLocalR.setVisibility(View.GONE);
    }
    this.viewFull.setVisibility(View.GONE);
    this.texViewVersion.setVisibility(View.GONE);
  }

  private void destroyArcCircle() {
    if (this.arcImage != null) {
      this.arcImage.clearAnimation();
      this.arcImage.setAnimation(null);
      this.arcImage.destroyDrawingCache();
    }
  }

  @Override
  public void onResume() {
    if (!this.isFullScreen) {
      this.onChangeLayout();
    } else {
      this.onFullScreen();
    }
    this.adapter.notifyDataSetChanged();
    this.toggleCamera.setVisibility(View.GONE);
    if (this.buttonStop.getVisibility() == View.VISIBLE) {
      this.buttonVideoAudio.setVisibility(View.GONE);
    }

    if (this.toggleCamera.isChecked())
      this.toggleCamera.setChecked(true);
    this.toggleCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(this.activity, R.drawable.ic_camera_rear),
      null, null, null);

    if (this.isStopPreview) {
      this.recyclerView.setVisibility(View.GONE);
      this.setStopPreview();
    } else {
      if (this.phenixApplication.isStopPublish()) {
        this.setStopPublisher();
      } else {
        this.startPublishing();
      }
    }
    if (this.phenixApplication.isStopPublish()
      && this.phenixApplication.isLandscape()) {
      setLayout(this.activity,
              true,
              this.buttonAudio,
              this.buttonVideo,
              this.buttonVideoAudio,
              this.buttonShareScreen,
              this.imageAudio,
              this.imageVideo);
    }
    super.onResume();
  }

  private void startPublishing() {
    if (this.isShare) {
      if (this.handler == null) {
        this.handler = new Handler();
        this.handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            MainFragment.this.presenter.startRendering();
          }
        }, RENDER_DELAY);
      }
    }
    this.qualityPublisher.setVisibility(View.VISIBLE);
    this.isThisPhone = true;
    this.setGoneActionButton(View.GONE);
    if (!this.isAudio) {
      this.viewDelay.setVisibility(View.GONE);
    }

    if (this.isError) {
      this.textViewNull.setText(getResources().getString(R.string.unable));
      this.textViewNull.setVisibility(View.VISIBLE);
      ((MainActivity)this.activity).setGoneVersion();
      this.buttonStop.setVisibility(View.GONE);
    } else {
      this.textViewNull.setVisibility(View.GONE);
      this.buttonStop.setVisibility(View.VISIBLE);
    }
  }

  private void setGoneActionButton(int gone) {
    this.buttonShareScreen.setVisibility(gone);
    this.buttonVideoAudio.setVisibility(gone);
    this.buttonVideo.setVisibility(gone);
    this.buttonAudio.setVisibility(gone);
    if (this.animator != null)
      this.animator.start();
  }

  private void setStopPublisher() {
    this.isThisPhone = false;
    this.setStopPreview();
  }

  private void setStopPreview() {
    this.buttonStop.setVisibility(View.GONE);
    this.viewDelay.setVisibility(View.VISIBLE);
    this.setVisibleActionButton();
    this.viewFull.setVisibility(View.GONE);
    this.animator.pause();
    this.qualityPublisher.setVisibility(View.GONE);
  }

  private void setVisibleActionButton() {
    this.buttonShareScreen.setVisibility(View.VISIBLE);
    this.buttonVideoAudio.setVisibility(View.VISIBLE);
    this.buttonVideo.setVisibility(View.VISIBLE);
    this.buttonAudio.setVisibility(View.VISIBLE);
  }

  @Override
  public void onDestroyView() {
    if (this.handler != null) {
      this.handler = null;
    }
    try {
      this.previewLocal.getHolder().getSurface().release();
    } catch (Exception e) {
      if (Fabric.isInitialized()) {
        Crashlytics.log(Log.ERROR, TAG, e.getMessage());
      }
    }
    this.previewLocal = null;
    super.onDestroyView();
  }

  // preview local user media
  @Override
  public void previewLocalUserMedia() {
    if (this.previewLocal != null
            && getMainActivity().getPublishMedia() != null
            && this.previewLocalHolder != null) {
      this.recyclerView.setVisibility(View.VISIBLE);
      if (!this.isStopPreview) {
        this.arcImage.setVisibility(View.VISIBLE);
        this.texViewVersion.setVisibility(View.VISIBLE);
        if (!this.isAudio) {
          this.renderVideo();
        } else {
          this.viewDelay.setVisibility(View.VISIBLE);
          this.handler = new Handler();
          this.handler.postDelayed(new Runnable() {
            @Override
            public void run() {
              MainFragment.this.toggleCamera.setVisibility(View.GONE);
            }
          }, VISIBILITY_DELAY);
          this.viewFull.setVisibility(View.GONE);
        }
      }
    }
    onGetListStream(this.first);
  }

  private void renderVideo() {
    this.buttonStop.setVisibility(View.VISIBLE);
    this.setViewPreview();
    this.renderPreView = getMainActivity().getPublishMedia().getMediaStream().createRenderer();
    if (this.renderPreView.start(new AndroidVideoRenderSurface(this.previewLocalHolder)) == RendererStartStatus.OK) {
      this.viewFull.setVisibility(View.VISIBLE);
      this.setGoneActionButton(View.GONE);
      this.viewDelay.setVisibility(View.GONE);

      if (this.phenixApplication.isShare()) {
        this.toggleCamera.setVisibility(View.GONE);
      } else {
        this.toggleCamera.setVisibility(View.VISIBLE);
      }
      if (!this.isCheckResume) {
        this.buttonVideoAudio.setVisibility(View.GONE);
      }
      if (!this.buttonStop.isEnabled()) {
        this.buttonStop.setEnabled(true);
      }
    }
  }

  private void setViewPreview() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      this.viewLocal.setVisibility(View.VISIBLE);
    } else {
      this.viewLocalR.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void getListStreams(List<String> streamId) {
    if (streamId.size() > 0) {
      ListStreamResponse.Stream stream = new ListStreamResponse.Stream();
      stream.setStreamId(this.currentStreamId);
      boolean isFound = streamId.contains(stream.getStreamId());
      if (isFound) {
        streamId.remove(stream.getStreamId());
      }
      if (streamId.size() > 1) {
        Collections.sort(streamId, new Comparator<String>() {
          @Override
          public int compare(String stream, String t1) {
            return stream.compareTo(t1);
          }
        });
      }
      if (isFound) {
        streamId.add(0, stream.getStreamId());
      }

      if (this.streamIdList.size() == 0) {
        this.streamIdList = streamId;
      } else {
        this.streamIdList.clear();
        this.streamIdList = streamId;
      }

      if (this.adapter != null) {
        this.recyclerView.setAdapter(this.adapter);
        this.adapter.notifyDataSetChanged();
        this.refreshLayout.setRefreshing(false);
      }
    } else {
      this.streamIdList.clear();
      this.adapter.notifyDataSetChanged();
      this.refreshLayout.setRefreshing(false);
    }
  }

  @Override
  public void onError(final String error) {
    if (this.activity != null && isVisible() && isAdded()) {
      this.activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(activity, "Error: " + error, Toast.LENGTH_SHORT).show();
        }
      });
    }
  }

  @Override
  public void onNullStreamToken() {}

  @Override
  public void onCheckedChanged(RadioGroup radioGroup, final int idChecked) {
    Capabilities capability;
    switch (idChecked) {
      case R.id.filter_real_time:
        capability = Capabilities.REAL_TIME;
        break;
      case R.id.filter_live:
        capability = Capabilities.STREAMING;
        break;
      case R.id.filter_broadcast:
        capability = Capabilities.BROADCAST;
        break;
      default:
        capability = null;
    }
    setCapability(capability);
  }

  private void setCapability(Capabilities capability) {
    if (capability == null) {
      return;
    }

    if (this.capability != capability) {
      this.stopRender(false);
      this.isStreamEnded = true;
      this.capability = capability;
      this.renderHolder = renderSurface.getHolder();
      this.previewLocalHolder =this.previewLocal.getHolder();
      this.getSubscribeToken(this.currentSessionId, this.streamIdByList, this.capability.getValue());
      if (this.popupWindow != null) {
        this.popupWindow.dismiss();
      }
    }
    switch (this.capability) {
      case REAL_TIME:
        this.clearMenuCapability();
        break;
      case STREAMING:
        filterRealTime.setChecked(false);
        filterLive.setChecked(true);
        filterBroadcast.setChecked(false);
        break;
      case BROADCAST:
        filterRealTime.setChecked(false);
        filterLive.setChecked(false);
        filterBroadcast.setChecked(true);
        break;
    }
  }

  private void clearMenuCapability() {
    filterRealTime.setChecked(true);
    filterLive.setChecked(false);
    filterBroadcast.setChecked(false);
  }

  private void didClickPlay(MainActivity mainActivity) {
    if (!mainActivity.isStopPublish()) {
      mainActivity.onEventStopStream();
    }
    this.isAudio = false;
    this.buttonAudio.setVisibility(View.GONE);
    this.buttonVideo.setVisibility(View.GONE);
    this.buttonShareScreen.setVisibility(View.GONE);
    this.imageType.setVisibility(View.GONE);
    this.onResumePublish();
    RxBus.getInstance().post(new Events.OnRestartStream());
  }

  private void didClickRecord(MainActivity mainActivity) {
    RxBus.getInstance().post(new Events.OnStopStream());
    this.phenixApplication.setStopPublish(true);
    if (!this.isViewDetail) {
      this.animator.pause();
    } else {
      this.arcImage.setTypeNomal(false);
      this.isStreamEnded = false;
    }
    this.qualityPublisher.setVisibility(View.GONE);
    this.onStopRender();
    this.onChangeLayout();
    if (this.viewFull.isChecked())
      this.viewFull.setChecked(false);
    this.buttonVideoAudio.setVisibility(View.VISIBLE);
    this.buttonStop.setVisibility(View.GONE);
    this.viewDelay.setVisibility(View.VISIBLE);
    this.isStopPreview = true;
    this.viewFull.setVisibility(View.GONE);
    this.isShare = false;
    this.isCheckResume = true;
    this.toggleCamera.clearAnimation();
    this.toggleCamera.setVisibility(View.GONE);
    if (this.streamIdList.size() >= 1) {
      this.isThisPhone = false;
      this.streamIdList.remove(0);
      if (this.streamIdList.size() == 0) {
        this.adapter.notifyItemChanged(0);
      } else {
        this.adapter.notifyDataSetChanged();
      }
    } else {
      this.streamIdList.clear();
      this.adapter.notifyDataSetChanged();
    }
    this.buttonAudio.setVisibility(View.VISIBLE);
    this.buttonVideo.setVisibility(View.VISIBLE);
    this.buttonShareScreen.setVisibility(View.VISIBLE);
    this.imageType.setVisibility(View.GONE);
  }

  private void didClickToggleCamera(View view) {
    ToggleButton toggleButton = (ToggleButton) view;
    if (toggleButton.isChecked()) {
      RxBus.getInstance().post(new Events.ChangeCamera(true));
    } else {
      RxBus.getInstance().post(new Events.ChangeCamera(false));
    }
  }

  private void didClickAudio(MainActivity mainActivity) {
    if (!mainActivity.isStopPublish()) {
      mainActivity.onEventStopStream();
    }
    this.isAudio = true;
    mainActivity.onlyVideoOrAudio(false);
    this.onResumePublish();
    this.toggleCamera.setVisibility(View.VISIBLE);
  }

  private void didClickVideo(MainActivity mainActivity) {
    if (!mainActivity.isStopPublish()) {
      mainActivity.onEventStopStream();
    }
    this.isAudio = false;
    mainActivity.onlyVideoOrAudio(true);
    this.onResumePublish();
    this.imageType.setVisibility(View.GONE);
  }

  private void didClickShare(MainActivity mainActivity) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      if (!mainActivity.isStopPublish()) {
        mainActivity.onEventStopStream();
      }
      TokenUtil.saveSessionIdIntoLocal(mainActivity, currentSessionId);
      TokenUtil.saveStreamIdIntoLocal(mainActivity, currentStreamId);
      this.isAudio = false;
      this.isShare = true;
      this.onResumePublish();
      mainActivity.onStartShareScreen();
      CaptureHelper.fireScreenCaptureIntent(mainActivity);
    } else {
      Toast.makeText(mainActivity, "Android 5.0 or later is required for this app", Toast.LENGTH_SHORT).show();
    }
  }

  //event click stop or restart review
  @Override
  public synchronized void onClick(View view) {
    MainActivity mainActivity = ((MainActivity) this.activity);
    switch (view.getId()) {
      case R.id.play:
        didClickPlay(mainActivity);
        break;
      case R.id.record:
        didClickRecord(mainActivity);
        break;
      case R.id.toggleCamera:
        didClickToggleCamera(view);
        break;
      case R.id.audio:
        didClickAudio(mainActivity);
        break;
      case R.id.video:
        didClickVideo(mainActivity);
        break;
      case R.id.share:
        didClickShare(mainActivity);
        break;
      case R.id.back:
        this.stopRender(true);
        break;
      case R.id.menu:
        this.popupWindow.showAsDropDown(view, 50, - 80);
        break;
      case R.id.surfaceViewSub:
        if (this.popupWindow != null && this.popupWindow.isShowing()) {
          this.popupWindow.dismiss();
        }
        break;
    }
  }

  // Stop render stream, and render preview local
  private void stopRender(boolean isSetVersionApp) {
    if (this.mediaStream != null && !this.mediaStream.isClosed()) {
      this.mediaStream.stop();
      Utilities.close(this.activity, this.mediaStream);
      this.mediaStream = null;
    }

    if (this.renderer != null && !this.renderer.isClosed()) {
      this.renderer.stop();
      Utilities.close(this.activity, this.renderer);
      this.renderer = null;
    }
    this.renderHolder = null;
    this.previewLocalHolder = null;
    if (isSetVersionApp) {
      this.hideComponents();
    }
  }

  private void hideComponents() {
    this.imageType.setVisibility(View.GONE);
    this.setVersionApp();
    if (this.isStopPreview) {
      this.viewFull.setVisibility(View.GONE);
    } else {
      if (this.isAudio) {
        this.viewFull.setVisibility(View.GONE);
      } else {
        this.viewFull.setVisibility(View.VISIBLE);
      }
    }
    this.viewToolBar.setVisibility(View.GONE);
    this.qualityRenderer.setVisibility(View.GONE);
    this.refreshLayout.setVisibility(View.VISIBLE);
    this.renderSurface.setVisibility(View.GONE);
    if (this.activity != null) {
      this.activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
  }

  private void onResumePublish() {
    this.qualityPublisher.setVisibility(View.VISIBLE);
    this.phenixApplication.setStopPublish(false);
    if (this.isViewDetail) {
      this.arcImage.setTypeNomal(true);
    } else {
      this.animator.start();
    }
    this.isCheckResume = false;
    this.isThisPhone = true;
    this.isStopPreview = false;
    this.viewFull.setVisibility(View.VISIBLE);
    this.buttonStop.setVisibility(View.VISIBLE);
    this.buttonVideoAudio.setVisibility(View.GONE);
    this.buttonAudio.setVisibility(View.GONE);
    this.buttonVideo.setVisibility(View.GONE);
    this.buttonShareScreen.setVisibility(View.GONE);
  }

  //select streamToken id
  @Override
  public void onItemClick(View itemView, int position) {
    MainFragment.this.position = position;
    this.renderSurface.setVisibility(View.VISIBLE);
    this.streamIdByList = this.streamIdList.get(position);
    if (this.renderHolder == null) {
      this.renderHolder = renderSurface.getHolder();
    }
    this.isViewDetail = true;
    this.arcImage.setAngle(135);
    this.texViewVersion.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
    this.texViewVersion.setText(getStreamId(this.streamIdByList));
    this.viewVersion.setBackground(ContextCompat.getDrawable(activity, R.drawable.shape));
    animateView(this.imageLoad);
    this.imageLoad.setVisibility(View.VISIBLE);
    this.viewToolBar.setVisibility(View.VISIBLE);
    this.refreshLayout.setVisibility(View.GONE);
    this.qualityRenderer.setVisibility(View.VISIBLE);
    this.viewFull.setVisibility(View.GONE);
    this.getSubscribeToken(this.currentSessionId, this.streamIdByList, this.capability.getValue());
    this.texViewVersion.setOnTouchListener(null);
    this.activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
  }

  @Override
  public List<String> getListStreams() {
    return this.streamIdList;
  }

  @Override
  public boolean isThisPhone() {
    return this.isThisPhone;
  }

  //event click full view and change camera
  @Override
  public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
    switch (compoundButton.getId()) {
      case R.id.imageFull:
        if (isChecked) {
          this.isFullScreen = true;
          this.onFullScreen();
          if (this.viewLocal != null) {
            this.viewLocal.setPadding(0, 0, 0, 0);
          } else {
            this.viewLocalR.setPadding(0, 0, 0, 0);
          }

        } else {
          this.activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
          this.arcImage.setVisibility(View.VISIBLE);
          this.onChangeLayout();
          this.isFullScreen = false;
          if (this.viewLocal != null) {
            this.viewLocal.setPadding(20, 20, 20, 50);
          } else {
            this.viewLocalR.setPadding(20, 20, 20, 50);
          }
        }
        break;
    }
  }

  //Called by the system when the activity changes orientation
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
    this.arcImage.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.with_parabol);
    // Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      this.layoutLandscape(tabletSize);
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
      this.layoutPortrait(tabletSize);
    }
  }

  private void layoutPortrait(boolean tabletSize) {
    setLayout(this.activity,
              false,
              this.buttonAudio,
              this.buttonVideo,
              this.buttonVideoAudio,
              this.buttonShareScreen,
              this.imageAudio,
              this.imageVideo);
    if (tabletSize) {
      if (this.isFullScreen) {
        this.onFullScreen();
      } else {
        setLayoutTablet(this.viewList, true);
        setPreviewDimensionsTablet(this.activity, this.viewLocal);
      }
    } else {
      if (this.isFullScreen) {
        this.onFullScreen();
      } else {
        setPreviewDimensions(this.activity, this.viewLocal);
      }
    }
    this.viewAnimation.setPadding(dpToPx(this.activity, 4),
            dpToPx(this.activity, 4),
            dpToPx(this.activity, 4),
            dpToPx(this.activity, 4));
  }

  private void layoutLandscape(boolean tabletSize) {
    setLayout(this.activity,
              true,
              this.buttonAudio,
              this.buttonVideo,
              this.buttonVideoAudio,
              this.buttonShareScreen,
              this.imageAudio,
              this.imageVideo);
    if (tabletSize) {
      if (this.isFullScreen) {
        this.onFullScreen();
      } else {
        setLayoutTablet(this.viewList, true);
        setPreviewDimensionsTablet(this.activity, getViewLocal());
      }
    } else {
      if (this.isFullScreen) {
        this.onFullScreen();
      } else {
        setPreviewDimensions(this.activity, getViewLocal());
      }
    }
    this.viewAnimation.setPadding(dpToPx(this.activity, 4),
            dpToPx(this.activity, 0),
            dpToPx(this.activity, 4),
            dpToPx(this.activity, 4));
  }

  private ViewGroup getViewLocal() {
    return this.viewLocal != null ? this.viewLocal : this.viewLocalR;
  }
  @Override
  public void onRefresh() {
    this.presenter.listStreams(STREAM_LIST_LENGTH, this.phenixApplication.getServerAddress());
  }

  private void onChangeLayout() {
    if (this.viewLocal != null) {
      this.viewLocal.setContentPadding(dpToPx(this.activity, 2),
              dpToPx(this.activity, 2),
              dpToPx(this.activity, 2),
              dpToPx(this.activity, 2));
      this.viewLocal.setRadius(dpToPx(this.activity, 6));
    }
    boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
    if (tabletSize) {
      setPreviewDimensionsTablet(this.activity, getViewLocal());
    } else {
      setPreviewDimensions(this.activity, getViewLocal());
    }
  }

  @Override
  public void onAnimationUpdate(ValueAnimator valueAnimator) {
    arcImage.setProcess((Integer) valueAnimator.getAnimatedValue());
  }

  public void onStopRender() {
    if (this.renderPreView != null) {
      if (!this.renderPreView.isClosed()) {
        this.renderPreView.stop();
        Utilities.close(this.activity, this.renderPreView);
        this.renderPreView = null;
      }
    }
    if (this.previewLocalHolder != null && !this.isShare) {
      this.previewLocalHolder = null;
    }
  }

  @Override
  protected Subscription subscribeEvents() {
    return RxBus.getInstance().toObservable()
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext(new Action1<Object>() {
        @Override
        public void call(Object objectEvent) {
          if (objectEvent instanceof Events.OnRestartStream) {
            MainFragment.this.buttonVideoAudio.setVisibility(View.GONE);
            MainFragment.this.buttonStop.setEnabled(false);
          }

          if (objectEvent instanceof Events.OnStateDataQuality) {
            MainFragment.this.setViewQuality(((Events.OnStateDataQuality) objectEvent).isStateRender,
              ((Events.OnStateDataQuality) objectEvent).dataQualityStatus,
              ((Events.OnStateDataQuality) objectEvent).dataQualityReason);
          }
        }
      }).subscribe(RxBus.defaultSubscriber());
  }

  public void onChangeIconCamera(FacingMode facingMode) {
    if (facingMode == FacingMode.ENVIRONMENT) {
      this.toggleCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(this.activity, R.drawable.ic_camera_rear),
        null, null, null);
    } else {
      this.toggleCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(this.activity, R.drawable.ic_camera_front),
        null, null, null);
    }
  }

  //set view data quality publish and subscribe
  private void setViewQuality(final boolean isRenderViewQuality, final DataQualityStatus dataQualityStatus, final DataQualityReason dataQualityReason) {
    if (this.activity != null && isAdded()) {
      if (isRenderViewQuality) {
        this.viewSubscriptionDataQuality(dataQualityStatus, dataQualityReason);
      } else {
        this.viewPublicationDataQuality(dataQualityStatus, dataQualityReason);
      }
    }
  }

  private void viewSubscriptionDataQuality(DataQualityStatus dataQualityStatus, DataQualityReason dataQualityReason) {
    switch (dataQualityStatus) {
      case NO_DATA:
        if (dataQualityReason == NONE || dataQualityReason == DOWNLOAD_LIMITED || dataQualityReason == PUBLISHER_LIMITED
                || dataQualityReason == NETWORK_LIMITED) {
          this.qualityRenderer.setStatusShow(NO_DATA);
        }
        break;
      case ALL:
        if (dataQualityReason == NONE) {
          this.qualityRenderer.setStatusShow(AUDIO_ONLY_NONE);
        } else if (dataQualityReason == DOWNLOAD_LIMITED) {
          this.qualityRenderer.setStatusShow(ALL_UPLOAD);
        } else if (dataQualityReason == PUBLISHER_LIMITED || dataQualityReason == NETWORK_LIMITED) {
          this.qualityRenderer.setStatusShow(ALL_DOWNLOAD);
        }
        break;
      case AUDIO_ONLY:
        if (dataQualityReason == NONE) {
          this.qualityRenderer.setStatusShow(AUDIO_ONLY_NONE);
        } else if (dataQualityReason == DOWNLOAD_LIMITED || dataQualityReason == PUBLISHER_LIMITED
                || dataQualityReason == NETWORK_LIMITED) {
          this.qualityRenderer.setStatusShow(AUDIO_ONLY_LIMITED);
        }
        break;
    }
  }

  private void viewPublicationDataQuality(DataQualityStatus dataQualityStatus, DataQualityReason dataQualityReason) {
    switch (dataQualityStatus) {
      case NO_DATA:
        if (dataQualityReason == NONE || dataQualityReason == UPLOAD_LIMITED) {
          this.qualityPublisher.setStatusShow(NO_DATA);
        }
        break;
      case ALL:
        if (dataQualityReason == NONE) {
          this.qualityPublisher.setStatusShow(AUDIO_ONLY_NONE);
        } else if (dataQualityReason == UPLOAD_LIMITED) {
          this.qualityPublisher.setStatusShow(ALL_UPLOAD);
        } else if (dataQualityReason == NETWORK_LIMITED) {
          this.qualityPublisher.setStatusShow(ALL_DOWNLOAD);
        }
        break;
      case AUDIO_ONLY:
        if (dataQualityReason == NONE) {
          this.qualityPublisher.setStatusShow(AUDIO_ONLY_NONE);
        } else if (dataQualityReason == UPLOAD_LIMITED || dataQualityReason == NETWORK_LIMITED) {
          this.qualityPublisher.setStatusShow(AUDIO_ONLY_LIMITED);
        }
        break;
    }
  }

  // 6. Get streamToken token from REST admin API.
  private void getSubscribeToken(String sessionId, String streamId, String capability) {
    this.presenter.createStreamToken(this.phenixApplication.getServerAddress(),
            sessionId,
            streamId,
            new String[]{capability},
            new MainPresenter.IStreamer() {
        @Override
        public void hereIsYourStreamToken(String streamToken) {
          subscribeStream(streamToken);
        }

        @Override
        public void isError(int count) {
          if (MainFragment.this.handlerCall != null && count == NUM_HTTP_RETRIES) {
            ((MainActivity) MainFragment.this.activity).setGoneVersion();
            Message.obtain(MainFragment.this.handlerCall, STOP_PUBLISHER).sendToTarget();
          }
        }
      });
  }

  // 7. Subscribe to streamToken with SDK.
  private void subscribeStream(String subscribeStreamToken) {
    final MainActivity mainActivity = getMainActivity();
    if (mainActivity == null || mainActivity.getPCast() == null && isAdded()) {
      return;
    }
    mainActivity.getPCast().subscribe(subscribeStreamToken, new PCast.SubscribeCallback() {
      public void onEvent(PCast p, final RequestStatus status, final MediaStream media) {
        if (status == RequestStatus.OK) {
          MainFragment.this.mediaStream = media;
          MainFragment.this.mediaStream.setStreamEndedCallback(MainFragment.this);
          // Prepare the player with the streaming source.
          viewStream(MainFragment.this.mediaStream.createRenderer());
        } else {
          MainFragment.this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              showDialog("Notification", getString(R.string.stop_video_pub, status.name()), new DialogUtil.ActionDialog() {
                @Override
                public AppCompatActivity getContext() {
                  return mainActivity;
                }

                @Override
                public void buttonYes() {
                  stopRender(true);
                }

                @Override
                public void autoDismiss(final AlertDialog alertDialog) {
                  new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      if (getMainActivity() != null && isVisible()) {
                        alertDialog.dismiss();
                        stopRender(true);
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
  private void viewStream(final Renderer new_renderer) {
    this.isStreamEnded = false;
    this.renderer = new_renderer;
    if (this.handlerCall != null) {
      Message message = Message.obtain(this.handlerCall, VIEW_STREAM, this.renderer);
      message.sendToTarget();
    }
  }

  // Call back event for when the video publisher stopped
  @Override
  public void onEvent(MediaStream mediaStream, StreamEndedReason streamEndedReason, String s) {
    if (this.handlerCall != null) {
      Message message = Message.obtain(this.handlerCall, STOP_PUBLISHER, streamEndedReason);
      message.sendToTarget();
    }
  }

  private void onViewStream(Renderer renderer) {
    RendererStartStatus rendererStartStatus;
    if (renderer != null) {
      renderer.setDataQualityChangedCallback(MainFragment.this);
      rendererStartStatus = renderer.start(new AndroidVideoRenderSurface(renderHolder));
      if (rendererStartStatus != RendererStartStatus.OK) {
        showDialog(getResources().getString(R.string.render_error, rendererStartStatus.name()),
          getString(R.string.please_try), new DialogUtil.ActionDialog() {
            @Override
            public AppCompatActivity getContext() {
              return (AppCompatActivity) activity;
            }

            @Override
            public void buttonYes() {
              stopRender(true);
            }

            @Override
            public void autoDismiss(AlertDialog alertDialog) {}
          });
      } else {
        this.imageLoad.clearAnimation();
        if (this.imageLoad.getVisibility() == View.VISIBLE)
          this.imageLoad.setVisibility(View.GONE);
        this.viewToolBar.setVisibility(View.VISIBLE);
        this.qualityRenderer.setVisibility(View.VISIBLE);
        if (!this.isStopPreview && this.position == 0) {
          if (this.isAudio) {
            this.imageType.setImageDrawable(ContextCompat.getDrawable(this.activity, R.drawable.ic_audio_gray));
            this.imageType.setVisibility(View.VISIBLE);
          } else {
            if (this.phenixApplication.isShare()) {
              this.imageType.setImageDrawable(ContextCompat.getDrawable(this.activity, R.drawable.ic_share_src_gray));
              this.imageType.setVisibility(View.VISIBLE);
            } else {
              this.imageType.setVisibility(View.GONE);
            }
          }
        }
      }
    }
  }

  //Data Quality Status For Viewers
  @Override
  public void onEvent(Renderer renderer, DataQualityStatus dataQualityStatus, DataQualityReason dataQualityReason) {
    RxBus.getInstance().post(new Events.OnStateDataQuality(true, dataQualityStatus, dataQualityReason));
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message.what == VIEW_STREAM) {
      if (this.renderHolder == null) {
        this.renderHolder = this.renderSurface.getHolder();
      }
      this.onViewStream((Renderer) message.obj);
    }

    if (message.what == STOP_PUBLISHER) {
      this.onStopStream(message);
    }

    if (message.what == NULL_STREAM_TOKEN) {
      showDialog("Notification", getString(R.string.stop_video_pub, ""), new DialogUtil.ActionDialog() {
        @Override
        public AppCompatActivity getContext() {
          return (AppCompatActivity) MainFragment.this.activity;
        }

        @Override
        public void buttonYes() {
          MainFragment.this.stopRender(true);
        }

        @Override
        public void autoDismiss(AlertDialog alertDialog) {}
      });
    }
    return false;
  }

  //stop stream when publish stop
  private void onStopStream(Message message) {
    StreamEndedReason endedReason = ((StreamEndedReason) message.obj);
    if (endedReason == null) {
      this.onEventStopStreamDetail();
      return;
    }
    if ((endedReason == StreamEndedReason.ENDED || endedReason == StreamEndedReason.CUSTOM ||
      endedReason == StreamEndedReason.APP_BACKGROUND || endedReason == StreamEndedReason.FAILED)
      && !this.isStreamEnded && !this.isPause) {
      this.onEventStopStreamDetail();
    } else {
      this.isStreamEnded = false;
    }
  }

  private void onEventStopStreamDetail() {
    if (this.activity != null && isVisible()) {
      showDialog("Notification", getString(R.string.stop_video_pub, ""), new DialogUtil.ActionDialog() {
        @Override
        public AppCompatActivity getContext() {
          return (AppCompatActivity) activity;
        }

        @Override
        public void buttonYes() {
          MainFragment.this.isViewDetail = false;
          MainFragment.this.stopRender(true);
        }

        @Override
        public void autoDismiss(final AlertDialog alertDialog) {
          if (getMainActivity() != null && isVisible()) {
            new Handler().postDelayed(new Runnable() {
              @Override
              public void run() {
                if (getMainActivity() != null && isVisible()) {
                  MainFragment.this.stopRender(true);
                  MainFragment.this.isViewDetail = false;
                  alertDialog.dismiss();
                }
              }
            }, DELAY);
          }
        }
      });
    }
  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    switch (view.getId()) {
      case R.id.textViewVersion:
        this.getMainActivity().onShowSecretMenu(motionEvent);
        return true;
    }
    return false;
  }

  public void onBackButtonPressed() {
    if (this.isViewDetail) {
      this.isViewDetail = false;
      this.imageLoad.clearAnimation();
      if (this.imageLoad.getVisibility() == View.VISIBLE)
        this.imageLoad.setVisibility(View.GONE);
      if (this.isStopPreview) {
        this.arcImage.setTypeNomal(true);
        this.animator.pause();
        this.viewFull.setVisibility(View.GONE);
      }
      this.stopRender(true);
    } else {
      activity.finish();
    }
  }
}
