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
 * The service that's scheduled to run the task of deleting a video from the watch next playlist
 * on a background thread.
 */
public class DeleteWatchNextService extends JobService {

    private static final String TAG = "AddWatchNextService";

    private static final String ID_KEY = "id_key";

    public static void scheduleDeleteWatchNextRequest(Context context, String clipId) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(ID_KEY, clipId);

        scheduler.schedule(new JobInfo.Builder(1, new ComponentName(context,
                DeleteWatchNextService.class))
                .setExtras(bundle)
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        DeleteWatchNextContinueInBackground newTask = new DeleteWatchNextContinueInBackground(
                jobParameters);
        newTask.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    /**
     * Deletes a video from the watch next playlist on a background thread.
     */
    private class DeleteWatchNextContinueInBackground extends AsyncTask<Void, Void, Void> {
        private JobParameters mJobParameters;

        DeleteWatchNextContinueInBackground(JobParameters jobParameters) {
            mJobParameters = jobParameters;
        }

        @Override
        protected Void doInBackground(Void... params) {
            PersistableBundle bundle = mJobParameters.getExtras();
            if (bundle == null) {
                Log.e(TAG, "No data passed to task for job " + mJobParameters.getJobId());
                return null;
            }

            String clipId = bundle.getString(ID_KEY);
            SampleTvProvider.deleteWatchNextContinue(getApplicationContext(), clipId);
            return null;
        }
    }
}
