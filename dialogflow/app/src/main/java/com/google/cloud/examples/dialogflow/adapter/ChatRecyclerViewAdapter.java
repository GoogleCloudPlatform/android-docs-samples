/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.examples.dialogflow.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.cloud.examples.dialogflow.R;
import com.google.cloud.examples.dialogflow.model.ChatMsgModel;

import java.util.ArrayList;

public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatRecyclerViewAdapter.MyViewHolder> {

    private ArrayList<ChatMsgModel> chatMsgModels;

    public ChatRecyclerViewAdapter(ArrayList<ChatMsgModel> chatMsgModels) {
        this.chatMsgModels = chatMsgModels;

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_chat_recyclerview_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        final ChatMsgModel chatMsgModel = chatMsgModels.get(position);

        if (chatMsgModel.getType() == 1) { // Message Sent
            holder.tvMsgSent.setText(chatMsgModel.getMsg());
            holder.tvMsgSent.setVisibility(View.VISIBLE);
            holder.tvMsgReceived.setVisibility(View.GONE);
        } else {
            holder.tvMsgReceived.setText(chatMsgModel.getMsg());
            holder.tvMsgReceived.setVisibility(View.VISIBLE);
            holder.tvMsgSent.setVisibility(View.GONE);
        }


    }

    @Override
    public int getItemCount() {
        return chatMsgModels.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout rlMain;
        TextView tvMsgSent;
        TextView tvMsgReceived;

        public MyViewHolder(View view) {
            super(view);
            rlMain = view.findViewById(R.id.rlMain);
            tvMsgSent = view.findViewById(R.id.tvMsgSent);
            tvMsgReceived = view.findViewById(R.id.tvMsgReceived);
        }
    }
}