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

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.DrawableRes;
import android.support.annotation.WorkerThread;
import android.support.media.tv.Channel;
import android.support.media.tv.ChannelLogoUtils;
import android.support.media.tv.PreviewProgram;
import android.support.media.tv.TvContractCompat;
import android.support.media.tv.TvContractCompat.Channels;
import android.support.media.tv.TvContractCompat.PreviewPrograms;
import android.support.media.tv.WatchNextProgram;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.tvhomescreenchannels.scheduler.ClipData;

import java.util.List;

public class SampleTvProvider {
    /**
     * Indices into "CHANNELS_MAP_PROJECTION" and if that changes, these should too.
     */
    static final int CHANNELS_COLUMN_ID_INDEX = 0;
    static final int CHANNELS_COLUMN_INTERNAL_PROVIDER_ID_INDEX = 1;
    static final int CHANNELS_COLUMN_BROWSABLE_INDEX = 2;
    static final String[] CHANNELS_MAP_PROJECTION =
            {Channels._ID, Channels.COLUMN_INTERNAL_PROVIDER_ID, Channels.COLUMN_BROWSABLE};
    /**
     * Indices into "PROGRAMS_MAP_PROJECTION" and if that changes, these should too.
     */
    static final int PROGRAMS_COLUMN_ID_INDEX = 0;
    static final int PROGRAMS_COLUMN_INTERNAL_PROVIDER_ID_INDEX = 1;
    static final int PROGRAMS_COLUMN_TITLE_INDEX = 2;
    static final int PROGRAMS_COLUMN_INTERNAL_INTERACTION_TYPE_INDEX = 3;
    static final int PROGRAMS_COLUMN_INTERNAL_INTERACTION_COUNT_INDEX = 4;
    static final String[] PROGRAMS_MAP_PROJECTION =
            {TvContractCompat.PreviewPrograms._ID,
                    TvContractCompat.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID,
                    TvContractCompat.PreviewPrograms.COLUMN_TITLE,
                    TvContractCompat.PreviewProgramColumns.COLUMN_INTERACTION_TYPE,
                    TvContractCompat.PreviewProgramColumns.COLUMN_INTERACTION_COUNT};
    private static final String TAG = "SampleTvProvider";
    private static final String SCHEME = "tvhomescreenchannels";
    private static final String APPS_LAUNCH_HOST = "com.google.android.tvhomescreenchannels";
    private static final String PLAY_VIDEO_ACTION_PATH = "playvideo";
    private static final String START_APP_ACTION_PATH = "startapp";
    /**
     * Index into "WATCH_NEXT_MAP_PROJECTION" and if that changes, this should change too.
     */
    private static final int COLUMN_WATCH_NEXT_ID_INDEX = 0;
    private static final int COLUMN_WATCH_NEXT_INTERNAL_PROVIDER_ID_INDEX = 1;
    private static final int COLUMN_WATCH_NEXT_COLUMN_BROWSABLE_INDEX = 2;

    private static final String[] WATCH_NEXT_MAP_PROJECTION =
            {BaseColumns._ID, TvContractCompat.WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID,
                    TvContractCompat.WatchNextPrograms.COLUMN_BROWSABLE};

    private static final Uri PREVIEW_PROGRAMS_CONTENT_URI =
            Uri.parse("content://android.media.tv/preview_program");

    private SampleTvProvider() {
    }

    static private String createInputId(Context context) {
        ComponentName cName = new ComponentName(context, MainActivity.class.getName());
        return TvContractCompat.buildInputId(cName);
    }

    /**
     * Writes a drawable as the channel logo.
     *
     * @param channelId  identifies the channel to write the logo.
     * @param drawableId resource to write as the channel logo. This must be a bitmap and not, say
     *                   a vector drawable.
     */
    @WorkerThread
    static private void writeChannelLogo(Context context, long channelId,
            @DrawableRes int drawableId) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);
        ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap);
    }

    @WorkerThread
    public static void addWatchNextContinue(Context context, ClipData clipData) {
        final String clipId = clipData.getClipId();
        final String contentId = clipData.getContentId();

        // Check if program "key" has already been added.
        boolean isProgramPresent = false;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    TvContractCompat.WatchNextPrograms.CONTENT_URI, WATCH_NEXT_MAP_PROJECTION, null,
                    null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!cursor.isNull(COLUMN_WATCH_NEXT_INTERNAL_PROVIDER_ID_INDEX)
                            && TextUtils.equals(clipId, cursor.getString(
                            COLUMN_WATCH_NEXT_INTERNAL_PROVIDER_ID_INDEX))) {
                        // Found a row that contains an equal COLUMN_INTERNAL_PROVIDER_ID.
                        long watchNextProgramId = cursor.getLong(COLUMN_WATCH_NEXT_ID_INDEX);
                        // If the clip exists in watch next programs, there are 2 cases:
                        // 1. The program was not removed by the user (browsable == 1) and we
                        // only need to update the existing info for that program
                        // 2. The program was removed by the user from watch next
                        // (browsable== 0), in which case we will first remove it from watch
                        // next database and then treat it as a new watch next program to be
                        // inserted.
                        if (cursor.getInt(COLUMN_WATCH_NEXT_COLUMN_BROWSABLE_INDEX) == 0) {
                            int rowsDeleted = context.getContentResolver().delete(
                                    TvContractCompat.buildWatchNextProgramUri(
                                            watchNextProgramId), null,
                                    null);
                            if (rowsDeleted < 1) {
                                Log.e(TAG, "Delete program failed");
                            }
                        } else {
                            WatchNextProgram existingProgram = WatchNextProgram.fromCursor(
                                    cursor);
                            // Updating the following columns since when a program is added
                            // manually through the launcher interface to the WatchNext row:
                            // 1. watchNextType is set to WATCH_NEXT_TYPE_WATCHLIST which
                            // should be changed to WATCH_NEXT_TYPE_CONTINUE when at least 1
                            // minute of the video is played.
                            // 2. The duration may not have been set for the programs in a
                            // channel row since the video wasn't processed then to set this
                            // column. Also setting lastPlaybackPosition to maintain the
                            // correct progressBar upon returning to the launcher.
                            WatchNextProgram.Builder builder = new WatchNextProgram.Builder(
                                    existingProgram)
                                    .setWatchNextType(TvContractCompat.WatchNextPrograms
                                            .WATCH_NEXT_TYPE_CONTINUE)
                                    .setLastPlaybackPositionMillis((int) clipData.getProgress())
                                    .setDurationMillis((int) clipData.getDuration());
                            ContentValues contentValues = builder.build().toContentValues();
                            Uri watchNextProgramUri = TvContractCompat.buildWatchNextProgramUri(
                                    watchNextProgramId);
                            int rowsUpdated = context.getContentResolver().update(
                                    watchNextProgramUri,
                                    contentValues, null, null);
                            if (rowsUpdated < 1) {
                                Log.e(TAG, "Update program failed");
                            }
                            isProgramPresent = true;
                        }
                    }
                }
            }
            if (!isProgramPresent) {
                WatchNextProgram.Builder builder = new WatchNextProgram.Builder();
                builder.setType(TvContractCompat.WatchNextPrograms.TYPE_CLIP)
                        .setWatchNextType(
                                TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                        .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                        .setTitle(clipData.getTitle())
                        .setDescription(clipData.getDescription())
                        .setPosterArtUri(Uri.parse(clipData.getCardImageUrl()))
                        .setIntentUri(Uri.parse(SCHEME + "://" + APPS_LAUNCH_HOST
                                + "/" + PLAY_VIDEO_ACTION_PATH + "/" + clipId))
                        .setInternalProviderId(clipId)
                        // Setting the contentId to avoid having duplicate programs with the same
                        // content added to the watch next row (The launcher will use the contentId
                        // to detect duplicates). Note that, programs of different channels can
                        // still point to the same content i.e. their contentId can be the same.
                        .setContentId(contentId)
                        .setLastPlaybackPositionMillis((int) clipData.getProgress())
                        .setDurationMillis((int) clipData.getDuration());
                ContentValues contentValues = builder.build().toContentValues();
                Uri programUri = context.getContentResolver().insert(
                        TvContractCompat.WatchNextPrograms.CONTENT_URI, contentValues);
                if (programUri == null || programUri.equals(Uri.EMPTY)) {
                    Log.e(TAG, "Insert watch next program failed");
                }
            }
            SampleContentDb.getInstance(context).updateClipProgress(clipId, clipData.getProgress());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @WorkerThread
    public static void deleteWatchNextContinue(Context context, String clipId) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    TvContractCompat.WatchNextPrograms.CONTENT_URI, WATCH_NEXT_MAP_PROJECTION, null,
                    null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!cursor.isNull(COLUMN_WATCH_NEXT_INTERNAL_PROVIDER_ID_INDEX)
                            && TextUtils.equals(clipId, cursor.getString(
                            COLUMN_WATCH_NEXT_INTERNAL_PROVIDER_ID_INDEX))) {
                        long watchNextProgramId = cursor.getLong(COLUMN_WATCH_NEXT_ID_INDEX);
                        int rowsDeleted = context.getContentResolver().delete(
                                TvContractCompat.buildWatchNextProgramUri(watchNextProgramId), null,
                                null);
                        if (rowsDeleted < 1) {
                            Log.e(TAG, "Delete program failed");
                        }
                        SampleContentDb.getInstance(context).deleteClipProgress(clipId);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @WorkerThread
    static long addChannel(Context context, Playlist playlist) {
        String channelInputId = createInputId(context);
        Channel channel = new Channel.Builder()
                .setDisplayName(playlist.getName())
                .setDescription(playlist.getDescription())
                .setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setInputId(channelInputId)
                .setAppLinkIntentUri(Uri.parse(SCHEME + "://" + APPS_LAUNCH_HOST
                        + "/" + START_APP_ACTION_PATH))
                .setInternalProviderId(playlist.getPlaylistId())
                .build();

        Uri channelUri = context.getContentResolver().insert(Channels.CONTENT_URI,
                channel.toContentValues());
        if (channelUri == null || channelUri.equals(Uri.EMPTY)) {
            Log.e(TAG, "Insert channel failed");
            return 0;
        }
        long channelId = ContentUris.parseId(channelUri);
        playlist.setChannelPublishedId(channelId);

        writeChannelLogo(context, channelId, R.drawable.app_icon);

        List<Clip> clips = playlist.getClips();

        int weight = clips.size();
        for (int i = 0; i < clips.size(); ++i, --weight) {
            Clip clip = clips.get(i);
            final String clipId = clip.getClipId();
            final String contentId = clip.getContentId();

            Uri previewProgramVideoUri;
            if (clip.isVideoProtected()) {
                // Create URI for TIF Input Service to be triggered
                // content://android.media.tv/preview_program/<clipId>
                ComponentName componentName = new ComponentName(context,
                        PreviewVideoInputService.class);
                previewProgramVideoUri = PreviewPrograms.CONTENT_URI.buildUpon()
                        .appendEncodedPath(clipId)
                        .appendQueryParameter("input", TvContractCompat.buildInputId(componentName))
                        .build();
            } else {
                // Not a protected video, use public https:// URL.
                previewProgramVideoUri = Uri.parse(clip.getPreviewVideoUrl());
            }

            PreviewProgram program = new PreviewProgram.Builder()
                    .setChannelId(channelId)
                    .setTitle(clip.getTitle())
                    .setDescription(clip.getDescription())
                    .setPosterArtUri(Uri.parse(clip.getCardImageUrl()))
                    .setIntentUri(Uri.parse(SCHEME + "://" + APPS_LAUNCH_HOST
                            + "/" + PLAY_VIDEO_ACTION_PATH + "/" + clipId))
                    .setPreviewVideoUri(previewProgramVideoUri)
                    .setInternalProviderId(clipId)
                    .setContentId(contentId)
                    .setWeight(weight)
                    .setPosterArtAspectRatio(clip.getAspectRatio())
                    .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                    .build();

            Uri programUri = context.getContentResolver().insert(PREVIEW_PROGRAMS_CONTENT_URI,
                    program.toContentValues());
            if (programUri == null || programUri.equals(Uri.EMPTY)) {
                Log.e(TAG, "Insert program failed");
            } else {
                clip.setProgramId(ContentUris.parseId(programUri));
            }
        }
        return channelId;
    }

    @WorkerThread
    static void deleteChannel(Context context, long channelId) {
        int rowsDeleted = context.getContentResolver().delete(
                TvContractCompat.buildChannelUri(channelId), null, null);
        if (rowsDeleted < 1) {
            Log.e(TAG, "Delete channel failed");
        }
    }

    @WorkerThread
    public static void deleteProgram(Context context, Clip clip) {
        deleteProgram(context, clip.getProgramId());
    }

    @WorkerThread
    static void deleteProgram(Context context, long programId) {
        int rowsDeleted = context.getContentResolver().delete(
                TvContractCompat.buildPreviewProgramUri(programId), null, null);
        if (rowsDeleted < 1) {
            Log.e(TAG, "Delete program failed");
        }
    }

    @WorkerThread
    static void updateProgramClip(Context context, Clip clip) {
        long programId = clip.getProgramId();
        Uri programUri = TvContractCompat.buildPreviewProgramUri(programId);
        try (Cursor cursor = context.getContentResolver().query(programUri, null, null, null,
                null)) {
            if (!cursor.moveToFirst()) {
                Log.e(TAG, "Update program failed");
            }
            PreviewProgram porgram = PreviewProgram.fromCursor(cursor);
            PreviewProgram.Builder builder = new PreviewProgram.Builder(porgram)
                    .setTitle(clip.getTitle());

            int rowsUpdated = context.getContentResolver().update(programUri,
                    builder.build().toContentValues(), null, null);
            if (rowsUpdated < 1) {
                Log.e(TAG, "Update program failed");
            }
        }
    }

    static void publishProgram(Context context, Clip clip, long channelId, int weight) {
        final String clipId = clip.getClipId();

        PreviewProgram program = new PreviewProgram.Builder()
                .setChannelId(channelId)
                .setTitle(clip.getTitle())
                .setDescription(clip.getDescription())
                .setPosterArtUri(Uri.parse(clip.getCardImageUrl()))
                .setIntentUri(Uri.parse(SCHEME + "://" + APPS_LAUNCH_HOST
                        + "/" + PLAY_VIDEO_ACTION_PATH + "/" + clipId))
                .setPreviewVideoUri(Uri.parse(clip.getPreviewVideoUrl()))
                .setInternalProviderId(clipId)
                .setWeight(weight)
                .setPosterArtAspectRatio(clip.getAspectRatio())
                .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE)
                .build();

        Uri programUri = context.getContentResolver().insert(PREVIEW_PROGRAMS_CONTENT_URI,
                program.toContentValues());
        if (programUri == null || programUri.equals(Uri.EMPTY)) {
            Log.e(TAG, "Insert program failed");
            return;
        }
        clip.setProgramId(ContentUris.parseId(programUri));
    }

    static String decodeVideoId(Uri uri) {
        List<String> paths = uri.getPathSegments();
        if (paths.size() == 2 && TextUtils.equals(paths.get(0), PLAY_VIDEO_ACTION_PATH)) {
            return paths.get(1);
        }
        return new String();
    }

    @WorkerThread
    static void setProgramViewCount(Context context, long programId, int numberOfViews) {
        Uri programUri = TvContractCompat.buildPreviewProgramUri(programId);
        try (Cursor cursor = context.getContentResolver().query(programUri, null, null, null,
                null)) {
            if (!cursor.moveToFirst()) {
                return;
            }
            PreviewProgram existingProgram = PreviewProgram.fromCursor(cursor);
            PreviewProgram.Builder builder = new PreviewProgram.Builder(existingProgram)
                    .setInteractionCount(numberOfViews)
                    .setInteractionType(TvContractCompat.PreviewProgramColumns
                            .INTERACTION_TYPE_VIEWS);
            int rowsUpdated = context.getContentResolver().update(
                    TvContractCompat.buildPreviewProgramUri(programId),
                    builder.build().toContentValues(), null, null);
            if (rowsUpdated != 1) {
                Log.e(TAG, "Update program failed");
            }
        }
    }
}
