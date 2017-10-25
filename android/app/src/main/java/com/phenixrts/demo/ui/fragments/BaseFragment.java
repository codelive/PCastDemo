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

package com.phenixrts.demo.ui.fragments;

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
import android.widget.Toast;

import com.phenixrts.demo.R;
import com.phenixrts.demo.ui.activities.MainActivity;

import com.phenixrts.demo.utils.Utilities;
import java.util.List;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public abstract class BaseFragment extends Fragment {
  private static final String TAG = BaseFragment.class.getSimpleName();
  private Toast toast;
  private CompositeSubscription subscriptions;

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.addSubscription(subscribeEvents());
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
                                  Class<? extends Fragment> classNameOfFragmentToOpen,
                                  AnimationDirection style,
                                  Bundle args,
                                  int frameContent,
                                  String fragmentName) {
    FragmentTransaction transaction = manager.beginTransaction();
    String tag = classNameOfFragmentToOpen.getName();

    if (!isFragmentAdded(manager, tag)) {
      android.support.v4.app.Fragment fragment;
      try {
        fragment = classNameOfFragmentToOpen.newInstance();
        android.support.v4.app.Fragment currentFragment = getCurrentFragment(manager);
        if (currentFragment != null) {
          if (style == AnimationDirection.FROM_LEFT) {
            BaseFragment.slideFragment(context, currentFragment, transaction, R.anim.exit_to_right);
          } else if (style == AnimationDirection.FROM_RIGHT) {
            BaseFragment.slideFragment(context, currentFragment, transaction, R.anim.exit_to_left);
          }
          transaction.hide(currentFragment);
        }

        if (style == AnimationDirection.FROM_LEFT) {
          transaction.setCustomAnimations(R.anim.enter_from_left, 0);
        } else if (style == AnimationDirection.FROM_RIGHT) {
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
        Utilities.handleException((Activity) context, e);
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

  private static boolean isFragmentAdded(FragmentManager manager, String classNameOfFragment) {
    List<Fragment> fragmentList = manager.getFragments();
    if (fragmentList != null) {
      if (fragmentList.size() > 0) {
        for (android.support.v4.app.Fragment fragment : fragmentList) {
          if (fragment != null) {
            if (fragment.getClass().getName().equals(classNameOfFragment)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private static void showFragment(FragmentManager manager, String tag,
                                   FragmentTransaction transaction, AnimationDirection style) {
    List<Fragment> fragmentList = manager.getFragments();
    if (fragmentList != null)
      for (android.support.v4.app.Fragment fragment : fragmentList) {
        if (fragment != null) {
          if (fragment.getClass().getName().equals(tag)) {
            if (style == AnimationDirection.FROM_LEFT) {
              transaction.setCustomAnimations(R.anim.enter_from_left, 0);
            } else if (style == AnimationDirection.FROM_RIGHT) {
              transaction.setCustomAnimations(R.anim.enter_from_right, 0);
            }
            transaction.show(fragment);
            if (fragment instanceof BaseFragment)
              ((BaseFragment) fragment).reloadWhenOpened();
          } else {
            if (style == AnimationDirection.FROM_LEFT) {
              transaction.setCustomAnimations(0, R.anim.exit_to_right);
            } else if (style == AnimationDirection.FROM_RIGHT) {
              transaction.setCustomAnimations(0, R.anim.exit_to_left);
            }
            transaction.hide(fragment);
          }
        }
      }
    transaction.commitAllowingStateLoss();
  }

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
    this.bindEventHandlers(view);
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
    Log.d(TAG, "onDestroyView: [" + getClass().getSimpleName() + "]");
  }

  public MainActivity getMainActivity() {
    return ((MainActivity) getActivity());
  }

  public void showAToast(String str) {
    if (this.toast != null) {
      this.toast.cancel();
    }
    this.toast = Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT);
    this.toast.show();
  }

  public enum AnimationDirection {
    FROM_LEFT, FROM_RIGHT
  }
}
