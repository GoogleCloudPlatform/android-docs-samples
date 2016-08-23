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

import com.google.android.flexbox.FlexboxLayout;
import com.google.cloud.android.language.model.TokenInfo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class SyntaxFragment extends Fragment {

    private static final String ARG_TOKENS = "tokens";
    private FlexboxLayout mLayout;

    public static SyntaxFragment newInstance() {
        final SyntaxFragment fragment = new SyntaxFragment();
        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_syntax, container, false);
        mLayout = (FlexboxLayout) view.findViewById(R.id.layout);
        TokenInfo[] tokens = (TokenInfo[]) getArguments().getParcelableArray(ARG_TOKENS);
        if (tokens != null) {
            showTokens(tokens);
        }
        return view;
    }

    public void setTokens(TokenInfo[] tokens) {
        showTokens(tokens);
        getArguments().putParcelableArray(ARG_TOKENS, tokens);
    }

    private void showTokens(TokenInfo[] tokens) {
        mLayout.removeAllViews();
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        for (TokenInfo token : tokens) {
            final View view = inflater.inflate(R.layout.item_token, mLayout, false);
            TextView text = (TextView) view.findViewById(R.id.text);
            TextView label = (TextView) view.findViewById(R.id.label);
            TextView partOfSpeech = (TextView) view.findViewById(R.id.part_of_speech);
            text.setText(token.text);
            label.setText(token.label != null ? token.label.toLowerCase() : null);
            partOfSpeech.setText(token.partOfSpeech);
            mLayout.addView(view);
        }
    }

}
