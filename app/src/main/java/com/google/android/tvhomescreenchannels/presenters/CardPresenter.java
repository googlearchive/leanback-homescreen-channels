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
import android.content.res.Resources;
import android.support.media.tv.TvContractCompat;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.android.tvhomescreenchannels.Clip;
import com.google.android.tvhomescreenchannels.R;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an ImageCardView to render each clip within each playlist row.
 */
public class CardPresenter extends Presenter {
    private static int sSelectedBackgroundColor;
    private static int sDefaultBackgroundColor;

    private static void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? sSelectedBackgroundColor : sDefaultBackgroundColor;
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    ;

    private static float getWidthMultplier(int aspectRatio) {
        switch (aspectRatio) {
            case TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_16_9:
                return 16.0f / 9.0f;
            case TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_3_2:
                return 3.0f / 2.0f;
            case TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_2_3:
                return 2.0f / 3.0f;
            case TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_4_3:
                return 4.0f / 3.0f;
            case TvContractCompat.PreviewProgramColumns.ASPECT_RATIO_1_1:
            default:
                return 1.0f;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();
        sDefaultBackgroundColor = ContextCompat.getColor(context, R.color.default_background);
        sSelectedBackgroundColor = ContextCompat.getColor(context, R.color.selected_background);

        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new CardViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Clip clip = (Clip) item;
        if (clip.getCardImageUrl() != null) {
            ImageCardView cardView = (ImageCardView) viewHolder.view;
            Resources resources = cardView.getContext().getResources();
            cardView.setTitleText(clip.getTitle());
            cardView.setContentText(clip.getDescription());
            float widthMultiplier = getWidthMultplier(clip.getAspectRatio());
            int cardWidth = Math.round(resources.getDimensionPixelSize(R.dimen.card_width)
                    * widthMultiplier);
            int cardHeight = resources.getDimensionPixelSize(R.dimen.card_height);
            cardView.setMainImageDimensions(cardWidth, cardHeight);
            Glide.with(viewHolder.view.getContext())
                    .load(clip.getCardImageUrl())
                    .into(cardView.getMainImageView());
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }

    public static final class CardViewHolder extends ViewHolder {

        CardViewHolder(ImageCardView view) {
            super(view);
        }
    }
}
