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
import android.os.Bundle;
import android.support.media.tv.TvContractCompat;
import android.util.Log;

public class LauncherNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "LauncherNotifcation";

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();

        switch (intent.getAction()) {
            case TvContractCompat.ACTION_PREVIEW_PROGRAM_BROWSABLE_DISABLED:
                // Notice that the user has removed a preview program.
                // The app should remove the program from the channel and should never publish it
                // again. The app could use this as an indication that the user is no longer
                // interested in watching this type of content.
                Log.d(TAG, "preview program browsable disabled");
                break;
            case TvContractCompat.ACTION_WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED:
                // Notice that the user has removed a program from the watch next list.
                Log.d(TAG, "watch next program browsable disabled");
                break;
            case TvContractCompat.ACTION_PREVIEW_PROGRAM_ADDED_TO_WATCH_NEXT:
                // Notice that the user has added a preview program to their watch next list. Use
                // this information to update internal data structures or as an indication of the
                // user's interest in this type of programming.
                // From that point on, the app is responsible for this program and should treat it
                // like other programs in a channel.
                Log.d(TAG, "preview program added to watch next   preview " +
                        extras.getString(TvContractCompat.EXTRA_PREVIEW_PROGRAM_ID)
                        + "  watch-next "
                        + extras.getString(TvContractCompat.EXTRA_WATCH_NEXT_PROGRAM_ID));
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                // Notice that the device has just finished booting. Use this opportunity to publish
                // the initial list of channels to the launcher. This intent is useful for the
                // preinstalled apps on the device. Apps that can be installed from the Play Store
                // should use ACTION_INITIALIZE_PROGRAMS as the signal to start posting their
                // channels.
                Log.d(TAG, "ACTION_BOOT_COMPLETED received");
                break;
            case TvContractCompat.ACTION_INITIALIZE_PROGRAMS:
                // Notice that the app was just installed from the Play Store. Use this opportunity
                // to publish the initial list of channels to the launcher. This intent is useful
                // for apps that don't come "pre-installed" and are installed via Play Store.
                Log.d(TAG, "ACTION_INITIALIZE_PROGRAMS received");
                break;
        }
    }
}

