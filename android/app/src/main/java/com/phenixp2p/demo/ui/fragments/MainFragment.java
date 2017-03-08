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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;
import com.phenixp2p.demo.BuildConfig;
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
import com.phenixp2p.demo.utils.TokenUtils;
import com.phenixp2p.pcast.DataQualityReason;
import com.phenixp2p.pcast.DataQualityStatus;
import com.phenixp2p.pcast.FacingMode;
import com.phenixp2p.pcast.Renderer;
import com.phenixp2p.pcast.RendererStartStatus;
import com.phenixp2p.pcast.android.AndroidVideoRenderSurface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static com.phenixp2p.demo.Constants.ANIMATION_DURATION;
import static com.phenixp2p.demo.Constants.ANIMATOR_VALUE_RANGE;
import static com.phenixp2p.demo.Constants.ENDPOINT;
import static com.phenixp2p.demo.Constants.RENDER_DELAY;
import static com.phenixp2p.demo.Constants.SESSION_ID;
import static com.phenixp2p.demo.Constants.STREAM_ID;
import static com.phenixp2p.demo.Constants.VISIBILITY_DELAY;
import static com.phenixp2p.demo.utils.LayoutUtils.animateView;
import static com.phenixp2p.demo.utils.LayoutUtils.dpToPx;
import static com.phenixp2p.demo.utils.Utilities.handleException;
import static com.phenixp2p.demo.utils.LayoutUtils.setLayout;
import static com.phenixp2p.demo.utils.LayoutUtils.setLayoutTablet;
import static com.phenixp2p.demo.utils.LayoutUtils.setPreviewDimensions;
import static com.phenixp2p.demo.utils.LayoutUtils.setPreviewDimensionsTablet;
import static com.phenixp2p.pcast.DataQualityReason.NETWORK_LIMITED;
import static com.phenixp2p.pcast.DataQualityReason.NONE;
import static com.phenixp2p.pcast.DataQualityReason.UPLOAD_LIMITED;

/**
 * A simple {@link Fragment} subclass.
 */
public final class MainFragment extends BaseFragment implements
        View.OnClickListener,
        IMainView,
        StreamIdAdapter.OnItemClickListener,
        CompoundButton.OnCheckedChangeListener,
        SwipeRefreshLayout.OnRefreshListener,
        ValueAnimator.AnimatorUpdateListener {

  private static final String TAG = MainFragment.class.getSimpleName();
  private SurfaceView surfaceView;
  private View viewDelay;
  private RecyclerView recyclerView;
  private ImageView buttonStop, buttonAudio, buttonVideo, buttonShareScreen, imageAudio, imageVideo;
  private ViewGroup viewList, viewAnimation, buttonVideoAudio;
  private CardView viewVideo;
  private ToggleButton viewFull;
  private StreamIdAdapter adapter;
  private List<String> streamIdList = new ArrayList<>();
  private IMainPresenter presenter;
  private Renderer renderPreView;
  private SurfaceHolder surfaceHolder;
  private ToggleButton toggleCamera;
  private SwipeRefreshLayout refreshLayout;
  private ArcImage arcImage;
  private ValueAnimator animator;
  private TextView tvVersion;
  private QualityStatusView qualityPublisher;

  private String currentSessionId;
  private String currentStreamId;
  private boolean first = true;
  private boolean isThisPhone = true;
  private boolean isCameraFront = false;
  private boolean isCheckResume = false;
  private boolean isFullScreen = false;
  private boolean isStopPreview = false;
  private boolean isAudio = false;
  private boolean isShare = false;
  private boolean isLandscape = false;
  private Handler handler;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    onStarts();
  }

  private void onStarts() {
    Bundle bundle = getArguments();
    if (bundle != null) {
      this.currentSessionId = bundle.getString(SESSION_ID);
      this.currentStreamId = bundle.getString(STREAM_ID);
    }
    this.presenter = new MainPresenter(this);
  }

  @Override
  protected int getFragmentLayout() {
    return R.layout.fragment_main;
  }

  @Override
  protected void bindEventHandlers(View view) {
    this.surfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);
    this.recyclerView = (RecyclerView) view.findViewById(R.id.recry);
    this.buttonVideoAudio = (LinearLayout) view.findViewById(R.id.play);
    this.buttonStop = (ImageView) view.findViewById(R.id.record);
    this.buttonAudio = (ImageView) view.findViewById(R.id.audio);
    this.buttonVideo = (ImageView) view.findViewById(R.id.video);
    this.buttonShareScreen = (ImageView) view.findViewById(R.id.share);
    this.imageAudio = (ImageView) view.findViewById(R.id.imageAudio);
    this.imageVideo = (ImageView) view.findViewById(R.id.imageVideo);
    this.viewDelay = view.findViewById(R.id.viewDelay);
    this.viewFull = (ToggleButton) view.findViewById(R.id.imageFull);
    this.toggleCamera = (ToggleButton) view.findViewById(R.id.toggleCamera);
    this.viewVideo = (CardView) view.findViewById(R.id.draggableView);
    this.tvVersion = (TextView) view.findViewById(R.id.tvVersion);
    this.refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipRefresh);
    this.viewList = (RelativeLayout) view.findViewById(R.id.viewList);
    this.arcImage = (ArcImage) view.findViewById(R.id.arcImage);
    this.viewAnimation = (RelativeLayout) view.findViewById(R.id.view);
    this.qualityPublisher = (QualityStatusView) view.findViewById(R.id.qualityPublisher);
    this.animator = ValueAnimator.ofInt(0, ANIMATOR_VALUE_RANGE);
    this.animator.setDuration(ANIMATION_DURATION);
    this.animator.setRepeatCount(ValueAnimator.INFINITE);
    this.animator.addUpdateListener(this);
    this.animator.start();
    this.refreshLayout.setOnRefreshListener(this);
    this.viewFull.setOnCheckedChangeListener(this);
    this.toggleCamera.setOnClickListener(this);
    this.buttonVideoAudio.setOnClickListener(this);
    this.buttonStop.setOnClickListener(this);
    this.buttonShareScreen.setOnClickListener(this);
    this.buttonVideo.setOnClickListener(this);
    this.buttonAudio.setOnClickListener(this);
    this.adapter = new StreamIdAdapter(this);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
    this.recyclerView.setLayoutManager(layoutManager);
    this.recyclerView.setItemAnimator(new DefaultItemAnimator());
    this.surfaceHolder = this.surfaceView.getHolder();
    String version = BuildConfig.VERSION_NAME.concat(" (" + String.valueOf(BuildConfig.VERSION_CODE) + ")");
    this.tvVersion.setText(getResources().getString(R.string.version, version));
    animateView(this.buttonStop);
    this.handler = new Handler();
    this.handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        MainFragment.this.presenter.startRendering();
      }
    }, RENDER_DELAY);
    if (getMainActivity().getDataQualityReason() != null && getMainActivity().getDataQualityReason() != null) {
      setViewQuality(getMainActivity().getDataQualityStatus(), getMainActivity().getDataQualityReason());
    }
  }

  @Override
  protected void reloadWhenOpened() {}

  public void callReload(String currentSessionId, String currentStreamId) {
    this.first = false;
    this.isStopPreview = false;
    if (this.surfaceHolder == null) {
      this.surfaceHolder = this.surfaceView.getHolder();
    }
    this.viewDelay.setVisibility(View.GONE);
    this.currentSessionId = currentSessionId;
    this.currentStreamId = currentStreamId;
    this.presenter.startRendering();
    this.isThisPhone = true;
  }

  private synchronized void onFullScreen() {
    this.arcImage.setVisibility(View.GONE);
    getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
    RelativeLayout.LayoutParams rel_view = new RelativeLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    this.viewVideo.setLayoutParams(rel_view);
    this.viewVideo.setContentPadding(0, 0, 0, 0);
    this.viewVideo.setRadius(0);
  }

  @Override
  public void onPause() {
    onStopRender();
    final boolean dontShare = !((PhenixApplication) getActivity().getApplicationContext()).isShare();
    if (this.animator != null && dontShare) {
      this.animator.pause();
    }
    if (!this.isFullScreen) {
      if (this.viewFull.isChecked()) {
        this.viewFull.setChecked(false);
      }
    }

    if (dontShare) {
      this.streamIdList.clear();
      this.surfaceHolder = null;
      this.viewDelay.setVisibility(View.VISIBLE);
      this.buttonStop.setVisibility(View.GONE);
      this.buttonVideoAudio.setVisibility(View.GONE);
      this.isCameraFront = false;
      this.arcImage.setVisibility(View.GONE);
      if (this.recyclerView != null) {
        this.recyclerView.setVisibility(View.GONE);
        this.streamIdList.clear();
        this.viewVideo.setVisibility(View.GONE);
        this.viewFull.setVisibility(View.GONE);
        this.tvVersion.setVisibility(View.GONE);
      }
    } else {
      this.handler = null;
    }
    first = true;
    if (this.arcImage != null) {
      this.arcImage.clearAnimation();
      this.arcImage.setAnimation(null);
      this.arcImage.destroyDrawingCache();
    }
    super.onPause();
  }

  @Override
  public void onResume() {
    Activity activity = getActivity();
    final PhenixApplication appContext = (PhenixApplication) activity.getApplicationContext();
    if (activity != null && isAdded() && isVisible())
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    if (!this.isFullScreen) {
      onChangeLayout();
    } else {
      onFullScreen();
    }
    this.adapter.notifyDataSetChanged();
    this.toggleCamera.setVisibility(View.GONE);
    if (this.buttonStop.getVisibility() == View.VISIBLE) {
      this.buttonVideoAudio.setVisibility(View.GONE);
    }

    if (this.toggleCamera.isChecked())
      this.toggleCamera.setChecked(true);
    this.toggleCamera.setCompoundDrawablesWithIntrinsicBounds(
            ContextCompat.getDrawable(activity, R.drawable.ic_camera_rear),
            null, null, null);

    if (this.isStopPreview) {
      this.recyclerView.setVisibility(View.GONE);
      this.buttonStop.setVisibility(View.GONE);
      this.viewDelay.setVisibility(View.VISIBLE);
      this.buttonShareScreen.setVisibility(View.VISIBLE);
      this.buttonVideoAudio.setVisibility(View.VISIBLE);
      this.buttonVideo.setVisibility(View.VISIBLE);
      this.buttonAudio.setVisibility(View.VISIBLE);
      this.viewFull.setVisibility(View.GONE);
      this.animator.pause();
      this.qualityPublisher.setVisibility(View.GONE);
    } else {
      if (appContext.isStopPublish()) {
        this.qualityPublisher.setVisibility(View.GONE);
        this.isThisPhone = false;
        this.buttonStop.setVisibility(View.GONE);
        this.viewDelay.setVisibility(View.VISIBLE);
        this.buttonShareScreen.setVisibility(View.VISIBLE);
        this.buttonVideoAudio.setVisibility(View.VISIBLE);
        this.buttonVideo.setVisibility(View.VISIBLE);
        this.buttonAudio.setVisibility(View.VISIBLE);
        this.viewFull.setVisibility(View.GONE);
        animator.pause();
      } else {
        if (this.isShare) {
          if (handler == null) {
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
        this.buttonStop.setVisibility(View.VISIBLE);
        this.buttonShareScreen.setVisibility(View.GONE);
        this.buttonVideoAudio.setVisibility(View.GONE);
        this.buttonVideo.setVisibility(View.GONE);
        this.buttonAudio.setVisibility(View.GONE);
        this.viewDelay.setVisibility(View.GONE);
        this.animator.start();
      }
    }
    if (appContext.isStopPublish()
      && appContext.isLandscape()) {
      setLayout(activity,
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

  @Override
  public void onDestroyView() {
    if (this.handler != null) {
      this.handler = null;
    }
    Activity activity = getActivity();
    if (activity != null)
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    this.presenter.onDestroy();
    try {
      this.surfaceView.getHolder().getSurface().release();
    } catch (Exception e) {
      if (Fabric.isInitialized()) {
        Crashlytics.log(Log.ERROR, TAG, e.getMessage());
      }
    }
    this.surfaceView = null;
    super.onDestroyView();
  }

  // preview local user media
  @Override
  public void previewLocalUserMedia() {
    if (this.surfaceView != null && getMainActivity().getPublishMedia() != null && this.surfaceHolder != null) {
      this.recyclerView.setVisibility(View.VISIBLE);
      if (!this.isStopPreview) {
        if (!this.isAudio) {
          this.renderPreView = getMainActivity().getPublishMedia().getMediaStream().createRenderer();
          if (this.renderPreView.start(new AndroidVideoRenderSurface(this.surfaceHolder)) == RendererStartStatus.OK) {
            if (viewVideo.getVisibility() == View.GONE
                    && viewFull.getVisibility() == View.GONE
                    && tvVersion.getVisibility() == View.GONE) {
              this.viewVideo.setVisibility(View.VISIBLE);
              this.viewFull.setVisibility(View.VISIBLE);
              this.tvVersion.setVisibility(View.VISIBLE);
            }

            if (this.isStopPreview) {
              this.buttonStop.setVisibility(View.GONE);
              this.viewDelay.setVisibility(View.VISIBLE);
              this.toggleCamera.setVisibility(View.GONE);
            } else {
              this.buttonVideo.setVisibility(View.GONE);
              this.buttonAudio.setVisibility(View.GONE);
              this.buttonShareScreen.setVisibility(View.GONE);
              this.buttonStop.setVisibility(View.VISIBLE);
              this.viewDelay.setVisibility(View.GONE);
              this.buttonVideoAudio.setVisibility(View.GONE);
              this.animator.start();
              if (((PhenixApplication) getActivity().getApplicationContext()).isShare()) {
                this.toggleCamera.setVisibility(View.GONE);
              } else {
                this.toggleCamera.setVisibility(View.VISIBLE);
              }
            }
            this.arcImage.setVisibility(View.VISIBLE);
            if (!isCheckResume) {
              this.buttonVideoAudio.setVisibility(View.GONE);
            }

            if (!this.buttonStop.isEnabled()) {
              this.buttonStop.setEnabled(true);
            }
          }
        } else {
          this.buttonStop.setVisibility(View.VISIBLE);
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
    if (this.first) {
      this.presenter.listStreams(ENDPOINT);
    }
  }

  @Override
  public void getListStreams(List<String> streamId) {
    ListStreamResponse.Stream stream = new ListStreamResponse.Stream();
    stream.setStreamId(this.currentStreamId);
    boolean containsCurrentStream = streamId.contains(stream.getStreamId());
    if (containsCurrentStream) {
      streamId.remove(stream.getStreamId());
    }

    Collections.sort(streamId, new Comparator<String>() {
      @Override
      public int compare(String stream, String t1) {
        return stream.compareTo(t1);
      }
    });

    if (containsCurrentStream) {
      streamId.add(0, stream.getStreamId());
    }

    this.streamIdList = streamId;

    if (this.adapter != null) {
      this.recyclerView.setAdapter(adapter);
      this.adapter.notifyDataSetChanged();
      this.refreshLayout.setRefreshing(false);
    }
  }

  @Override
  public void onError(final String error) {
    final Activity activity = getActivity();
    if (activity != null && isVisible() && isAdded()) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(activity, "Error: " + error, Toast.LENGTH_SHORT).show();
        }
      });
    }
  }

  private void toastPublishStopFail(Activity activity) {
    String message = activity.getString(R.string.publishStopFail);
    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
  }

  //event click stop or restart review
  @Override
  public synchronized void onClick(View view) {
    Activity activity = getActivity();
    switch (view.getId()) {
      case R.id.play:
        if (((MainActivity) activity).isStopPublish()) {
          this.isAudio = false;
          this.buttonAudio.setVisibility(View.GONE);
          this.buttonVideo.setVisibility(View.GONE);
          this.buttonShareScreen.setVisibility(View.GONE);
          onResumePublish();
          RxBus.getInstance().post(new Events.OnRestartStream());
        } else {
          toastPublishStopFail(activity);
        }
        break;
      case R.id.record:
        RxBus.getInstance().post(new Events.OnStopStream());
        ((PhenixApplication) activity.getApplicationContext()).setStopPublish(true);
        this.animator.pause();
        this.qualityPublisher.setVisibility(View.GONE);
        onStopRender();
        onChangeLayout();
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
        break;
      case R.id.toggleCamera:
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
          this.isCameraFront = true;
          RxBus.getInstance().post(new Events.ChangeCamera(true));
        } else {
          this.isCameraFront = false;
          RxBus.getInstance().post(new Events.ChangeCamera(false));
        }
        break;
      case R.id.audio:
        if (((MainActivity) activity).isStopPublish()) {
          this.isAudio = true;
          ((MainActivity) activity).onlyVideoOrAudio(false);
          onResumePublish();
          this.toggleCamera.setVisibility(View.VISIBLE);
        } else {
          toastPublishStopFail(activity);
        }
        break;
      case R.id.video:
        if (((MainActivity) activity).isStopPublish()) {
          this.isAudio = false;
          ((MainActivity) activity).onlyVideoOrAudio(true);
          onResumePublish();
        } else {
          toastPublishStopFail(activity);
        }
        break;
      case R.id.share:
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
          if (((MainActivity) activity).isStopPublish()) {
            TokenUtils.saveSessionIdIntoLocal(activity, this.currentSessionId);
            TokenUtils.saveStreamIdIntoLocal(activity, this.currentStreamId);
            this.isAudio = false;
            this.isShare = true;
            onResumePublish();
            ((MainActivity) activity).onStartShareScreen();
            CaptureHelper.fireScreenCaptureIntent(activity);
          } else {
            toastPublishStopFail(activity);
          }
        } else {
          Toast.makeText(activity, "This app requires Android 5.0 or later", Toast.LENGTH_SHORT).show();
        }
        break;
    }
  }

  private void onResumePublish() {
    this.qualityPublisher.setVisibility(View.VISIBLE);
    ((PhenixApplication) getActivity().getApplicationContext()).setStopPublish(false);
    this.animator.start();
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
    String streamIdByList = this.streamIdList.get(position);
    Bundle bundle = new Bundle();
    bundle.putString(SESSION_ID, this.currentSessionId);
    bundle.putString(Constants.STREAM_ID_FROM_LIST, streamIdByList);
    bundle.putBoolean(Constants.IS_CAMERA_FRONT, this.isCameraFront);
    bundle.putBoolean(Constants.IS_PUBLISH_STOPPED, this.isStopPreview);
    bundle.putBoolean(Constants.IS_LANDSCAPE, this.isLandscape);
    bundle.putBoolean(Constants.IS_AUDIO, this.isAudio);
    AnimationStyle animationStyle = null;
    openFragment(getActivity(),
            getFragmentManager(),
            ViewDetailStreamFragment.class,
            animationStyle,
            bundle,
            R.id.content,
            "MainFragment");
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
          onFullScreen();
          this.viewVideo.setPadding(0, 0, 0, 0);
        } else {
          getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
          this.arcImage.setVisibility(View.VISIBLE);
          onChangeLayout();
          this.isFullScreen = false;
          this.viewVideo.setPadding(20, 20, 20, 50);
        }
        break;
    }
  }

  private void setLayoutItems(boolean isLandscape) {

  }

  //Called by the system when the activity changes orientation
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    Activity activity = getActivity();
    super.onConfigurationChanged(newConfig);
    boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
    this.arcImage.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.with_parabol);
    // Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      setLayout(activity,
              true,
              this.buttonAudio,
              this.buttonVideo,
              this.buttonVideoAudio,
              this.buttonShareScreen,
              this.imageAudio,
              this.imageVideo);
      isLandscape = true;
      if (tabletSize) {
        if (this.isFullScreen) {
          onFullScreen();
        } else {
          setLayoutTablet(this.viewList, true);
          setPreviewDimensionsTablet(activity, this.viewVideo);
        }
      } else {
        if (this.isFullScreen) {
          onFullScreen();
        } else {
          setPreviewDimensions(activity, this.viewVideo);
        }
      }
      viewAnimation.setPadding(
              dpToPx(activity, 4),
              dpToPx(activity, 0),
              dpToPx(activity, 4),
              dpToPx(activity, 4));
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
      setLayout(activity,
              false,
              this.buttonAudio,
              this.buttonVideo,
              this.buttonVideoAudio,
              this.buttonShareScreen,
              this.imageAudio,
              this.imageVideo);
      isLandscape = false;
      if (tabletSize) {
        if (this.isFullScreen) {
          onFullScreen();
        } else {
          setLayoutTablet(this.viewList, true);
          setPreviewDimensionsTablet(activity, this.viewVideo);
        }
      } else {
        if (this.isFullScreen) {
          onFullScreen();
        } else {
          setPreviewDimensions(activity, this.viewVideo);
        }
      }
      viewAnimation.setPadding(
              dpToPx(activity, 4),
              dpToPx(activity, 4),
              dpToPx(activity, 4),
              dpToPx(activity, 4));
    }
  }

  @Override
  public void onRefresh() {
    this.presenter.listStreams(ENDPOINT);
  }

  private void onChangeLayout() {
    Activity activity = getActivity();
    this.viewVideo.setContentPadding(
            dpToPx(activity, 2),
            dpToPx(activity, 2),
            dpToPx(activity, 2),
            dpToPx(activity, 2));
    this.viewVideo.setRadius(dpToPx(activity, 6));
    boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
    if (tabletSize) {
      setPreviewDimensionsTablet(activity, this.viewVideo);
    } else {
      setPreviewDimensions(activity, this.viewVideo);
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
        try {
          this.renderPreView.close();
          this.renderPreView = null;
        } catch (IOException e) {
          handleException(getActivity(), e);
        }
      }
    }
    if (this.surfaceHolder != null && !this.isShare) {
      this.surfaceHolder = null;
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
            setViewQuality(((Events.OnStateDataQuality) objectEvent).dataQualityStatus,
              ((Events.OnStateDataQuality) objectEvent).dataQualityReason);
          }
        }
      }).subscribe(RxBus.defaultSubscriber());
  }

  public void onChangeIconCamera(FacingMode facingMode) {
    Activity activity = getActivity();
    if (facingMode == FacingMode.ENVIRONMENT) {
      this.toggleCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(activity,
              R.drawable.ic_camera_rear),
              null, null, null);
    } else {
      this.toggleCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(activity,
              R.drawable.ic_camera_front),
              null, null, null);
    }
  }

  //set view data quality publish and subscribe
  private void setViewQuality(final DataQualityStatus dataQualityStatus,
                              final DataQualityReason dataQualityReason) {
    final Activity activity = getActivity();
    if (activity != null && isAdded()) {
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
