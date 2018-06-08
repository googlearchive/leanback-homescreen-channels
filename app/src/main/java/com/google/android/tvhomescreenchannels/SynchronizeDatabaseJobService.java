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

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.media.tv.TvContractCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JobScheduler task to synchronize the TV provider database with the desired list of channels and
 * programs. This sample app runs this once at install time to publish an initial set of channels
 * and programs, however in a real-world setting this might be run at other times to synchronize
 * a server's database with the TV provider database.
 * This code will ensure that the channels from "SampleClipApi.getDesiredPublishedChannelSet()"
 * appear in the TV provider database, and that these and all other programs are synchronized with
 * TV provider database.
 */

public class SynchronizeDatabaseJobService extends JobService {
    private SynchronizeDatabaseTask mSynchronizeDatabaseTask;

    static void schedule(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        scheduler.schedule(new JobInfo.Builder(0,
                new ComponentName(context, SynchronizeDatabaseJobService.class))
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        mSynchronizeDatabaseTask = new SynchronizeDatabaseTask(this, jobParameters);
        mSynchronizeDatabaseTask.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mSynchronizeDatabaseTask != null) {
            mSynchronizeDatabaseTask.cancel(true);
            mSynchronizeDatabaseTask = null;
        }
        return true;
    }

    private static final class ProgramClip {
        String clipId;
        long programId;
        String programTitle;

        ProgramClip(String clipId, long programId, String programTitle) {
            this.clipId = clipId;
            this.programId = programId;
            this.programTitle = programTitle;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ProgramClip)) {
                return false;
            } else {
                ProgramClip other = (ProgramClip) obj;
                return TextUtils.equals(clipId, other.clipId) && programId == other.programId &&
                        TextUtils.equals(programTitle, other.programTitle);
            }
        }

        public int hashCode() {
            return 101 + (clipId != null ? clipId.hashCode() : 0)
                    + (int) (programId ^ (programId >>> 32))
                    + (programTitle != null ? programTitle.hashCode() : 0);
        }
    }

    private static final class ChannelPlaylistId {
        final ArrayList<ProgramClip> mProgramClipId = new ArrayList<>();
        String mPlaylistId;
        long mChannelId;

        ChannelPlaylistId(String playlistId, long channelId) {
            mPlaylistId = playlistId;
            mChannelId = channelId;
        }

        void addProgram(String id, long programId, String programTitle) {
            mProgramClipId.add(new ProgramClip(id, programId, programTitle));
        }
    }

    /**
     * Publish any default channels not already published.
     */
    private class SynchronizeDatabaseTask extends AsyncTask<Void, Void, Void> {
        private final HashMap<Long, ChannelPlaylistId> mChannelPlaylistIds = new HashMap<>();
        private Context mContext;
        private JobParameters mJobParameters;
        private List<Playlist> mDesiredPlaylists;

        SynchronizeDatabaseTask(Context context, JobParameters jobParameters) {
            mContext = context;
            mJobParameters = jobParameters;
            // Get a list of the channels/programs the app wants published.
            mDesiredPlaylists = SampleClipApi.getDesiredPublishedChannelSet();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Load all channels owned by TvLauncherSample from the database.
            loadChannels();
            SampleContentDb sampleContentDb = SampleContentDb.getInstance(mContext);

            // Generate a list of playlists this app wants published. This is "mDesiredPlaylists"
            // (from server) minus those playlists the user has deleted.
            final List<Playlist> wantPlaylistsPublished = new ArrayList<>();
            for (Playlist wantPublished : mDesiredPlaylists) {
                // Want this playlist published.
                wantPlaylistsPublished.add(wantPublished);
            }

            // Generate a list of playlists this app wants un-published. This is any published
            // playlists (from "mChannelPlaylistIds") that are not in (server hosted data set (from
            // "serverPlaylists") less those playlists deleted by the user).
            List<Playlist> serverPlaylists = SampleClipApi.getPlaylistBlocking();
            final HashSet<String> serverPlaylistIds = new LinkedHashSet<>();
            for (Playlist serverPlaylist : serverPlaylists) {
                serverPlaylistIds.add(serverPlaylist.getPlaylistId());
            }
            final List<Long> wantChannelsUnpublished = new ArrayList<>();

            for (Map.Entry<Long, ChannelPlaylistId> entry : mChannelPlaylistIds.entrySet()) {
                ChannelPlaylistId publishedPlaylist = entry.getValue();
                if (!serverPlaylistIds.contains(publishedPlaylist.mPlaylistId)) {
                    wantChannelsUnpublished.add(publishedPlaylist.mChannelId);
                }
            }

            // Unpublish the channels in "wantChannelsUnpublished" and remove them from
            // "mChannelPlaylistIds".
            for (Long channelIdToUnpublish : wantChannelsUnpublished) {
                SampleTvProvider.deleteChannel(mContext, channelIdToUnpublish);
                mChannelPlaylistIds.remove(channelIdToUnpublish);
            }

            Set<Map.Entry<Long, ChannelPlaylistId>> channelPlayListIdsSet =
                    mChannelPlaylistIds.entrySet();

            // Load published programs from still-published channels.
            for (Map.Entry<Long, ChannelPlaylistId> entry : channelPlayListIdsSet) {
                ChannelPlaylistId channelPlaylistId = entry.getValue();
                loadProgramsForChannel(channelPlaylistId);
            }

            // Publish the playlists in "wantPlaylistsPublished" that are not already published.
            final HashSet<String> publishedPlaylists = new HashSet<>();
            for (Map.Entry<Long, ChannelPlaylistId> entry : channelPlayListIdsSet) {
                ChannelPlaylistId channelPlaylistId = entry.getValue();
                publishedPlaylists.add(channelPlaylistId.mPlaylistId);
            }
            for (Playlist playlist : wantPlaylistsPublished) {
                if (!publishedPlaylists.contains(playlist.getPlaylistId())) {
                    SampleTvProvider.addChannel(mContext, playlist);
                }
            }

            // Synchronize the clips remaining in "mChannelPlaylistIds" by adding clips not present,
            // deleting clips that aren't in "SampleClipApi" database and updating any that differ.
            for (Map.Entry<Long, ChannelPlaylistId> entry : channelPlayListIdsSet) {
                ChannelPlaylistId channelPlaylistId = entry.getValue();
                Playlist serverPlaylist =
                        SampleClipApi.getPlaylistById(channelPlaylistId.mPlaylistId);
                final HashMap<Long, Clip> wantClipsPublished = new HashMap<>();
                for (Clip serverClip : serverPlaylist.getClips()) {
                    if (!sampleContentDb.isClipRemoved(serverClip.getClipId())) {
                        wantClipsPublished.put(serverClip.getProgramId(), serverClip);
                    }
                }
                final HashSet<Long> wantProgramsUnpublished = new HashSet<>();
                for (ProgramClip publishedClip : channelPlaylistId.mProgramClipId) {
                    wantClipsPublished.remove(publishedClip.programId);
                    wantProgramsUnpublished.add(publishedClip.programId);
                }
                for (Clip serverClip : serverPlaylist.getClips()) {
                    if (!sampleContentDb.isClipRemoved(serverClip.getClipId())) {
                        wantProgramsUnpublished.remove(serverClip.getProgramId());
                    }
                }
                final List<Clip> wantClipsProgramsUpdate = new ArrayList<>();
                for (ProgramClip publishedClip : channelPlaylistId.mProgramClipId) {
                    if (!wantProgramsUnpublished.contains(publishedClip.programId)) {
                        Clip clip = SampleClipApi.getClipByIdBlocking(publishedClip.clipId);
                        if (!TextUtils.equals(publishedClip.programTitle, clip.getTitle())) {
                            wantClipsProgramsUpdate.add(clip);
                        }
                    }
                }
                unpublishPrograms(wantProgramsUnpublished);
                updateProgramsClips(wantClipsProgramsUpdate);
                publishClips(wantClipsPublished, channelPlaylistId.mChannelId,
                        channelPlaylistId.mProgramClipId.size());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mSynchronizeDatabaseTask = null;
            jobFinished(mJobParameters, false);
        }

        private void loadChannels() {
            // Iterate "cursor" through all the channels owned by this app.
            try (Cursor cursor = mContext.getContentResolver().query(TvContractCompat
                            .Channels.CONTENT_URI, SampleTvProvider.CHANNELS_MAP_PROJECTION,
                    null, null,
                    null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String playlistId = cursor.getString(SampleTvProvider
                                .CHANNELS_COLUMN_INTERNAL_PROVIDER_ID_INDEX);
                        long channelId = cursor.getLong(SampleTvProvider.CHANNELS_COLUMN_ID_INDEX);
                        mChannelPlaylistIds.put(channelId,
                                new ChannelPlaylistId(playlistId, channelId));
                    }
                    cursor.close();
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
                            long programId = cursor.getLong(SampleTvProvider
                                    .PROGRAMS_COLUMN_ID_INDEX);
                            String title = cursor.getString(SampleTvProvider
                                    .PROGRAMS_COLUMN_TITLE_INDEX);
                            channel.addProgram(id, programId, title);
                        }
                    }
                    cursor.close();
                }
            }
        }

        private void updateProgramsClips(List<Clip> wantClipsProgramsUpdate) {
            for (Clip clip : wantClipsProgramsUpdate) {
                SampleTvProvider.updateProgramClip(mContext, clip);
            }
        }

        private void unpublishPrograms(HashSet<Long> wantProgramsUnpublished) {
            for (Long programId : wantProgramsUnpublished) {
                SampleTvProvider.deleteProgram(mContext, programId);
            }
        }

        private void publishClips(HashMap<Long, Clip> wantClipsPublished, long channelId,
                int clipsPublishedAlready) {
            int weight = clipsPublishedAlready + wantClipsPublished.size();
            for (Clip clip : wantClipsPublished.values()) {
                SampleTvProvider.publishProgram(mContext, clip, channelId, weight--);
            }
        }
    }
}
