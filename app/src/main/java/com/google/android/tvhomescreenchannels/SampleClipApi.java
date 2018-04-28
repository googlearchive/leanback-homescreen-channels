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

import android.os.AsyncTask;
import android.support.annotation.WorkerThread;
import android.support.media.tv.TvContractCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SampleClipApi {

    private static final int YOUTUBE_PLAYLIST_START_INDEX = 6;
    private static final String PLAY_LIST_NAMES[] = {
            "Dog videos",
            "Cat videos",
            "Beaches and rivers",
            "Nature videos",
            "Birds of prey",
            "Chickens in trees",
            "Youtube videos",
    };
    private static final int YOUTUBE_VIDEO_START_INDEX = 5;
    private static final String VIDEO_TITLES[] = {
            "Zeitgeist 2010 - Year in Review",
            "Google Demo Slam - 20ft Search",
            "Introducing Gmail Blue",
            "Introducing Google Fiber to the Pole",
            "Introducing Google Nose",
            "YouTube Developers Live: Embedded Web Player Customization",
            "Homer goes to College",
            "How Beauty and the Beast Should Have Ended",
            "Motivation Archive",
    };
    private static final String VIDEO_DESCRIPTION = "Lorem ipsum dolor sit amet.";
    private static final String VIDEO_URLS[] = {
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in"
                    + "%20Review.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft"
                    + "%20Search.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail"
                    + "%20Blue.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Fiber%20to%20the%20Pole.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Nose.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail"
                    + "%20Blue.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Fiber%20to%20the%20Pole.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Nose.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft"
                    + "%20Search.mp4",
    };
    private static final String PREVIEW_VIDEO_URLS[] = {
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in"
                    + "%20Review.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft"
                    + "%20Search.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail"
                    + "%20Blue.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Fiber%20to%20the%20Pole.mp4",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Nose.mp4",
            "https://www.youtube.com/watch?v=M7lc1UVf-VE",
            "https://www.youtube.com/watch?v=k-LCw4CGIV8",
            "https://www.youtube.com/watch?v=8hm9ezomDhQ",
            "https://www.youtube.com/watch?v=QygpaIJclm4",
    };
    private static final String BG_IMAGE_URLS[] = {
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in"
                    + "%20Review/bg.jpg",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft"
                    + "%20Search/bg.jpg",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail"
                    + "%20Blue/bg.jpg",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Fiber%20to%20the%20Pole/bg.jpg",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Nose/bg.jpg",
            "https://img.youtube.com/vi/M7lc1UVf-VE/maxresdefault.jpg",
            "https://img.youtube.com/vi/k-LCw4CGIV8/maxresdefault.jpg",
            "https://img.youtube.com/vi/8hm9ezomDhQ/maxresdefault.jpg",
            "https://img.youtube.com/vi/QygpaIJclm4/maxresdefault.jpg",
    };
    private static final String CARD_IMAGE_URLS[] = {
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in"
                    + "%20Review/card.jpg",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft"
                    + "%20Search/card.jpg",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail"
                    + "%20Blue/card.jpg",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Fiber%20to%20the%20Pole/card.jpg",
            "http://commondatastorage.googleapis"
                    + ".com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google"
                    + "%20Nose/card.jpg",
            "https://img.youtube.com/vi/M7lc1UVf-VE/hqdefault.jpg",
            "https://img.youtube.com/vi/k-LCw4CGIV8/hqdefault.jpg",
            "https://img.youtube.com/vi/8hm9ezomDhQ/hqdefault.jpg",
            "https://img.youtube.com/vi/QygpaIJclm4/hqdefault.jpg",
    };
    private static int mNextAspectRatio = TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_16_9;
    private static List<Playlist> mPlaylists;
    /*
     * Generate a repeatable random sequence. The seed values must be non-zero, and these
     * particular values are hand chosen to give a pleasing sequence for "numberOfVideos".
     */
    private static int mSeedZ = 11;
    private static int mSeedW = 15;

    private SampleClipApi() {
    }

    private static int getNextAspectRatio() {
        if (mNextAspectRatio == TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_16_9) {
            mNextAspectRatio = TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_3_2;
        } else if (mNextAspectRatio == TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_3_2) {
            mNextAspectRatio = TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_1_1;
        } else if (mNextAspectRatio == TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_1_1) {
            mNextAspectRatio = TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_4_3;
        } else if (mNextAspectRatio == TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_4_3) {
            mNextAspectRatio = TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_16_9;
        }
        return mNextAspectRatio;
    }

    private static int getNumber() {
        mSeedZ = 36969 * (mSeedZ & 65535) + (mSeedZ >> 16);
        mSeedW = 18000 * (mSeedW & 65535) + (mSeedW >> 16);
        int number = (mSeedZ << 16) + mSeedW;
        if (number < 0) {
            return -number;
        } else {
            return number;
        }
    }

    private static void populatePlaylists() {
        if (mPlaylists == null) {
            mPlaylists = new ArrayList<>();
            int videoId = 0;
            int clipId = 1;
            int playlistId = 1;
            final int numberOfDemoChannels = PLAY_LIST_NAMES.length;
            for (int i = 0; i < numberOfDemoChannels; ++i, ++playlistId) {
                int numberOfVideos = getNumber() % 5 + 2;
                List<Clip> videos = new ArrayList<>();
                for (int j = 0; j < numberOfVideos; ++j, ++videoId, ++clipId) {
                    int videoIndex = playlistId <= YOUTUBE_PLAYLIST_START_INDEX
                            ? videoId % YOUTUBE_VIDEO_START_INDEX
                            : videoId % (VIDEO_TITLES.length - YOUTUBE_VIDEO_START_INDEX)
                                    + YOUTUBE_VIDEO_START_INDEX;
                    // Mocking protected videos for half of the playlist.
                    boolean isVideoProtected = clipId % 2 == 0;
                    videos.add(new Clip(VIDEO_TITLES[videoIndex], VIDEO_DESCRIPTION,
                            BG_IMAGE_URLS[videoIndex], CARD_IMAGE_URLS[videoIndex],
                            VIDEO_URLS[videoIndex], PREVIEW_VIDEO_URLS[videoIndex],
                            isVideoProtected, "category",
                            Integer.toString(clipId), Integer.toString(videoIndex),
                            getNextAspectRatio()));
                }
                Collections.shuffle(videos);
                mPlaylists.add(
                        new Playlist(PLAY_LIST_NAMES[i], videos, Integer.toString(playlistId)));
            }
        }
    }

    /**
     * In a real application this call could block the UI thread and so should be implemented with
     * completion callback. This is simulated here with an AsyncTask.
     */
    static void getPlaylists(GetPlaylistsListener getPlaylistsListener) {
        populatePlaylists();
        new SimulateGetPlaylistsTask(getPlaylistsListener, mPlaylists).execute();
    }

    static void cancelGetPlaylists(GetPlaylistsListener getPlaylistsListener) {
        // do nothing for now
    }

    /**
     * Load playlists and block if necessary since this will only be called from background task.
     */
    @WorkerThread
    static List<Playlist> getPlaylistBlocking() {
        populatePlaylists();
        return mPlaylists;
    }

    /**
     * In a real application this call could block the UI thread and so should be implemented with
     * completion callback. This sample does not block, so the call back mechanism is simulated.
     */
    static void getClipById(String clipId, GetClipByIdListener getClipByIdListener) {
        populatePlaylists();
        Clip clip = null;
        for (int i = 0; i < mPlaylists.size() && clip == null; ++i) {
            List<Clip> clips = mPlaylists.get(i).getClips();
            for (Clip candidateClip : clips) {
                if (TextUtils.equals(candidateClip.getClipId(), clipId)) {
                    clip = candidateClip;
                    break;
                }
            }
        }
        new SimulateGetClipByIdTask(getClipByIdListener, clip).execute();
    }

    static void cancelGetClipById(GetClipByIdListener getClipByIdListener) {
        // do nothing for now
    }

    @WorkerThread
    static Clip getClipByIdBlocking(String clipId) {
        populatePlaylists();
        for (Playlist playlist : mPlaylists) {
            List<Clip> clips = playlist.getClips();
            for (Clip candidateClip : clips) {
                if (TextUtils.equals(candidateClip.getClipId(), clipId)) {
                    return candidateClip;
                }
            }
        }
        return null;
    }

    static List<Clip> getSearchResults(String query) {
        int numberOfVideos = (int) (Math.random() * 4 + 2);
        int videoIndex = 0;
        List<Clip> videos = new ArrayList<>();
        int clipId = 1;
        for (int j = 0; j < numberOfVideos; ++j,
                videoIndex = (videoIndex + 1) % VIDEO_TITLES.length, ++clipId) {
            boolean isVideoProtected = clipId % 2 == 0; // Mocking protected videos for even videos.
            videos.add(new Clip(VIDEO_TITLES[videoIndex], VIDEO_DESCRIPTION,
                    BG_IMAGE_URLS[videoIndex], CARD_IMAGE_URLS[videoIndex], VIDEO_URLS[videoIndex],
                    PREVIEW_VIDEO_URLS[videoIndex], isVideoProtected, "category",
                    Integer.toString(clipId), Integer.toString(videoIndex), getNextAspectRatio()));
        }
        return videos;
    }

    /**
     * Return the desired list of published channels and programs. The app will update the set of
     * published channels and programs to align with this list.
     */
    static List<Playlist> getDesiredPublishedChannelSet() {
        populatePlaylists();
        List<Playlist> list = new ArrayList<>();
        // For now, arbitrarily pick the first three channels.
        for (int i = 0; i < 3 && i < mPlaylists.size(); ++i) {
            list.add(mPlaylists.get(i));
        }
        return list;
    }

    static Playlist getPlaylistById(String playlistId) {
        for (Playlist playlist : mPlaylists) {
            if (TextUtils.equals(playlist.getPlaylistId(), playlistId)) {
                return playlist;
            }
        }
        return null;
    }

    interface GetClipByIdListener {
        void onGetClipById(Clip clip);
    }

    interface GetPlaylistsListener {
        void onGetPlaylists(List<Playlist> playlists);
    }

    private static class SimulateGetClipByIdTask extends AsyncTask<Void, Void, Void> {
        private GetClipByIdListener mGetClipByIdListener;
        private Clip mClip;

        SimulateGetClipByIdTask(GetClipByIdListener getClipByIdListener, Clip clip) {
            mGetClipByIdListener = getClipByIdListener;
            mClip = clip;
        }

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mGetClipByIdListener.onGetClipById(mClip);
        }
    }

    private static class SimulateGetPlaylistsTask extends AsyncTask<Void, Void, Void> {
        private GetPlaylistsListener mGetPlaylistsListener;
        private List<Playlist> mPlaylists;

        SimulateGetPlaylistsTask(GetPlaylistsListener getPlaylistsListener,
                List<Playlist> playlists) {
            mGetPlaylistsListener = getPlaylistsListener;
            mPlaylists = playlists;
        }

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mGetPlaylistsListener.onGetPlaylists(mPlaylists);
        }
    }
}
