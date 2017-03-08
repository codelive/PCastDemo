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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.transition.ChangeBounds;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.phenixp2p.demo.R;
import com.phenixp2p.demo.ui.activities.MainActivity;

import java.util.List;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import static com.phenixp2p.demo.utils.Utilities.handleException;

public abstract class BaseFragment extends Fragment {
  private static final String TAG = BaseFragment.class.getSimpleName();
  private CompositeSubscription subscriptions;

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    addSubscription(subscribeEvents());
  }

  protected Subscription subscribeEvents() {
    return null;
  }

  protected void addSubscription(Subscription subscription) {
    if (subscription == null) return;
    if (this.subscriptions == null) {
      this.subscriptions = new CompositeSubscription();
    }
    this.subscriptions.add(subscription);
  }

  public static void openFragment(Context context,
                                  FragmentManager manager,
                                  Class<? extends Fragment> clazz,
                                  AnimationStyle style,
                                  Bundle args,
                                  int frameContent,
                                  String fragmentName) {
    FragmentTransaction transaction = manager.beginTransaction();
    String tag = clazz.getName();

    if (!isFragmentAdded(manager, tag)) {
      android.support.v4.app.Fragment fragment;
      try {
        fragment = clazz.newInstance();
        android.support.v4.app.Fragment currentFragment = getCurrentFragment(manager);
        if (currentFragment != null) {
          if (style == AnimationStyle.FROM_LEFT) {
            BaseFragment.slideFragment(context, currentFragment, transaction, R.anim.exit_to_right);
          } else if (style == AnimationStyle.FROM_RIGHT) {
            BaseFragment.slideFragment(context, currentFragment, transaction, R.anim.exit_to_left);
          }
          transaction.hide(currentFragment);
        }

        if (style == AnimationStyle.FROM_LEFT) {
          transaction.setCustomAnimations(R.anim.enter_from_left, 0);
        } else if (style == AnimationStyle.FROM_RIGHT) {
          transaction.setCustomAnimations(R.anim.enter_from_right, 0);
        }
        transaction.replace(frameContent, fragment, tag);
        if (fragmentName != null) {
          transaction.addToBackStack(fragmentName);
        }

        if (args != null) {
          fragment.setArguments(args);
        }
        transaction.commitAllowingStateLoss();
      } catch (java.lang.InstantiationException | IllegalAccessException e) {
        handleException((Activity) context, e);
      }
    } else {
      showFragment(manager, tag, transaction, style);
    }
  }

  @SuppressLint("RtlHardcoded")
  private static void slideFragment(Context context,
                                    Fragment currentFragment,
                                    FragmentTransaction transaction,
                                    int exitAnimation) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      Slide slideTransition;
      try {
        slideTransition = new Slide(Gravity.START);
      } catch (java.lang.IllegalArgumentException e) {
        slideTransition = new Slide(Gravity.LEFT);
      }
      slideTransition.setDuration(context.getResources().getInteger(R.integer.anim_duration_medium));
      ChangeBounds changeBoundsTransition = new ChangeBounds();
      changeBoundsTransition.setDuration(context.getResources().getInteger(R.integer.anim_duration_medium));
      currentFragment.setEnterTransition(slideTransition);
      currentFragment.setAllowEnterTransitionOverlap(false);
      currentFragment.setAllowReturnTransitionOverlap(false);
      currentFragment.setSharedElementEnterTransition(changeBoundsTransition);
    } else transaction.setCustomAnimations(0, exitAnimation);
  }

  /**
   * Check a fragment if added
   * @param manager fragment manager
   * @param tag     name of fragment
   * @return true if added
   */
  private static Boolean isFragmentAdded(FragmentManager manager, String tag) {
    List<Fragment> fragmentList = manager.getFragments();
    if (fragmentList != null) {
      if (fragmentList.size() > 0) {
        for (android.support.v4.app.Fragment fragment : fragmentList) {
          if (fragment != null) {
            if (fragment.getClass().getName().equals(tag)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Show a fragment which added
   * @param manager     fragment manager
   * @param tag         name of fragment
   * @param transaction fragment transaction
   * @param style       transaction anim style
   */
  private static void showFragment(FragmentManager manager,
                                   String tag,
                                   FragmentTransaction transaction,
                                   AnimationStyle style) {
    List<Fragment> fragmentList = manager.getFragments();
    if (fragmentList != null)
      for (android.support.v4.app.Fragment fragment : fragmentList) {
        if (fragment != null) {
          if (fragment.getClass().getName().equals(tag)) {
            if (style == AnimationStyle.FROM_LEFT) {
              transaction.setCustomAnimations(R.anim.enter_from_left, 0);
            } else if (style == AnimationStyle.FROM_RIGHT) {
              transaction.setCustomAnimations(R.anim.enter_from_right, 0);
            }
            transaction.show(fragment);
            if (fragment instanceof BaseFragment)
              ((BaseFragment) fragment).reloadWhenOpened();
          } else {
            if (style == AnimationStyle.FROM_LEFT) {
              transaction.setCustomAnimations(0, R.anim.exit_to_right);
            } else if (style == AnimationStyle.FROM_RIGHT) {
              transaction.setCustomAnimations(0, R.anim.exit_to_left);
            }
            transaction.hide(fragment);
          }
        }
      }
    transaction.commitAllowingStateLoss();
  }

  /**
   * Get current showing fragment
   * @param manager fragment manager
   * @return current fragment
   */
  public static Fragment getCurrentFragment(FragmentManager manager) {
    List<Fragment> fragmentList = manager.getFragments();
    if (fragmentList != null) {
      if (fragmentList.size() > 0) {
        for (android.support.v4.app.Fragment fragment : fragmentList) {
          if (fragment != null && fragment.isVisible()) {
            return fragment;
          }
        }
      }
    }
    return null;
  }

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(getFragmentLayout(), container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bindEventHandlers(view);
  }

  protected abstract int getFragmentLayout();

  protected abstract void bindEventHandlers(View view);

  protected abstract void reloadWhenOpened();

  @Override
  public void onDestroyView() {
    if (this.subscriptions != null) {
      this.subscriptions.clear();
      this.subscriptions.unsubscribe();
    }
    super.onDestroyView();
    Log.d(TAG, "onDestroyView: " + getClass().getSimpleName());
  }

  public MainActivity getMainActivity() {
    return ((MainActivity) getActivity());
  }

  public enum AnimationStyle {
    FROM_LEFT, FROM_RIGHT
  }
}
