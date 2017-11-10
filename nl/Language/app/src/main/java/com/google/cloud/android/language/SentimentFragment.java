/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.android.language;

import com.google.cloud.android.language.model.SentimentInfo;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class SentimentFragment extends Fragment {

    private static final String ARG_SENTIMENT = "sentiment";

    public static SentimentFragment newInstance() {
        final SentimentFragment fragment = new SentimentFragment();
        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    private int mColorPositive;
    private int mColorNeutral;
    private int mColorNegative;

    private TextView mScore;
    private TextView mMagnitude;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources resources = getResources();
        final Resources.Theme theme = getActivity().getTheme();
        mColorPositive = ResourcesCompat.getColor(resources, R.color.score_positive, theme);
        mColorNeutral = ResourcesCompat.getColor(resources, R.color.score_neutral, theme);
        mColorNegative = ResourcesCompat.getColor(resources, R.color.score_negative, theme);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sentiment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mScore = (TextView) view.findViewById(R.id.score);
        mMagnitude = (TextView) view.findViewById(R.id.magnitude);
        final Bundle args = getArguments();
        if (args.containsKey(ARG_SENTIMENT)) {
            showSentiment((SentimentInfo) args.getParcelable(ARG_SENTIMENT));
        }
    }

    public void setSentiment(SentimentInfo sentiment) {
        showSentiment(sentiment);
        getArguments().putParcelable(ARG_SENTIMENT, sentiment);
    }

    private void showSentiment(SentimentInfo sentiment) {
        mScore.setText(String.valueOf(sentiment.score));
        if (sentiment.score > 0.25) {
            mScore.setBackgroundColor(mColorPositive);
        } else if (sentiment.score > -0.75) {
            mScore.setBackgroundColor(mColorNeutral);
        } else {
            mScore.setBackgroundColor(mColorNegative);
        }
        mMagnitude.setText(String.valueOf(sentiment.magnitude));
    }

}
