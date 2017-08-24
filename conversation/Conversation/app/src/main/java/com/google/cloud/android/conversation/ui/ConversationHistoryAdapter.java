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

import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.cloud.android.conversation.R;
import com.google.cloud.android.conversation.api.Utterance;

import java.util.ArrayList;


public class ConversationHistoryAdapter extends
        RecyclerView.Adapter<ConversationHistoryAdapter.ViewHolder> {

    private final ArrayList<Utterance> mHistory = new ArrayList<>();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Utterance utterance = mHistory.get(position);
        holder.bind(utterance.text,
                utterance.direction == Utterance.INCOMING
                        ? BubbleView.DIRECTION_INCOMING
                        : BubbleView.DIRECTION_OUTGOING);
    }

    @Override
    public int getItemCount() {
        return mHistory.size();
    }

    public void addUtterance(Utterance utterance) {
        mHistory.add(utterance);
        notifyItemInserted(mHistory.size() - 1);
    }

    public ArrayList<Utterance> getHistory() {
        return mHistory;
    }

    public void restoreHistory(ArrayList<Utterance> history) {
        mHistory.clear();
        mHistory.addAll(history);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        FrameLayout frame;
        BubbleView bubble;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_conversation, parent, false));
            frame = itemView.findViewById(R.id.frame);
            bubble = itemView.findViewById(R.id.bubble);
        }

        void bind(String message, int direction) {
            bubble.setText(message);
            bubble.setDirection(direction);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) bubble.getLayoutParams();
            if (direction == BubbleView.DIRECTION_INCOMING) {
                lp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
            } else {
                lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
            }
            bubble.setLayoutParams(lp);
        }

    }

}
