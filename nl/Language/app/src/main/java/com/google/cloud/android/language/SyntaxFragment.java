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

import com.google.cloud.android.language.model.TokenInfo;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class SyntaxFragment extends Fragment {

    private static final String ARG_TOKENS = "tokens";

    private TokensAdapter mAdapter;

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
        return inflater.inflate(R.layout.fragment_syntax, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
        final Context context = getContext();
        list.setLayoutManager(new LinearLayoutManager(context));
        TokenInfo[] tokens = (TokenInfo[]) getArguments().getParcelableArray(ARG_TOKENS);
        mAdapter = new TokensAdapter(tokens);
        list.setAdapter(mAdapter);
    }

    public void setTokens(TokenInfo[] tokens) {
        mAdapter.setTokens(tokens);
        getArguments().putParcelableArray(ARG_TOKENS, tokens);
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView text;
        public final TextView label;
        public final TextView partOfSpeech;

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_token, parent, false));
            text = (TextView) itemView.findViewById(R.id.text);
            label = (TextView) itemView.findViewById(R.id.label);
            partOfSpeech = (TextView) itemView.findViewById(R.id.part_of_speech);
        }

    }

    private static class TokensAdapter extends RecyclerView.Adapter<ViewHolder> {

        private TokenInfo[] mTokens;

        public TokensAdapter(TokenInfo[] tokens) {
            mTokens = tokens;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final TokenInfo token = mTokens[position];
            holder.text.setText(token.text);
            holder.label.setText(token.label != null ? token.label.toLowerCase() : null);
            holder.partOfSpeech.setText(token.partOfSpeech);
        }

        @Override
        public int getItemCount() {
            return mTokens == null ? 0 : mTokens.length;
        }

        public void setTokens(TokenInfo[] tokens) {
            mTokens = tokens;
            notifyDataSetChanged();
        }

    }

}
