/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.tvhomescreenchannels;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

/**
 * PlaybackActivity for video playback that loads VideoFragment
 */
public class PlaybackActivity extends Activity {

    public static final String EXTRA_CLIP = "Clip";
    public static final String EXTRA_PROGRESS = "Progress";
    private Clip mClip;
    private long mProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mClip = getIntent().getParcelableExtra(EXTRA_CLIP);
            mProgress = getIntent().getLongExtra(EXTRA_PROGRESS, -1);
            VideoFragment videoFragment =
                    VideoFragment.newInstance(mClip, mProgress);
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(android.R.id.content, videoFragment);
            fragmentTransaction.commit();
        }
    }
}
