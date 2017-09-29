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

import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.SearchFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.google.android.tvhomescreenchannels.presenters.CardPresenter;

public class TvSearchFragment extends SearchFragment
        implements SearchFragment.SearchResultProvider {

    private static final int SEARCH_DELAY_MS = 300;
    private ArrayObjectAdapter mRowsAdapter;
    private Handler mHandler = new Handler();
    private SearchRunnable mDelayedLoad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);
        setOnItemViewClickedListener(getDefaultItemClickedListener());
        mDelayedLoad = new SearchRunnable();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    private void searchFor(String query) {
        mRowsAdapter.clear();
        if (!TextUtils.isEmpty(query)) {
            mDelayedLoad.setSearchQuery(query);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        searchFor(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchFor(query);
        return true;
    }

    private OnItemViewClickedListener getDefaultItemClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof Clip) {
                    Toast.makeText(getActivity(), "** toggle selected **",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), "** add as playlist **",
                            Toast.LENGTH_LONG).show();

                }
            }
        };
    }

    private static final class AddPlaylistButton {
        // no class contents
    }

    private static final class AddPlaylistButtonPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View v = inflater.inflate(R.layout.add_playlist, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

    private static final class SearchPresenterSelector extends PresenterSelector {
        private final CardPresenter mCardPresenter = new CardPresenter();
        private final AddPlaylistButtonPresenter mAddChannelButtonPresenter =
                new AddPlaylistButtonPresenter();

        public Presenter getPresenter(Object item) {
            if (item instanceof Clip) {
                return mCardPresenter;
            } else {
                return mAddChannelButtonPresenter;
            }
        }
    }

    // This is a fake query for now and search results come from PlayLis
    private class SearchRunnable implements Runnable {
        private String mQuery;

        void setSearchQuery(String query) {
            mQuery = query;
        }

        @Override
        public void run() {
            // Search for query and collate results.
            mRowsAdapter.clear();
            ArrayObjectAdapter searchRowAdapter =
                    new ArrayObjectAdapter(new SearchPresenterSelector());
            searchRowAdapter.add(new AddPlaylistButton());
            searchRowAdapter.addAll(1, SampleClipApi.getSearchResults(mQuery));
            HeaderItem header =
                    new HeaderItem(0, getResources().getString(R.string.search_results));
            mRowsAdapter.add(new ListRow(header, searchRowAdapter));

        }
    }

}