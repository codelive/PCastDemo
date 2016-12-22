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

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.phenixp2p.demo.BuildConfig;
import com.phenixp2p.demo.R;
import com.phenixp2p.demo.RxBus;
import com.phenixp2p.demo.events.Events;
import com.phenixp2p.demo.model.StreamList;
import com.phenixp2p.demo.presenters.MainPresenter;
import com.phenixp2p.demo.presenters.inter.IMainPresenter;
import com.phenixp2p.demo.ui.adapter.StreamIdAdapter;
import com.phenixp2p.demo.ui.view.IMainView;
import com.phenixp2p.pcast.FacingMode;
import com.phenixp2p.pcast.Renderer;
import com.phenixp2p.pcast.RendererStartStatus;
import com.phenixp2p.pcast.android.AndroidVideoRenderSurface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static com.phenixp2p.demo.ui.fragments.ViewDetailStreamFragment.STREAM_ID;
import static com.phenixp2p.demo.utils.Utilities.animateView;
import static com.phenixp2p.demo.utils.Utilities.handleException;
import static com.phenixp2p.demo.utils.Utilities.setPreviewDimensions;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends BaseFragment implements View.OnClickListener, IMainView, StreamIdAdapter.OnItemClickListener, CompoundButton.OnCheckedChangeListener, SwipeRefreshLayout.OnRefreshListener {

  public static final String STREAM_TOKEN = "stream_token";
  public static final String SESSION_ID = "session_id";
  public static final String MY_STREAM_ID = "my_stream_id";
  public static final String MY_ID = "My_ID";
  public static final String IS_CHANGE_CAMEARA = "IS_CHANGE_CAMEARA";
  private SurfaceView surfaceView;
  private View viewDelay;
  private RecyclerView recyclerView;
  private ImageView buttonPlayToggle, record;
  private RelativeLayout viewVideo;
  private ToggleButton viewFull;
  private StreamIdAdapter mAdapter;
  private List<StreamList.Stream> streamIDList = new ArrayList<>();
  private IMainPresenter presenter;
  private Renderer mRenderPreView;
  private SurfaceHolder surfaceHolder;
  private ToggleButton togCamera;
  private SwipeRefreshLayout refreshLayout;
  private TextView tvNull;

  private String mSessionId;
  private String myStreamId;
  private boolean first = false;
  private boolean isThisPhone = true;
  private boolean isCameraFront = false;
  private boolean isCheckResume = false;
  private boolean isFullScreen = false;


  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    onStarts();
  }

  private void onStarts() {
    Bundle bundle = getArguments();
    if (bundle != null) {
      mSessionId = bundle.getString(SESSION_ID);
      myStreamId = bundle.getString(MY_STREAM_ID);
    }
    presenter = new MainPresenter(this);
  }

  @Override
  protected int getFragmentLayout() {
    return R.layout.fragment_main;
  }

  @Override
  protected void bindEventHandlers(View view) {
    surfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);
    recyclerView = (RecyclerView) view.findViewById(R.id.recry);
    buttonPlayToggle = (ImageView) view.findViewById(R.id.play);
    record = (ImageView) view.findViewById(R.id.record);
    viewDelay = view.findViewById(R.id.view_delay);
    viewFull = (ToggleButton) view.findViewById(R.id.imvFull);
    togCamera = (ToggleButton) view.findViewById(R.id.togCamera_);
    viewVideo = (RelativeLayout) view.findViewById(R.id.draggable_view);
    TextView tvVersion = (TextView) view.findViewById(R.id.tvVersion);
    tvNull = (TextView) view.findViewById(R.id.tvNull);
    refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipRefresh);
    refreshLayout.setOnRefreshListener(this);
    viewFull.setOnCheckedChangeListener(this);
    togCamera.setOnClickListener(this);
    buttonPlayToggle.setOnClickListener(this);
    record.setOnClickListener(this);
    mAdapter = new StreamIdAdapter(this);
    RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
    recyclerView.setLayoutManager(mLayoutManager);
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    surfaceHolder = surfaceView.getHolder();

    if (isCheckResume) {
      if (buttonPlayToggle.getVisibility() == View.GONE) {
        buttonPlayToggle.setVisibility(View.VISIBLE);
      }
    }
    String version = BuildConfig.VERSION_NAME.concat(" (" + String.valueOf(BuildConfig.VERSION_CODE) + ")");
    tvVersion.setText(getResources().getString(R.string.version, version));
    animateView(record);
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        presenter.startRendering();
      }
    }, 500);
  }

  @Override
  protected void reloadWhenOpened() {

  }

  public void callReload(String sessionId, String myStreamId) {
    if (first) {
      surfaceHolder = surfaceView.getHolder();
      viewDelay.setVisibility(View.GONE);
      recyclerView.setVisibility(View.VISIBLE);
      presenter.startRendering();
      mSessionId = sessionId;
      this.myStreamId = myStreamId;
      isThisPhone = true;
    }
  }

  private void onFullScreen() {
    getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
    RelativeLayout.LayoutParams rel_view = new RelativeLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    viewVideo.setLayoutParams(rel_view);
  }

  public void updateNewInfo(String sectionId, String streamId) {
    mSessionId = sectionId;
    myStreamId = streamId;
  }


  @Override
  public void onPause() {
    streamIDList.clear();
    surfaceHolder = null;
    first = true;
    viewDelay.setVisibility(View.VISIBLE);
    record.setVisibility(View.GONE);
    buttonPlayToggle.setVisibility(View.GONE);
    isCameraFront = false;
    if (! isFullScreen) {
      if (viewFull.isChecked()) {
        viewFull.setChecked(false);
      }
    }

    if (mRenderPreView != null) {
      mRenderPreView.stop();
      if (! mRenderPreView.isClosed()) {
        try {
          mRenderPreView.close();
          mRenderPreView = null;
        } catch (IOException e) {
          handleException(getActivity(), e);
        }
      }

    }
    super.onPause();
  }

  @Override
  public void onResume() {
    if (getActivity() != null && isAdded() && isVisible())
      getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    if (! isFullScreen) {
      setPreviewDimensions(getActivity(), viewVideo);
    }else {
      onFullScreen();
    }
    mAdapter.notifyDataSetChanged();
    togCamera.setVisibility(View.GONE);
    if (record.getVisibility() == View.VISIBLE) {
      buttonPlayToggle.setVisibility(View.GONE);
    }

    if (togCamera.isChecked())
      togCamera.setChecked(false);
    togCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_front_white_24dp), null, null, null);
    if (recyclerView.getVisibility() == View.GONE) {
      recyclerView.setVisibility(View.VISIBLE);
    }
    super.onResume();
  }

  @Override
  public void onDestroyView() {
    if (getActivity() != null)
      getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    presenter.onDestroy();
    try {
      surfaceView.getHolder().getSurface().release();
    } catch (Throwable ignored) {
    }
    surfaceView = null;
    super.onDestroyView();
  }

  // preview local user media
  @Override
  public void previewLocalUserMedia() {
    if (surfaceView != null && getMainActivity().getPublishMedia() != null && surfaceHolder != null) {
      mRenderPreView = getMainActivity().getPublishMedia().getMediaStream().createRenderer();
      if (mRenderPreView.start(new AndroidVideoRenderSurface(surfaceHolder)) == RendererStartStatus.OK) {
        record.setVisibility(View.VISIBLE);
        togCamera.setVisibility(View.VISIBLE);
        if (! record.isEnabled()) {
          record.setEnabled(true);
        }

      }
    }
    presenter.listStreams(100);
  }

  @Override
  public void getListStreams(StreamList streamlist) {
    if (streamlist.getStreams().size() > 0) {
      StreamList.Stream stream = new StreamList.Stream();
      stream.setStreamId(myStreamId);
      boolean isContain = streamlist.getStreams().contains(stream);
      if (isContain) {
        streamlist.getStreams().remove(stream);
      }
      if (streamlist.getStreams().size() > 1) {
        Collections.sort(streamlist.getStreams(), new Comparator<StreamList.Stream>() {
          @Override
          public int compare(StreamList.Stream stream, StreamList.Stream t1) {
            return stream.getStreamId().compareTo(t1.getStreamId());
          }
        });
      }
      if (isContain) {
        streamlist.getStreams().add(0, stream);
      }
      if (streamIDList.size() == 0) {
        streamIDList = streamlist.getStreams();

      } else {
        assert streamIDList != null;
        streamIDList.clear();
        streamIDList = streamlist.getStreams();
      }
      if (streamIDList.size() == 0) {
        tvNull.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
      } else {
        tvNull.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
      }
      if (mAdapter != null) {
        recyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        refreshLayout.setRefreshing(false);
      }
    }
  }

  @Override
  public void onError(String error) {
    Activity activity = getActivity();
    if (activity != null && isVisible() && isAdded()) {
      Toast.makeText(activity, "Error: " + error, Toast.LENGTH_SHORT).show();
    }
  }

  //event click stop or restart review
  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.play:
        isCheckResume = false;
        RxBus.getInstance().post(new Events.HideMyStreamId(false));
        RxBus.getInstance().post(new Events.OnRestartStream());
        break;
      case R.id.record:
        isCheckResume = true;
        togCamera.clearAnimation();
        togCamera.setVisibility(View.GONE);
        RxBus.getInstance().post(new Events.HideMyStreamId(true));
        RxBus.getInstance().post(new Events.OnStopStream());
        break;
      case R.id.togCamera_:
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
          isCameraFront = true;
          RxBus.getInstance().post(new Events.ChangeCamera(false));
        } else {
          isCameraFront = false;
          RxBus.getInstance().post(new Events.ChangeCamera(true));
        }
        break;
    }
  }

  //select streamToken id
  @Override
  public void onItemClick(View itemView, int position) {
    String streamIdByList = streamIDList.get(position).getStreamId();
    Bundle bundle = new Bundle();
    bundle.putString(SESSION_ID, mSessionId);
    bundle.putString(STREAM_ID, streamIdByList);
    bundle.putBoolean(IS_CHANGE_CAMEARA, isCameraFront);
    if (position == 0) {
      if (! myStreamId.equals(streamIdByList)) {
        bundle.putBoolean(MY_ID, false);
      } else {
        bundle.putBoolean(MY_ID, true);
      }
    } else {
      bundle.putBoolean(MY_ID, false);
    }
    openFragment(getActivity(), getFragmentManager(), ViewDetailStreamFragment.class, AnimStyle.FROM_RIGHT, bundle, R.id.content_content, "MainFragment");
  }

  @Override
  public List<StreamList.Stream> getListStreams() {
    return streamIDList;
  }

  @Override
  public boolean isThisPhone() {
    return isThisPhone;
  }

  //event click full view and change camera
  @Override
  public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
    switch (compoundButton.getId()) {
      case R.id.imvFull:
        if (isChecked) {
          isFullScreen = true;
          onFullScreen();
        } else {
          getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
          setPreviewDimensions(getActivity(), viewVideo);
          isFullScreen = false;
        }
        break;

    }
  }


  //event bus with Rx Java
  @Override
  protected Subscription subscribeEvents() {
    autoUnsubBus();
    return RxBus.getInstance().toObservable()
      .observeOn(AndroidSchedulers.mainThread())
      .doOnNext(new Action1<Object>() {
        @Override
        public void call(Object eventObject) {
          //remove stream id at stop preview local user media
          if (eventObject instanceof Events.HideMyStreamId) {
            if (((Events.HideMyStreamId) eventObject).isHide) {
              if (streamIDList.size() >= 1) {
                isThisPhone = false;
                streamIDList.remove(0);
                if (streamIDList.size() == 0) {
                  mAdapter.notifyItemChanged(0);
                } else {
                  mAdapter.notifyDataSetChanged();
                }
              }
            } else if (! ((Events.HideMyStreamId) eventObject).isHide) {
              isThisPhone = true;
            }

          }

          if (eventObject instanceof Events.OnRestartStream) {
            buttonPlayToggle.setVisibility(View.GONE);
            record.setEnabled(false);
            viewDelay.setVisibility(View.GONE);
          }
          if (eventObject instanceof Events.GetFacingMode) {
            if (((Events.GetFacingMode) eventObject).facingMode == FacingMode.ENVIRONMENT) {
              togCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_rear_white_24dp), null, null, null);
            } else {
              togCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_front_white_24dp), null, null, null);
            }
          }
        }
      }).subscribe(RxBus.defaultSubscriber());
  }

  //Called by the system when the activity changes orientation
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      setPreviewDimensions(getActivity(), viewVideo);
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
      setPreviewDimensions(getActivity(), viewVideo);
    }
  }

  @Override
  public void onRefresh() {
    presenter.listStreams(100);
  }

  public void onStopPreview() {
    setPreviewDimensions(getActivity(), viewVideo);
    if (viewFull.isChecked())
      viewFull.setChecked(false);
    buttonPlayToggle.setVisibility(View.VISIBLE);
    record.setVisibility(View.GONE);
    surfaceHolder = null;
    viewDelay.setVisibility(View.VISIBLE);
    first = true;
    if (mRenderPreView != null) {
      mRenderPreView.stop();
      if (! mRenderPreView.isClosed()) {
        try {
          mRenderPreView.close();
        } catch (IOException e) {
          handleException(getActivity(), e);
        }
      }
      mRenderPreView = null;
    }
  }
}
