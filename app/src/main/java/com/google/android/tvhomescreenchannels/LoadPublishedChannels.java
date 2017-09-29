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

import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.media.tv.TvContractCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Query the channels already added by this app.
 */
class LoadPublishedChannels extends AsyncTask<Void, Void, Void> {
    private Context mContext;
    private ArrayList<ChannelPlaylistId> mChannelPlaylistIds = new ArrayList<>();
    private Listener mListener;

    LoadPublishedChannels(Context context, Listener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected Void doInBackground(Void... params) {
        loadChannels();
        for (ChannelPlaylistId channelPlaylistId : mChannelPlaylistIds) {
            loadProgramsForChannel(channelPlaylistId);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        mListener.onPublishedChannelsLoaded(mChannelPlaylistIds);
    }

    private void loadChannels() {
        // Iterate "cursor" through all the channels owned by this app.
        try (Cursor cursor = mContext.getContentResolver().query(TvContract.Channels.CONTENT_URI,
                SampleTvProvider.CHANNELS_MAP_PROJECTION, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!cursor.isNull(SampleTvProvider
                            .CHANNELS_COLUMN_INTERNAL_PROVIDER_ID_INDEX)) {
                        long channelId = cursor.getLong(SampleTvProvider.CHANNELS_COLUMN_ID_INDEX);
                        if (cursor.getInt(SampleTvProvider.CHANNELS_COLUMN_BROWSABLE_INDEX) == 0) {
                            // This channel is not browsable as it was removed by the user from the
                            // launcher. Use this as an indication that the channel is not desired
                            // by the user and could relay this information to the server to act
                            // accordingly. Note that no intent is received when a channel is
                            // removed from the launcher and it's the app's responsibility to
                            // examine the browsable flag and act accordingly.
                            SampleTvProvider.deleteChannel(mContext, channelId);
                        } else {
                            // Found a row that contains a non-null provider id.
                            String id = cursor.getString(SampleTvProvider
                                    .CHANNELS_COLUMN_INTERNAL_PROVIDER_ID_INDEX);
                            mChannelPlaylistIds.add(new ChannelPlaylistId(id, channelId));
                        }
                    }
                }
            }
        }
    }

    private void loadProgramsForChannel(ChannelPlaylistId channel) {
        // Iterate "cursor" through all the programs assigned to "channelId".
        Uri programUri = TvContractCompat.buildPreviewProgramsUriForChannel(channel.mChannelId);
        try (Cursor cursor = mContext.getContentResolver().query(programUri,
                SampleTvProvider.PROGRAMS_MAP_PROJECTION, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!cursor.isNull(SampleTvProvider
                            .PROGRAMS_COLUMN_INTERNAL_PROVIDER_ID_INDEX)) {
                        // Found a row that contains a non-null COLUMN_INTERNAL_PROVIDER_ID.
                        String id = cursor.getString(SampleTvProvider
                                .PROGRAMS_COLUMN_INTERNAL_PROVIDER_ID_INDEX);
                        long programId = cursor.getLong(SampleTvProvider.PROGRAMS_COLUMN_ID_INDEX);
                        int viewCount = 0;
                        if (!cursor.isNull(SampleTvProvider
                                .PROGRAMS_COLUMN_INTERNAL_INTERACTION_TYPE_INDEX)
                                && !cursor.isNull(SampleTvProvider
                                .PROGRAMS_COLUMN_INTERNAL_INTERACTION_COUNT_INDEX)) {
                            int interactionType = cursor.getInt(SampleTvProvider
                                    .PROGRAMS_COLUMN_INTERNAL_INTERACTION_TYPE_INDEX);
                            if (interactionType == TvContractCompat.PreviewProgramColumns
                                    .INTERACTION_TYPE_VIEWS) {
                                viewCount = cursor.getInt(SampleTvProvider
                                        .PROGRAMS_COLUMN_INTERNAL_INTERACTION_TYPE_INDEX);
                            }
                        }

                        channel.addProgram(id, programId, viewCount);
                    }
                }
            }
        }
    }

    interface Listener {
        void onPublishedChannelsLoaded(List<ChannelPlaylistId> publishedChannels);
    }

    static final class ProgramClipId {
        String mId;
        long mProgramId;
        int mViewCount;

        ProgramClipId(String id, long programId, int viewCount) {
            mId = id;
            mProgramId = programId;
            mViewCount = viewCount;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ProgramClipId)) {
                return false;
            } else {
                ProgramClipId other = (ProgramClipId) obj;
                return TextUtils.equals(mId, other.mId) && mProgramId == other.mProgramId
                        && mViewCount == other.mViewCount;
            }
        }

        public int hashCode() {
            return 101 + (mId != null ? mId.hashCode() : 0)
                    + (int) (mProgramId ^ (mProgramId >>> 32)) + mViewCount;
        }
    }

    static final class ChannelPlaylistId {
        String mId;
        long mChannelId;
        ArrayList<ProgramClipId> mProgramClipIds = new ArrayList<>();

        ChannelPlaylistId(String id, long channelId) {
            mId = id;
            mChannelId = channelId;
        }

        void addProgram(String id, long programId, int viewCount) {
            mProgramClipIds.add(new ProgramClipId(id, programId, viewCount));
        }
    }
}
