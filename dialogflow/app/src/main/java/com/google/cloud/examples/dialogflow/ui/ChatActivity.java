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

package com.google.cloud.examples.dialogflow.ui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.cloud.examples.dialogflow.AppController;
import com.google.cloud.examples.dialogflow.R;
import com.google.cloud.examples.dialogflow.adapter.ChatRecyclerViewAdapter;
import com.google.cloud.examples.dialogflow.model.ChatMsgModel;
import com.google.cloud.examples.dialogflow.utils.ApiRequest;
import com.google.cloud.examples.dialogflow.utils.AuthUtils;

import java.util.ArrayList;
import java.util.Date;

public class ChatActivity extends AppCompatActivity {

    private static ChatRecyclerViewAdapter chatRecyclerViewAdapter;
    private static ArrayList<ChatMsgModel> chatMsgModels;
    private static RecyclerView rvChats;
    private ApiRequest apiRequest;

    private EditText etMsg;
    private ImageButton btnSend;
    private ImageButton btnMic;
    private AlertDialog alert;

    private ImageButton ibMore;

    private boolean tts = false;
    private boolean knowledge = false;
    private boolean sentiment = false;

    private String voiceInput = "";

    /**
     * Broadcast receiver to hide the progress dialog when token is received
     */
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            alert.dismiss();
            if (!voiceInput.equals("")) {
                sendMsg(voiceInput);
            } else if (!etMsg.getText().toString().trim().equals("")) {
                sendMsg(etMsg.getText().toString().trim());
            }
        }
    };

    /**
     * function to scroll the recyclerview at the bottom after each message sent or received
     */
    private static void scrollToBottom() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (chatMsgModels.size() > 0) {
                    rvChats.smoothScrollToPosition(chatMsgModels.size() - 1);
                }
            }
        });
    }

    /**
     * function to addMessage in the recyclerview
     *
     * @param msg  : message to add
     * @param type : Type of message (sent|received)
     */
    private void addMsg(String msg, int type) {
        chatMsgModels.add(new ChatMsgModel(msg, type));
        chatRecyclerViewAdapter.notifyDataSetChanged();
        scrollToBottom();
    }

    /**
     * function to show the progress dialog
     */
    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Fetching auth token...");
        builder.setCancelable(false);

        alert = builder.create();
        alert.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (AppController.PROJECT_ID.equals("GCP_PROJECT_ID")) {
            Toast.makeText(this, "Please update the GCP_PROJECT_ID in strings.xml", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        checkPermissions();

        AuthUtils.signInAnonymously(this);
        AuthUtils.getFirebaseInstanceId();

        initViews();
        setupRecyclerView();
        initListeners();

        apiRequest = new ApiRequest();

    }

    /**
     * function to initialize the views
     */
    private void initViews() {
        etMsg = findViewById(R.id.etMsg);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        rvChats = findViewById(R.id.rvChat);
        ibMore = findViewById(R.id.ibMore);
    }

    /**
     * function to initialize the recyclerview
     */
    private void setupRecyclerView() {
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        chatMsgModels = new ArrayList<>();

        chatRecyclerViewAdapter = new ChatRecyclerViewAdapter(chatMsgModels);
        rvChats.setAdapter(chatRecyclerViewAdapter);
    }

    /**
     * function to initialize the onClick listeners
     */
    private void initListeners() {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AuthUtils.checkSignIn()) {
                    sendMsg(etMsg.getText().toString());
                    scrollToBottom();
                } else {
                    AuthUtils.signInAnonymously(ChatActivity.this);
                }
            }
        });

        btnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AuthUtils.checkSignIn()) {
                    promptSpeechInput();
                }
            }
        });

        ibMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMorePopup();
            }
        });
    }


    /**
     * function to send the message
     *
     * @param msg :   message sent from user
     */
    private void sendMsg(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            // check if the token is received and expiry time is received and not expired
            if (AuthUtils.isTokenValid()) {
                addMsg(msg, 1);
                etMsg.setText("");
                voiceInput = "";
                new APIRequest(AuthUtils.token, AuthUtils.expiryTime, msg, tts, sentiment, knowledge).execute();
            } else {
                // get new token if expired or not received
                getNewToken();
            }
        } else {
            Toast.makeText(this, "Please enter or say some message to send.", Toast.LENGTH_SHORT).show();
        }
    }

    private void getNewToken() {
        showProgressDialog();
        AuthUtils.callFirebaseFunction();
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1000);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speak");
        try {
            startActivityForResult(intent, 101);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Not Supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 101) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                voiceInput = result.get(0);
                sendMsg(result.get(0));
            }
        }
    }

    public void checkPermissions() {

        ArrayList<String> arrPerm = new ArrayList<>();
        arrPerm.add(Manifest.permission.INTERNET);
        arrPerm.add(Manifest.permission.RECORD_AUDIO);
        arrPerm.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);


        if (!arrPerm.isEmpty()) {
            String[] permissions = new String[arrPerm.size()];
            permissions = arrPerm.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(AppController.TOKEN_RECEIVED);
        registerReceiver(br, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (br != null) {
            try {
                unregisterReceiver(br);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void showMorePopup() {
        PopupMenu popup = new PopupMenu(this, ibMore);
        popup.inflate(R.menu.main_menu);
        popup.getMenu().findItem(R.id.action_tts).setChecked(tts);
        popup.getMenu().findItem(R.id.action_sentiment).setChecked(sentiment);
        popup.getMenu().findItem(R.id.action_knowledge).setChecked(knowledge);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.action_tts:
                        tts = !tts;
                        item.setChecked(!item.isChecked());
                        break;
                    case R.id.action_sentiment:
                        sentiment = !sentiment;
                        item.setChecked(!item.isChecked());
                        break;
                    case R.id.action_knowledge:
                        knowledge = !knowledge;
                        item.setChecked(!item.isChecked());
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    private class APIRequest extends AsyncTask<Void, Void, String> {
        private String token;
        private Date expiryTime;
        private String msg;
        private boolean tts;
        private boolean sentiment;
        private boolean knowledge;

        public APIRequest(String token, Date expiryTime, String msg, boolean tts, boolean sentiment, boolean knowledge) {
            this.token = token;
            this.expiryTime = expiryTime;
            this.msg = msg;
            this.tts = tts;
            this.sentiment = sentiment;
            this.knowledge = knowledge;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return apiRequest.callAPI(token, expiryTime, msg, tts, sentiment, knowledge);
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            addMsg(response, 0);
        }
    }

}
