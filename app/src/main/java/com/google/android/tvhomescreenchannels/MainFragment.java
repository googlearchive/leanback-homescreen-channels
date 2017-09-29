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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.media.tv.TvContractCompat;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.tvhomescreenchannels.presenters.AddChannelPresenter;
import com.google.android.tvhomescreenchannels.presenters.CardPresenter;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFragment extends BrowseFragment implements SampleClipApi.GetPlaylistsListener,
        LoadPublishedChannels.Listener {

    private static final String TAG = "MainFragment";
    private static final boolean DEBUG = true;
    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int ADD_CHANNEL_REQUEST = 1;

    private final Handler mHandler = new Handler();
    // The main adapter containing all the rows of this fragment.
    ArrayObjectAdapter mRowsAdapter;
    // The adapter for the last row that contains the "Add channel" action button.
    ArrayObjectAdapter mLastRowAdapter;
    int mNextChannelIndexToPublish = 0;
    Map<String, LoadPublishedChannels.ChannelPlaylistId> mPublishedChannels = new HashMap<>();
    private DisplayMetrics mMetrics;
    private URI mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private List<Playlist> mPlaylists;
    private Runnable mBackgroudUpdateRunnable;
    // Whether the playlists have finished loading from the server.
    private boolean mPlaylistsLoadedFromServer = false;
    // Whether the fragment has started.
    private boolean mStarted = false;
    // AsyncTask for loading published channels on a background thread.
    private AsyncTask mLoadPublishedChannelsTask;
    // The PresenterSelector for picking the presenter to display clips or "Add channel" button
    private ClipPresenterSelector mPresenterSelector;

    @Override
    public void onGetPlaylists(List<Playlist> playlists) {
        mPlaylists = playlists;
        loadRows();
        mPlaylistsLoadedFromServer = true;
        loadPublishedChannelsIfReady();
    }

    /**
     * Loads published channels from the TV provider database in a background thread. This is used
     * for the following purposes:
     * 1. Store the program id and viewCount of clips displayed in channel rows.
     * 2. Keep track of what channels are currently published in order to present the user with the
     * correct set of channels to be published next to the launcher.
     * 3. Relay this information to the server to act accordingly.
     */
    private void loadPublishedChannelsIfReady() {
        // This method is called from both when the playlists are loaded from the server and when
        // the fragment has started. The background thread of loading channels will start only if
        // both these conditions are true.
        if (!mPlaylistsLoadedFromServer || !mStarted) {
            return;
        }
        cleanUpLoadChannelsTask();
        mLoadPublishedChannelsTask = new LoadPublishedChannels(getActivity(), this)
                .execute();
    }

    @Override
    public void onPublishedChannelsLoaded(List<LoadPublishedChannels.ChannelPlaylistId>
            publishedChannels) {
        mPublishedChannels.clear();
        for (LoadPublishedChannels.ChannelPlaylistId channelPlaylist : publishedChannels) {
            mPublishedChannels.put(channelPlaylist.mId, channelPlaylist);
        }
        for (Playlist playlist : mPlaylists) {
            LoadPublishedChannels.ChannelPlaylistId channelPlaylistId =
                    mPublishedChannels.get(playlist.getPlaylistId());
            if (channelPlaylistId != null) {
                playlist.setChannelPublishedId(channelPlaylistId.mChannelId);
                HashMap<String, ClipData> loadedPrograms = new HashMap<>();
                for (LoadPublishedChannels.ProgramClipId programClipId :
                        channelPlaylistId.mProgramClipIds) {
                    loadedPrograms.put(programClipId.mId, new ClipData(programClipId.mProgramId,
                            programClipId.mViewCount));
                }
                for (Clip clip : playlist.getClips()) {
                    ClipData clipData = loadedPrograms.get(clip.getClipId());
                    if (clipData != null) {
                        clip.setProgramId(clipData.programId);
                        clip.setViewCount(clipData.viewCount);
                    }
                }
            } else {
                // Should explicitly set the published flag to false as the playlist is cached in
                // the form of a static member in SampleClipApi. Thus it's possible that a cached
                // playlist which used to be published is no longer published as a result of user
                // removing the channel from the launcher and returning to the app.
                playlist.setChannelPublished(false);
            }
        }

        // Now that we have the current list of published channels, update the "Add channel" UI with
        // the next available unpublished channel to be added next.
        provideNextUnpublishedChannel();
    }

    private void cleanUpLoadChannelsTask() {
        if (mLoadPublishedChannelsTask != null) {
            mLoadPublishedChannelsTask.cancel(true);
            mLoadPublishedChannelsTask = null;
        }
    }

    /**
     * Traverses the list of unpublished channels and presents the user with the next available
     * channel to be published to the TV provider, or removes the "Add channel" UI if no such
     * channels are available.
     */
    private void provideNextUnpublishedChannel() {

        int nextChannelIndex = mNextChannelIndexToPublish;
        boolean candidateChannelFound = false;

        do {
            if (!mPublishedChannels.containsKey(mPlaylists.get(nextChannelIndex).getPlaylistId())) {
                mNextChannelIndexToPublish = nextChannelIndex;
                candidateChannelFound = true;
                break;
            }
            nextChannelIndex = (nextChannelIndex + 1) % mPlaylists.size();
        } while (nextChannelIndex != mNextChannelIndexToPublish);

        if (!candidateChannelFound) {
            if (mLastRowAdapter != null) {
                // No candidate unpublished channels are found and the "Add channel" UI exists.
                // Will remove this action button from the UI.
                mRowsAdapter.removeItems(mRowsAdapter.size() - 1, 1);
                mLastRowAdapter = null;
            }
            return;
        }

        if (mLastRowAdapter == null) {
            // No "Add channel" UI exists and an unpublished channel is found.
            // Will add this action button to the UI.
            mLastRowAdapter = new ArrayObjectAdapter(mPresenterSelector.mAddChannelPresenter);
            mLastRowAdapter.add(mPlaylists.get(mNextChannelIndexToPublish));
            mRowsAdapter.add(new ListRow(mLastRowAdapter));
        } else {
            // The "Add channel" UI already exists and a candidate unpublished channel is found.
            // Update the adapter entry for this action button in order to display this new
            // unpublished channel.
            mLastRowAdapter.replace(0, mPlaylists.get(mNextChannelIndexToPublish));
            mRowsAdapter.notifyItemRangeChanged(mRowsAdapter.size() - 1, 1);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        prepareBackgroundManager();
        setupUIElements();
        setupEventListeners();
        mBackgroudUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mBackgroundURI != null) {
                    updateBackground(mBackgroundURI.toString());
                } else {
                    mBackgroundManager.setDrawable(null);
                }
            }
        };
        // Retrieve the list of playlists from the server in a background thread.
        SampleClipApi.getPlaylists(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        mStarted = true;
        // Use onStart to load the list of published channels. onCreate is not suitable for this
        // purpose as the list of published channels could be different as a result of user's
        // interaction with the launcher between when "home button" is pressed
        // (onStop called but onDestroy is not called) and the app starts again (where onCreate is
        // not called).
        loadPublishedChannelsIfReady();
    }

    @Override
    public void onStop() {
        super.onStop();
        mStarted = false;
        cleanUpLoadChannelsTask();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SampleClipApi.cancelGetPlaylists(this);
        mHandler.removeCallbacks(mBackgroudUpdateRunnable);
    }

    /**
     * Processes results of adding a channel by "startActivityForResult".
     *
     * @param requestCode The request code passed to "startActivityForResult" to identify this
     *                    request.
     * @param resultCode  The result of adding the channel. If the channel was added this value
     *                    will
     *                    be "RESULT_OK".
     * @param data        Not used.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_CHANNEL_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (DEBUG) {
                    Log.d(TAG, "channel added");
                }
                cleanUpLoadChannelsTask();
                mLoadPublishedChannelsTask = new LoadPublishedChannels(getActivity(), this)
                        .execute();
            } else {
                Log.e(TAG, "could not add channel");
            }
        }
    }

    private void loadRows() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        SampleContentDb sampleContentDb = SampleContentDb.getInstance(getActivity());
        mPresenterSelector = new ClipPresenterSelector(getContext());
        mPresenterSelector.mAddChannelPresenter
                .setOnButtonClickedListener(new AddChannelPresenter.OnButtonClickedListener() {
                    @Override
                    public void onButtonClicked(Playlist playlist) {
                        new AddChannelInBackground().execute(playlist);
                    }
                });
        for (int i = 0; i < mPlaylists.size(); i++) {
            Playlist playlist = mPlaylists.get(i);
            List<Clip> clips = playlist.getClips();
            for (int j = 0; j < clips.size(); ++j) {
                if (sampleContentDb.isClipRemoved(clips.get(j).getClipId())) {
                    clips.remove(j);
                    --j;
                }
            }
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(
                    mPresenterSelector.mCardPresenter);
            for (int j = 0; j < clips.size(); ++j) {
                listRowAdapter.add(clips.get(j));
            }
            HeaderItem header = new HeaderItem(i, playlist.getName());
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }
        setAdapter(mRowsAdapter);
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), TvSearchActivity.class));
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    protected void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .into(new SimpleTarget<Drawable>(width, height) {
                    @Override
                    public void onResourceReady(Drawable resource,
                            Transition<? super Drawable> glideAnimation) {
                        mBackgroundManager.setDrawable(resource);
                    }
                });
        mHandler.removeCallbacks(mBackgroudUpdateRunnable);
    }

    private void startBackgroundTimer() {
        mHandler.removeCallbacks(mBackgroudUpdateRunnable);
        mHandler.postDelayed(mBackgroudUpdateRunnable, BACKGROUND_UPDATE_DELAY);
    }

    private static final class ClipPresenterSelector extends PresenterSelector {
        final AddChannelPresenter mAddChannelPresenter;
        CardPresenter mCardPresenter;

        ClipPresenterSelector(Context context) {
            mAddChannelPresenter = new AddChannelPresenter(context);
            mCardPresenter = new CardPresenter();
        }

        public Presenter getPresenter(Object item) {
            if (item instanceof Clip) {
                return mCardPresenter;
            } else {
                return mAddChannelPresenter;
            }
        }
    }

    private final static class ClipData {
        long programId;
        int viewCount;

        ClipData(long programId, int viewCount) {
            this.programId = programId;
            this.viewCount = viewCount;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ClipData)) {
                return false;
            } else {
                ClipData other = (ClipData) obj;
                return programId == other.programId && viewCount == other.viewCount;
            }
        }

        public int hashCode() {
            return 17 + (int) (programId ^ (programId >>> 32)) + viewCount;
        }
    }

    /**
     * Add a playlist as a channel on a background thread, since adding a channel can potentially
     * block. See in "MainFragment" for how the result of the "startActivityForResult" call is
     * processed.
     */
    private final class AddChannelInBackground extends AsyncTask<Playlist, Void, Long> {
        @Override
        protected Long doInBackground(Playlist... params) {
            return SampleTvProvider.addChannel(getActivity(), params[0]);
        }

        @Override
        protected void onPostExecute(Long channelId) {
            Intent intent = new Intent(TvContract.ACTION_REQUEST_CHANNEL_BROWSABLE);
            intent.putExtra(TvContractCompat.EXTRA_CHANNEL_ID, channelId);
            try {
                startActivityForResult(intent, ADD_CHANNEL_REQUEST);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "could not start add channel approval UI", e);
            }
        }
    }

    private final class SetViewCountInBackground extends AsyncTask<Void, Void, Void> {
        private long programId;
        private int viewCount;

        SetViewCountInBackground(long programId, int viewCount) {
            this.programId = programId;
            this.viewCount = viewCount;
            execute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            SampleTvProvider.setProgramViewCount(getActivity(), programId, viewCount);
            return null;
        }
    }

    /**
     * Remove a channel on a background thread, since adding a channel can potentially
     * block.
     */
    private final class RemoveChannelInBackground extends AsyncTask<Playlist, Void, Void> {
        @Override
        protected Void doInBackground(Playlist... params) {
            Playlist playlist = params[0];
            SampleTvProvider.deleteChannel(getActivity(), playlist.getChannelId());
            return null;
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (isAdded()) {
                if (item instanceof Clip) {
                    Clip clip = (Clip) item;
                    Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                    intent.putExtra(PlaybackActivity.EXTRA_CLIP, clip);
                    startActivity(intent);
                    final long programId = clip.getProgramId();
                    if (programId != 0) {
                        // This clip is published as a program. Increment the view count for the
                        // program to demonstrate updating.
                        new SetViewCountInBackground(programId, clip.incrementViewCount());
                    }
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {

        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Clip) {
                mBackgroundURI = ((Clip) item).getBackgroundImageURI();
                startBackgroundTimer();
            } else if (item instanceof Playlist) {
                // Remove the background when the "Add channel" UI is selected.
                mBackgroundURI = null;
                startBackgroundTimer();
            }
        }
    }
}
