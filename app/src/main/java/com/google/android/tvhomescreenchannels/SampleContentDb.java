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
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A sample database emulating a server-base central database capable of tracking:
 * 1. Removed clips 2. Removed programs 3. The latest playback position of a clip to be later
 * replayed if the clip is launched from the watch next row.
 */

public class SampleContentDb {
    private static final String SAMPLE_LOCAL_DB = "sample_local_db";
    private static final String CLIPS_PROGRESS_DB = "clips_progress_db";

    private static final String REMOVED_CLIPS_KEY = "removed_clips_key";
    private static SampleContentDb sSampleContentDb = null;
    private final Set<String> mRemovedClips;
    private final Map<String, Long> mClipsProgress;

    private final Context mContext;

    private SampleContentDb(Context context) {
        mContext = context.getApplicationContext();
        SharedPreferences sampleLocalDbPrefs = mContext.getSharedPreferences(SAMPLE_LOCAL_DB,
                Context.MODE_PRIVATE);
        // Creating a copy of the set instance returned by getStringSet since the consistency of the
        // stored data is not guaranteed if the content is modified according to the docs.
        Set<String> removedClips = sampleLocalDbPrefs.getStringSet(REMOVED_CLIPS_KEY,
                new HashSet<String>());
        mRemovedClips = new HashSet<>(removedClips);

        SharedPreferences clipsProgressPrefs = context.getSharedPreferences(CLIPS_PROGRESS_DB,
                Context.MODE_PRIVATE);
        Map<String, Long> clipsProgress = (Map<String, Long>) clipsProgressPrefs.getAll();
        mClipsProgress = new HashMap<>(clipsProgress);
    }

    public static SampleContentDb getInstance(Context context) {
        if (sSampleContentDb == null) {
            sSampleContentDb = new SampleContentDb(context);
        }
        return sSampleContentDb;
    }

    boolean isClipRemoved(String clipId) {
        return mRemovedClips.contains(clipId);
    }

    public void addRemovedClip(String clipId) {
        if (!mRemovedClips.contains(clipId)) {
            SharedPreferences sampleLocalDbPrefs = mContext.getSharedPreferences(SAMPLE_LOCAL_DB,
                    Context.MODE_PRIVATE);
            mRemovedClips.add(clipId);
            sampleLocalDbPrefs.edit().putStringSet(REMOVED_CLIPS_KEY, mRemovedClips).apply();
        }
    }

    void updateClipProgress(String clipId, long progress) {
        SharedPreferences clipsProgressPrefs = mContext.getSharedPreferences(CLIPS_PROGRESS_DB,
                Context.MODE_PRIVATE);
        mClipsProgress.put(clipId, progress);
        clipsProgressPrefs.edit().putLong(clipId, progress).apply();
    }

    void deleteClipProgress(String clipId) {
        SharedPreferences clipsProgressPrefs = mContext.getSharedPreferences(CLIPS_PROGRESS_DB,
                Context.MODE_PRIVATE);
        mClipsProgress.remove(clipId);
        clipsProgressPrefs.edit().remove(clipId).apply();
    }

    long getClipProgress(String clipId) {
        return mClipsProgress.containsKey(clipId) ? mClipsProgress.get(clipId) : -1;
    }
}
