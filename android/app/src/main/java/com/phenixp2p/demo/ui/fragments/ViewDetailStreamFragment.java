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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.phenixp2p.demo.R;
import com.phenixp2p.demo.RxBus;
import com.phenixp2p.demo.events.Events;
import com.phenixp2p.demo.presenters.MainActivityPresenter;
import com.phenixp2p.demo.presenters.inter.IMainActivityPresenter;
import com.phenixp2p.demo.ui.view.IDetailStreamView;
import com.phenixp2p.demo.utils.Utilities;
import com.phenixp2p.pcast.MediaStream;
import com.phenixp2p.pcast.PCast;
import com.phenixp2p.pcast.Renderer;
import com.phenixp2p.pcast.RendererStartStatus;
import com.phenixp2p.pcast.RequestStatus;
import com.phenixp2p.pcast.StreamEndedReason;
import com.phenixp2p.pcast.android.AndroidVideoRenderSurface;

import java.io.IOException;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static com.phenixp2p.demo.ui.fragments.MainFragment.IS_CHANGE_CAMEARA;
import static com.phenixp2p.demo.utils.Utilities.animateView;
import static com.phenixp2p.demo.utils.Utilities.getStreamId;
import static com.phenixp2p.demo.utils.Utilities.handleException;
import static com.phenixp2p.demo.utils.Utilities.isEquals;
import static com.phenixp2p.demo.utils.Utilities.setPreviewDimensions;
import static com.phenixp2p.demo.utils.Utilities.showDialog;
import static com.phenixp2p.demo.utils.Utilities.showToast;

public class ViewDetailStreamFragment extends BaseFragment implements MediaStream.StreamEndedCallback, IDetailStreamView, CompoundButton.OnCheckedChangeListener, RadioGroup.OnCheckedChangeListener, View.OnClickListener {
  public static final String STREAM_ID = "stream_id";
  public static final String SESSION_ID = "session_id";
  public static final String STREAMING = "streaming";
  private static final String REAL_TIME = "real-time";
  private static final String BROADCAST = "broadcast";

  private String mStreamId;
  private String mSessionId;
  private boolean first = false;
  private String mCapability;
  private boolean isStreamEnded = false;
  private MediaStream mMediaStream;
  private IMainActivityPresenter presenter;

  private TextView tvStreamId;
  private ImageView imvLoad;
  private SurfaceView renderSurface, previewLocal;
  private RelativeLayout viewPreView, viewLocal;
  private RadioGroup radioGroup;
  private RadioButton filter_real_time, filter_broadcast, filter_live;
  private FrameLayout rootView;
  private PopupWindow popupWindow;
  private RelativeLayout goBack;
  private boolean isStop = true;
  private boolean isCamaraFront;
  private SurfaceHolder renderHolder, previewLocalHolder;
  private ToggleButton togCamera;
  private View viewStop;
  private Renderer mRenderer, mRenderPreView;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle args = getArguments();
    if (args != null) {
      mStreamId = args.getString(STREAM_ID);
      mSessionId = args.getString(SESSION_ID);
      isCamaraFront = args.getBoolean(IS_CHANGE_CAMEARA);
    }
    presenter = new MainActivityPresenter(this);
  }


  @Override
  protected int getFragmentLayout() {
    return R.layout.fragment_view_stream;
  }

  @Override
  protected void bindEventHandlers(View view) {
    setHasOptionsMenu(true);
    renderSurface = (SurfaceView) view.findViewById(R.id.surfaceView);
    previewLocal = (SurfaceView) view.findViewById(R.id.previewLocal);
    tvStreamId = (TextView) view.findViewById(R.id.tvStreamId);
    imvLoad = (ImageView) view.findViewById(R.id.imvLoad);
    Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
    viewPreView = (RelativeLayout) view.findViewById(R.id.draggable_view);
    viewLocal = (RelativeLayout) view.findViewById(R.id.viewLocal);
    togCamera = (ToggleButton) view.findViewById(R.id.togCamera);
    goBack = (RelativeLayout) view.findViewById(R.id.back);
    viewStop = view.findViewById(R.id.viewStop);
    togCamera.setChecked(isCamaraFront);

    if (isCamaraFront) {
      togCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_rear_white_24dp), null, null, null);
    } else {
      togCamera.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_front_white_24dp), null, null, null);
    }
    togCamera.setOnCheckedChangeListener(this);
    renderSurface.setOnClickListener(this);
    goBack.setOnClickListener(this);
    AppCompatActivity activity = (AppCompatActivity) getActivity();
    activity.setSupportActionBar(toolbar);
    if (activity.getSupportActionBar() != null) {
      activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
      activity.getSupportActionBar().setTitle("");
    }
    setPreviewDimensions(getActivity(), viewPreView);
    setPreviewDimensions(getActivity(), viewLocal);
    animateView(imvLoad);
    LayoutInflater layoutInflater = (LayoutInflater) getActivity()
      .getSystemService(LAYOUT_INFLATER_SERVICE);
    View popupView = layoutInflater.inflate(R.layout.popup_select, null);
    popupWindow = new PopupWindow(popupView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    radioGroup = (RadioGroup) popupView.findViewById(R.id.radioGroup);
    filter_broadcast = (RadioButton) popupView.findViewById(R.id.filter_broadcast);
    filter_real_time = (RadioButton) popupView.findViewById(R.id.filter_real_time);
    filter_live = (RadioButton) popupView.findViewById(R.id.filter_live);
    radioGroup.setOnCheckedChangeListener(this);
    renderHolder = renderSurface.getHolder();
    previewLocalHolder = previewLocal.getHolder();
    tvStreamId.setText(getStreamId(mStreamId));
    if (mCapability == null) {
      mCapability = REAL_TIME;
    }
    getSubscribeToken(mSessionId, mStreamId, mCapability);
  }

  @Override
  protected void reloadWhenOpened() {

  }

  @Override
  public void onPause() {
    first = true;
    if (isStop) {
      stopRender();
    }
    if (popupWindow != null && popupWindow.isShowing()) {
      popupWindow.dismiss();
    }
    goBack.setVisibility(View.GONE);
    togCamera.setVisibility(View.GONE);
    if (togCamera.isChecked()) {
      togCamera.setChecked(false);
    }
    imvLoad.clearAnimation();
    setHasOptionsMenu(false);
    super.onPause();

  }

  // Stop render stream, and render preview local
  private void stopRender() {
    if (mMediaStream != null && ! mMediaStream.isClosed()) {
      mMediaStream.stop();
      try {
        mMediaStream.close();
      } catch (IOException e) {
        handleException(getActivity(), e);
      }
      mMediaStream = null;
    }
    if (mRenderer != null && ! mRenderer.isClosed()) {
      mRenderer.stop();
      try {
        mRenderer.close();
      } catch (IOException e) {
        handleException(getActivity(), e);
      }
      mRenderer = null;
    }
    if (mRenderPreView != null && ! mRenderPreView.isClosed()) {
      mRenderPreView.stop();
      try {
        mRenderPreView.close();
      } catch (IOException e) {
        handleException(getActivity(), e);
      }
      mRenderPreView = null;
    }
    renderHolder = null;
    previewLocalHolder = null;
  }

  public void callReload(String newSectionId, String streamId) {
    if (first) {
      renderHolder = renderSurface.getHolder();
      previewLocalHolder = previewLocal.getHolder();
      if (mCapability == null) {
        mCapability = REAL_TIME;
      }
      getSubscribeToken(mSessionId, mStreamId, mCapability);
      tvStreamId.setText(getStreamId(streamId));
    }
  }

  @Override
  public void onResume() {
    if (getActivity() != null) {
      getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
      getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    super.onResume();
  }

  @Override
  public void onDestroyView() {
    presenter.onDestroy();
    Activity activity = getActivity();
    if (activity != null) {
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    first = false;
    try {
      renderSurface.getHolder().getSurface().release();
      previewLocal.getHolder().getSurface().release();
    } catch (Throwable ignored) {
    }
    renderSurface = null;
    previewLocal = null;
    super.onDestroyView();
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    final MenuItem menuSelect = menu.findItem(R.id.action_filter_stream);
    rootView = (FrameLayout) menuSelect.getActionView();

    rootView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onOptionsItemSelected(menuSelect);
      }
    });

    super.onPrepareOptionsMenu(menu);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_select_type_stream, menu);
  }

  // This hook is called whenever an item in your options menu is selected Order: real-time, broadcast, live
  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_filter_stream:
        popupWindow.showAsDropDown(rootView, 50, -80);
        break;
      case android.R.id.home:
        getActivity().onBackPressed();
        break;

    }
    return super.onOptionsItemSelected(item);
  }


  // 6. Get streamToken token from REST admin API.
  private void getSubscribeToken(String sessionId, String streamId, String capability) {
    presenter.createStreamToken(sessionId, streamId, capability, new MainActivityPresenter.Streamer() {
      @Override
      public void hereIsYourStreamToken(String streamToken) {
        subscribeStream(streamToken);
      }
    });
  }

  // 7. Subscribe to streamToken with SDK.
  private void subscribeStream(String subscribeStreamToken) {
    if (getMainActivity().getPCast() == null) {
      return;
    }
    getMainActivity().getPCast().subscribe(subscribeStreamToken, new PCast.SubscribeCallback() {
      public void onEvent(PCast p, final RequestStatus status, final MediaStream media) {
        if (status == RequestStatus.OK) {
          mMediaStream = media;
          mMediaStream.setStreamEndedCallback(ViewDetailStreamFragment.this);
          // Prepare the player with the streaming source.
          viewStream(mMediaStream.createRenderer());
        } else {
          getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              showDialog(status.name(), getString(R.string.stop_video_pub), new Utilities.ActionDialog() {
                @Override
                public Activity getActivity() {
                  return ViewDetailStreamFragment.this.getActivity();
                }

                @Override
                public void buttonYes() {
                  getMainActivity().getSupportFragmentManager().popBackStack();
                }
              });
            }
          });

        }
      }
    });
  }

  // 8. View streamToken.
  private void viewStream(final Renderer renderer) {
    mRenderer = renderer;
    getMainActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        RendererStartStatus rendererStartStatus;
        if (renderHolder != null) {
          rendererStartStatus = renderer.start(new AndroidVideoRenderSurface(renderHolder));
          if (rendererStartStatus != RendererStartStatus.OK) {
            showDialog(getResources().getString(R.string.render_error, rendererStartStatus.name()),
              getString(R.string.please_try), new Utilities.ActionDialog() {
              @Override
              public Activity getActivity() {
                return ViewDetailStreamFragment.this.getActivity();
              }

              @Override
                public void buttonYes() {
                  getMainActivity().getSupportFragmentManager().popBackStack();
                }
              });

          } else {
            imvLoad.clearAnimation();
            if (imvLoad.getVisibility() == View.VISIBLE)
              imvLoad.setVisibility(View.GONE);
            setHasOptionsMenu(true);
            goBack.setVisibility(View.VISIBLE);
          }
        }
        if (previewLocalHolder != null) {
          if (getMainActivity().getPublishMedia() == null) {
            viewStop.setVisibility(View.VISIBLE);
            return;
          }
          mRenderPreView = getMainActivity().getPublishMedia().getMediaStream().createRenderer();
          rendererStartStatus = mRenderPreView.start(new AndroidVideoRenderSurface(previewLocalHolder));
          if (rendererStartStatus != RendererStartStatus.OK) {
            showDialog(getResources().getString(R.string.render_error, rendererStartStatus.name()),
              getString(R.string.please_try), new Utilities.ActionDialog() {
              @Override
              public Activity getActivity() {
                return ViewDetailStreamFragment.this.getActivity();
              }

              @Override
                public void buttonYes() {
                  getMainActivity().getSupportFragmentManager().popBackStack();
                }
              });
          } else {
            setHasOptionsMenu(true);
            goBack.setVisibility(View.VISIBLE);
            mRenderPreView.muteAudio();
            togCamera.setVisibility(View.VISIBLE);
            viewStop.setVisibility(View.GONE);
          }
        }

      }
    });
  }

  //Called by the system when the activity changes orientation
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      setPreviewDimensions(getActivity(), viewPreView);
      setPreviewDimensions(getActivity(), viewLocal);
      hideSystemUI();
      renderSurface.setOnClickListener(new View.OnClickListener() {
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
      setPreviewDimensions(getActivity(), viewPreView);
      setPreviewDimensions(getActivity(), viewLocal);
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

  // Call back event for when the video publisher stopped
  @Override
  public void onEvent(MediaStream mediaStream, final StreamEndedReason streamEndedReason, final String s) {
    isStop = false;
    final Activity activity = getActivity();
    if (activity != null && isAdded() && isVisible()) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if ((streamEndedReason == StreamEndedReason.ENDED || streamEndedReason == StreamEndedReason.CUSTOM) && ! isStreamEnded) {
            showDialog("Notification", getString(R.string.stop_video_pub), new Utilities.ActionDialog() {
              @Override
              public Activity getActivity() {
                return ViewDetailStreamFragment.this.getActivity();
              }

              @Override
              public void buttonYes() {
                stopRender();
                getMainActivity().getSupportFragmentManager().popBackStack();
              }
            });

            tvStreamId.setVisibility(View.GONE);
          } else {
            isStreamEnded = false;
          }
        }
      });
    }
  }

  // View error on get createStreamToken token from REST admin API
  @Override
  public void onError(String error) {
    Activity activity = getActivity();
    if (activity != null && isVisible() && isAdded()) {
      if (error.equals("HTTP 410 ")) {
        showDialog("Notification", getString(R.string.stop_video_pub), new Utilities.ActionDialog() {
          @Override
          public Activity getActivity() {
            return ViewDetailStreamFragment.this.getActivity();
          }

          @Override
          public void buttonYes() {
            stopRender();
            getMainActivity().getSupportFragmentManager().popBackStack();
          }
        });
      }else {
        showToast(activity, error);
      }

    }
  }

  @Override
  public void onCheckedChanged(final CompoundButton compoundButton, boolean isChecked) {
    switch (compoundButton.getId()) {
      case R.id.togCamera:
        if (isChecked) {
          compoundButton.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_rear_white_24dp), null, null, null);
          RxBus.getInstance().post(new Events.ChangeCamera(false));
        } else {
          compoundButton.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_camera_front_white_24dp), null, null, null);
          RxBus.getInstance().post(new Events.ChangeCamera(true));
        }
        break;
    }

  }

  @Override
  public void onCheckedChanged(RadioGroup radioGroup, final int idChecked) {
    String capability = null;
    switch (idChecked) {
      case R.id.filter_real_time:
        capability = REAL_TIME;
        break;
      case R.id.filter_live:
        capability = STREAMING;
        break;
      case R.id.filter_broadcast:
        capability = BROADCAST;
        break;
    }
    if (capability != null) {
      if (! isEquals(capability, mCapability)) {
        stopRender();
        isStreamEnded = true;
        mCapability = capability;
        renderHolder = renderSurface.getHolder();
        previewLocalHolder = previewLocal.getHolder();
        getSubscribeToken(mSessionId, mStreamId, mCapability);
        if (popupWindow != null) {
          popupWindow.dismiss();
        }
      }
      switch (mCapability) {
        case REAL_TIME:
          filter_real_time.setChecked(true);
          filter_live.setChecked(false);
          filter_broadcast.setChecked(false);
          break;
        case STREAMING:
          filter_real_time.setChecked(false);
          filter_live.setChecked(true);
          filter_broadcast.setChecked(false);
          break;
        case BROADCAST:
          filter_real_time.setChecked(false);
          filter_live.setChecked(false);
          filter_broadcast.setChecked(true);
          break;
      }
    }

  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.surfaceView:
        if (popupWindow != null && popupWindow.isShowing()) {
          popupWindow.dismiss();
        }
        break;
      case R.id.back:
        stopRender();
        getMainActivity().getSupportFragmentManager().popBackStack();
        break;
    }
  }

}
