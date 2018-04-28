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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * The RunOnInstallReceiver is automatically invoked when the app is first installed from Play
 * Store. Use this as a run once signal to publish the app's first channel and add others to
 * "Customize Channels" menu.
 *
 * Test in your app by "adb shell am broadcast -a android.media.tv.action.INITIALIZE_PROGRAMS
 *      -n your.package.name/.YourReceiverName"
 *
 * Test in this sample app by "adb shell am broadcast -a android.media.tv.action.INITIALIZE_PROGRAMS
 *      -n com.google.android.tvhomescreenchannels/.RunOnInstallReceiver"
 */
public class RunOnInstallReceiver extends BroadcastReceiver {
    private static final String TAG = "RunOnInstallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Synchronizing database");
        SynchronizeDatabaseJobService.schedule(context);
    }
}