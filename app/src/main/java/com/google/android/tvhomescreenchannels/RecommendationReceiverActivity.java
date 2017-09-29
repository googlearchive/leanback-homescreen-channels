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
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class RecommendationReceiverActivity extends Activity implements
        SampleClipApi.GetClipByIdListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        String videoId = SampleTvProvider.decodeVideoId(getIntent().getData());
        SampleClipApi.getClipById(videoId, this);
    }

    public void onGetClipById(Clip clip) {
        if (clip != null) {
            Intent playVideo = new Intent(this, PlaybackActivity.class);
            playVideo.putExtra(PlaybackActivity.EXTRA_CLIP, clip);
            playVideo.putExtra(PlaybackActivity.EXTRA_PROGRESS,
                    SampleContentDb.getInstance(this).getClipProgress(clip.getClipId()));
            startActivity(playVideo);
        } else {
            Toast.makeText(this, getResources().getString(R.string.cant_play_video),
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SampleClipApi.cancelGetClipById(this);
    }
}
