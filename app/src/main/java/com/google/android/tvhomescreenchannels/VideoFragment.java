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

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.app.VideoFragmentGlueHost;
import android.support.v17.leanback.media.MediaPlayerAdapter;
import android.support.v17.leanback.media.PlaybackBannerControlGlue;
import android.support.v17.leanback.media.PlaybackGlue;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.tvhomescreenchannels.scheduler.AddWatchNextService;
import com.google.android.tvhomescreenchannels.scheduler.ClipData;
import com.google.android.tvhomescreenchannels.scheduler.DeleteWatchNextService;

/**
 * Class for video playback fragment with media controls. It uses
 * 1. PlaybackBannerControlGlue as the glue for displaying media controls.
 * 2. MediaPlayerAdapter that uses an Android MediaPlayer.
 * 3. VideoFragmentGlueHost which provides a SurfaceView.
 */
public class VideoFragment extends android.support.v17.leanback.app.VideoFragment {

    private static final String TAG = "VideoFragment";
    // The min watch time for a video to be considered for the watch next row.
    private static final int MIN_WATCH_TIME_FOR_WATCH_NEXT = 5000;
    final VideoFragmentGlueHost mHost = new VideoFragmentGlueHost(VideoFragment.this);
    private PlaybackBannerControlGlue<MediaPlayerAdapter> mMediaPlayerGlue;
    private Clip mSelectedClip;
    private long mProgress;
    private MediaSessionCompat mSession;
    private boolean mCompleted = false;

    static VideoFragment newInstance(Clip selectedClip, long progress) {
        VideoFragment videoFragment = new VideoFragment();
        Bundle args = new Bundle(2);
        args.putParcelable(PlaybackActivity.EXTRA_CLIP, selectedClip);
        args.putLong(PlaybackActivity.EXTRA_PROGRESS, progress);
        videoFragment.setArguments(args);
        return videoFragment;
    }

    void playWhenReady() {
        mMediaPlayerGlue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
            @Override
            public void onPreparedStateChanged(PlaybackGlue glue) {
                if (glue.isPrepared()) {
                    if (mProgress > 0) {
                        mMediaPlayerGlue.seekTo(mProgress);
                    }
                    glue.play();
                }
            }

            @Override
            public void onPlayStateChanged(PlaybackGlue glue) {
                super.onPlayStateChanged(glue);
                mCompleted = false;
                updatePlaybackState();
            }

            @Override
            public void onPlayCompleted(PlaybackGlue glue) {
                super.onPlayCompleted(glue);
                mCompleted = true;
            }
        });
        if (mMediaPlayerGlue.isPrepared()) {
            if (mProgress > 0) {
                mMediaPlayerGlue.seekTo(mProgress);
            }
            mMediaPlayerGlue.play();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaPlayerGlue = new PlaybackBannerControlGlue<MediaPlayerAdapter>(getContext(),
                new int[]{1}, new MediaPlayerAdapter(getContext())) {
            @Override
            public long getSupportedActions() {
                return PlaybackBannerControlGlue.ACTION_PLAY_PAUSE
                        | PlaybackBannerControlGlue.ACTION_SKIP_TO_NEXT
                        | PlaybackBannerControlGlue.ACTION_SKIP_TO_PREVIOUS;
            }
        };
        mMediaPlayerGlue.setHost(mHost);

        Bundle args = getArguments();
        mSelectedClip = args.getParcelable(PlaybackActivity.EXTRA_CLIP);
        mProgress = args.getLong(PlaybackActivity.EXTRA_PROGRESS);

        mMediaPlayerGlue.setTitle(mSelectedClip.getTitle());
        mMediaPlayerGlue.setSubtitle(mSelectedClip.getDescription());
        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(mSelectedClip.getVideoUrl()));
        mSession = new MediaSessionCompat(getContext(), "TvLauncherSampleApp");
        mSession.setActive(true);
        mSession.setCallback(new MediaSessionCallback());
        playWhenReady();
        updatePlaybackState();
        updateMetadata(mSelectedClip);
    }

    @Override
    public void onStop() {
        super.onStop();
        mSession.release();
        if (mMediaPlayerGlue.getCurrentPosition() >= MIN_WATCH_TIME_FOR_WATCH_NEXT) {
            // Add or remove from the watch next row only if the media has been watched above a
            // minimum threshold
            if (!mCompleted) {
                // If it hasn't be completed yet, add it to watch next
                AddWatchNextService.scheduleAddWatchNextRequest(getContext(), new ClipData.Builder()
                        .setClipId(mSelectedClip.getClipId())
                        .setContentId(mSelectedClip.getContentId())
                        .setTitle(mSelectedClip.getTitle())
                        .setDescription(mSelectedClip.getDescription())
                        .setDuration(mMediaPlayerGlue.getDuration())
                        .setProgress(mMediaPlayerGlue.getCurrentPosition())
                        .setCardImageUrl(mSelectedClip.getCardImageUrl())
                        .build());
            } else {
                // Remove it from the watch next row if the media has finished playing.
                DeleteWatchNextService.scheduleDeleteWatchNextRequest(getContext(),
                        mSelectedClip.getClipId());
            }
        }
    }

    private void updatePlaybackState() {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());
        int state = PlaybackStateCompat.STATE_PLAYING;
        if (!mMediaPlayerGlue.isPlaying()) {
            state = PlaybackState.STATE_PAUSED;
        }
        stateBuilder.setState(state, mMediaPlayerGlue.getCurrentPosition(), 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;
        if (mMediaPlayerGlue.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }

    private void updateMetadata(final Clip clip) {
        final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, clip.getTitle());
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
                clip.getDescription());
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
                clip.getCardImageUrl());

        // And at minimum the title and artist for legacy support
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, clip.getTitle());
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, clip.getDescription());

        Glide.with(this)
                .asBitmap()
                .load(Uri.parse(clip.getCardImageUrl()))
                .into(new SimpleTarget<Bitmap>(500, 500) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition transition) {
                        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
                        mSession.setMetadata(metadataBuilder.build());
                    }

                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        Log.e(TAG, "onLoadFailed: " + errorDrawable);
                        mSession.setMetadata(metadataBuilder.build());
                    }
                });
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            mMediaPlayerGlue.play();
        }

        @Override
        public void onSeekTo(long position) {
            mMediaPlayerGlue.seekTo((int) position);
        }

        @Override
        public void onPause() {
            mMediaPlayerGlue.pause();
        }
    }
}
