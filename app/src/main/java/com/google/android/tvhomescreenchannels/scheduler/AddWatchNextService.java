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

package com.google.android.tvhomescreenchannels.scheduler;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.Log;

import com.google.android.tvhomescreenchannels.SampleTvProvider;

/**
 * The service that's scheduled to run the task of adding a video to the watch next playlist
 * on a background thread.
 */
public class AddWatchNextService extends JobService {

    private static final String TAG = "AddWatchNextService";

    private static final String ID_KEY = "id_key";
    private static final String CONTENT_ID_KEY = "content_id_key";
    private static final String DURATION_KEY = "id_duration";
    private static final String PROGRESS_KEY = "id_progress";
    private static final String TITLE_KEY = "id_title";
    private static final String DESCRIPTION_KEY = "id_description";
    private static final String CARD_IMAGE_URL_KEY = "id_card_image_url";

    public static void scheduleAddWatchNextRequest(Context context, ClipData clipData) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(ID_KEY, clipData.getClipId());
        bundle.putString(CONTENT_ID_KEY, clipData.getContentId());
        bundle.putLong(DURATION_KEY, clipData.getDuration());
        bundle.putLong(PROGRESS_KEY, clipData.getProgress());
        bundle.putString(TITLE_KEY, clipData.getTitle());
        bundle.putString(DESCRIPTION_KEY, clipData.getDescription());
        bundle.putString(CARD_IMAGE_URL_KEY, clipData.getCardImageUrl());

        scheduler.schedule(new JobInfo.Builder(1,
                new ComponentName(context, AddWatchNextService.class))
                .setExtras(bundle)
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        AddWatchNextContinueInBackground newTask = new AddWatchNextContinueInBackground(
                jobParameters);
        newTask.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    /**
     * Add a video to the watch next playlist on a background thread.
     */
    private class AddWatchNextContinueInBackground extends AsyncTask<Void, Void, Void> {
        private JobParameters mJobParameters;

        AddWatchNextContinueInBackground(JobParameters jobParameters) {
            mJobParameters = jobParameters;
        }

        @Override
        protected Void doInBackground(Void... params) {
            PersistableBundle bundle = mJobParameters.getExtras();
            if (bundle == null) {
                Log.e(TAG, "No data passed to task for job " + mJobParameters.getJobId());
                return null;
            }

            String id = bundle.getString(ID_KEY);
            String contentId = bundle.getString(CONTENT_ID_KEY);
            long duration = bundle.getLong(DURATION_KEY);
            long progress = bundle.getLong(PROGRESS_KEY);
            String title = bundle.getString(TITLE_KEY);
            String description = bundle.getString(DESCRIPTION_KEY);
            String cardImageURL = bundle.getString(CARD_IMAGE_URL_KEY);

            ClipData clipData = new ClipData.Builder().setClipId(id)
                    .setContentId(contentId)
                    .setDuration(duration)
                    .setProgress(progress)
                    .setTitle(title)
                    .setDescription(description)
                    .setCardImageUrl(cardImageURL)
                    .build();

            SampleTvProvider.addWatchNextContinue(getApplicationContext(), clipData);
            return null;

        }
    }
}
