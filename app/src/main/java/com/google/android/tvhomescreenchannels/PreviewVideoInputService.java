/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.media.tv.TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import com.google.android.tvhomescreenchannels.SampleClipApi.GetClipByIdListener;

import java.io.IOException;

/**
 * Plays video previews on a surface on the home screen.
 */
public class PreviewVideoInputService extends TvInputService {
    private static final String TAG = "PreviewVideoInputService";

    @Nullable
    @Override
    public Session onCreateSession(String inputId) {
        Log.d(TAG, inputId);
        return new PreviewSession(this);
    }

    private class PreviewSession extends TvInputService.Session {

        private MediaPlayer mPlayer;
        private final GetClipByIdListener mGetClipByIdListener;

        PreviewSession(Context context) {
            super(context);
            mPlayer = new MediaPlayer();

            mGetClipByIdListener = new GetClipByIdListener() {
                @Override
                public void onGetClipById(Clip clip) {
                    try {
                        mPlayer.setDataSource(clip.getPreviewVideoUrl());
                        mPlayer.prepare();
                        mPlayer.start();

                        notifyVideoAvailable();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not prepare media mPlayer", e);
                        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                    }
                }
            };
        }

        @Override
        public boolean onTune(Uri channelUri) {

            notifyVideoUnavailable(VIDEO_UNAVAILABLE_REASON_TUNING);

            String clipId = channelUri.getLastPathSegment();

            SampleClipApi.getClipById(clipId, mGetClipByIdListener);

            return true;
        }

        @Override
        public boolean onSetSurface(@Nullable Surface surface) {
            if (mPlayer != null) {
                mPlayer.setSurface(surface);
            }
            return true;
        }

        @Override
        public void onRelease() {
            SampleClipApi.cancelGetClipById(mGetClipByIdListener);
            if (mPlayer != null) {
                mPlayer.release();
            }
            mPlayer = null;
        }

        @Override
        public void onSetStreamVolume(float volume) {
            if (mPlayer != null) {
                // The home screen may control the video's volume. Your player should be updated
                // accordingly.
                mPlayer.setVolume(volume, volume);
            }
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
        }
    }
}
