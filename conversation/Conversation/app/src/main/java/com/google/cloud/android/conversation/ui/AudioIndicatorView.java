/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.android.conversation.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.google.cloud.android.conversation.R;


/**
 * Shows microphone icon indicating that the app is listening to audio.
 */
public class AudioIndicatorView extends AppCompatImageView {

    private final int mColorNormal;
    private final int mColorHearingVoice;

    private boolean mHearingVoice;
    private ObjectAnimator mAnimator;

    public AudioIndicatorView(Context context) {
        this(context, null);
    }

    public AudioIndicatorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AudioIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setImageResource(R.drawable.ic_mic);
        mColorNormal = ContextCompat.getColor(context, R.color.input_button);
        mColorHearingVoice = ContextCompat.getColor(context, R.color.accent);
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(mColorNormal));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimating();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mHearingVoice) {
            stopAnimating();
        }
        super.onDetachedFromWindow();
    }

    public void setHearingVoice(boolean hearingVoice) {
        if (mHearingVoice == hearingVoice) {
            return;
        }
        mHearingVoice = hearingVoice;
        if (hearingVoice) {
            stopAnimating();
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(mColorHearingVoice));
        } else {
            startAnimating();
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(mColorNormal));
        }
    }

    public boolean isHearingVoice() {
        return mHearingVoice;
    }

    private void startAnimating() {
        mAnimator = ObjectAnimator.ofFloat(this, View.ALPHA, 1.f, 0.3f);
        mAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        mAnimator.setDuration(1000);
        mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimator.start();
    }

    private void stopAnimating() {
        if (mAnimator != null) {
            mAnimator.end();
            setAlpha(1.0f);
            mAnimator = null;
        }
    }

}
