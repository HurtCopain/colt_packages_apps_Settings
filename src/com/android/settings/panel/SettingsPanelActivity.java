/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.panel;

import static com.android.settingslib.media.MediaOutputConstants.EXTRA_PACKAGE_NAME;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;

/**
 * Dialog Activity to host Settings Slices.
 */
public class SettingsPanelActivity extends FragmentActivity {

    private static final String TAG = "SettingsPanelActivity";

    @VisibleForTesting
    final Bundle mBundle = new Bundle();
    @VisibleForTesting
    boolean mForceCreation = false;
    @VisibleForTesting
    PanelFragment mPanelFragment;

    /**
     * Key specifying which Panel the app is requesting.
     */
    public static final String KEY_PANEL_TYPE_ARGUMENT = "PANEL_TYPE_ARGUMENT";

    /**
     * Key specifying the package which requested the Panel.
     */
    public static final String KEY_CALLING_PACKAGE_NAME = "PANEL_CALLING_PACKAGE_NAME";

    /**
     * Key specifying the package name for which the
     */
    public static final String KEY_MEDIA_PACKAGE_NAME = "PANEL_MEDIA_PACKAGE_NAME";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getApplicationContext().getTheme().rebase();
        createOrUpdatePanel(true /* shouldForceCreation */);
        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        createOrUpdatePanel(mForceCreation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mForceCreation = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mPanelFragment != null && !mPanelFragment.isPanelCreating()) {
            mForceCreation = true;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mForceCreation = true;
    }

    private void createOrUpdatePanel(boolean shouldForceCreation) {
        final Intent callingIntent = getIntent();
        if (callingIntent == null) {
            Log.e(TAG, "Null intent, closing Panel Activity");
            finish();
            return;
        }

        final String action = callingIntent.getAction();
        // We will use it once media output switch panel support remote device.
        final String mediaPackageName = callingIntent.getStringExtra(EXTRA_PACKAGE_NAME);
        mBundle.putString(KEY_PANEL_TYPE_ARGUMENT, action);
        mBundle.putString(KEY_CALLING_PACKAGE_NAME, getCallingPackage());
        mBundle.putString(KEY_MEDIA_PACKAGE_NAME, mediaPackageName);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentById(R.id.main_content);

        // If fragment already exists and visible, we will need to update panel with animation.
        if (!shouldForceCreation && fragment != null && fragment instanceof PanelFragment) {
            mPanelFragment = (PanelFragment) fragment;
            if (mPanelFragment.isPanelCreating()) {
                Log.w(TAG, "A panel is creating, skip " + action);
                return;
            }

            final Bundle bundle = fragment.getArguments();
            if (bundle != null
                    && TextUtils.equals(action, bundle.getString(KEY_PANEL_TYPE_ARGUMENT))) {
                Log.w(TAG, "Panel is showing the same action, skip " + action);
                return;
            }

            mPanelFragment.setArguments(new Bundle(mBundle));
            mPanelFragment.updatePanelWithAnimation();
        } else {
            setContentView(R.layout.settings_panel);

            // Move the window to the center of screen, and make it take up the entire screen width.
            final Window window = getWindow();
            window.setGravity(Gravity.CENTER);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            mPanelFragment = new PanelFragment();
            mPanelFragment.setArguments(new Bundle(mBundle));
            fragmentManager.beginTransaction().add(R.id.main_content, mPanelFragment).commit();
        }
    }
}
