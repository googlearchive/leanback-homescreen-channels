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

package com.google.android.tvhomescreenchannels.presenters;

import android.content.Context;
import android.support.v17.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.tvhomescreenchannels.Playlist;
import com.google.android.tvhomescreenchannels.R;

/**
 * Renders the "Add channel" action button on the last row. The object bound to the action button is
 * a playlist that corresponds to the next available channel not published yet to the TV provider
 * database.
 */
public class AddChannelPresenter extends Presenter {

    String mDescriptionFormat;
    private OnButtonClickedListener mOnButtonClickedListener;

    public AddChannelPresenter(Context context) {
        mDescriptionFormat = context.getString(R.string.add_channel_description_text);
    }

    static void updateSelectedStatus(AddChannelViewHolder viewHolder, boolean selected) {
        if (selected) {
            viewHolder.mDescription.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mDescription.setVisibility(View.GONE);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_channels_row,
                parent, false);

        return new AddChannelViewHolder(v, mOnButtonClickedListener);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        final Playlist playlist = (Playlist) item;
        AddChannelViewHolder addChannelViewHolder = (AddChannelViewHolder) viewHolder;
        String descriptionText = String.format(mDescriptionFormat, playlist.getName());
        addChannelViewHolder.mDescription.setText(descriptionText);
        if (addChannelViewHolder.mOnButtonClickedListener != null) {
            addChannelViewHolder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnButtonClickedListener != null) {
                        mOnButtonClickedListener.onButtonClicked(playlist);
                    }
                }
            });
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {

    }

    public void setOnButtonClickedListener(OnButtonClickedListener buttonClickedListener) {
        mOnButtonClickedListener = buttonClickedListener;
    }

    /**
     * Listener to call when the "Add channel" action button is clicked.
     */
    public interface OnButtonClickedListener {
        /**
         * Called when the "Add channel" action button is clicked.
         *
         * @param playlist The playlist bound to this action button.
         */
        void onButtonClicked(Playlist playlist);
    }

    public static class AddChannelViewHolder extends Presenter.ViewHolder {
        TextView mButton;
        TextView mDescription;
        OnButtonClickedListener mOnButtonClickedListener;

        AddChannelViewHolder(final View view, final OnButtonClickedListener buttonClickedListener) {
            super(view);
            mButton = view.findViewById(R.id.button);
            mDescription = view.findViewById(R.id.description);
            mButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    updateSelectedStatus(AddChannelViewHolder.this, hasFocus);
                }
            });
            mOnButtonClickedListener = buttonClickedListener;
        }
    }
}
