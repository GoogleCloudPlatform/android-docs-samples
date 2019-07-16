package com.google.cloud.examples.dialogflow.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.cloud.examples.dialogflow.AppController;
import com.google.cloud.examples.dialogflow.R;
import com.google.cloud.examples.dialogflow.adapter.ChatRecyclerViewAdapter;
import com.google.cloud.examples.dialogflow.model.ChatMsgModel;
import com.google.cloud.examples.dialogflow.utils.ApiRequest;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private static ChatRecyclerViewAdapter chatRecyclerViewAdapter;
    private static ArrayList<ChatMsgModel> chatMsgModels;
    private static RecyclerView rvChats;
    private ApiRequest apiRequest;

    private String type = "";

    private EditText etMsg;
    private ImageButton btnSend;
    private ImageButton btnMic;

    private ProgressDialog progressDialog;

    // Broadcast receiver to hide the progress dialog when token is received
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progressDialog.dismiss();
        }
    };

    // function to addMessage in the recyclerview
    public static void addMsg(String msg, int type) {
        chatMsgModels.add(new ChatMsgModel(msg, type));
        chatRecyclerViewAdapter.notifyDataSetChanged();
        scrollToBottom();
    }

    // function to scroll the recyclerview at the bottom after each message sent or received
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        getSupportActionBar().setTitle("Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        checkPermissions();

        type = this.getIntent().getStringExtra("type");

        initViews();
        setupRecyclerView();
        initListeners();

        apiRequest = new ApiRequest();

    }

    private void initViews() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Waiting for the token");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);


        etMsg = findViewById(R.id.etMsg);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        rvChats = findViewById(R.id.rvChat);
    }

    private void setupRecyclerView() {
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        chatMsgModels = new ArrayList<>();

        chatRecyclerViewAdapter = new ChatRecyclerViewAdapter(this, chatMsgModels);
        rvChats.setAdapter(chatRecyclerViewAdapter);
    }

    private void initListeners() {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsg(type, etMsg.getText().toString());
                scrollToBottom();
            }
        });

        btnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });
    }

    /**
     * function to send the message
     * @param type  :   type of message sentiment | tts | knowledge
     * @param msg   :   message sent from user
     */
    private void sendMsg(String type, String msg) {
        if (!TextUtils.isEmpty(msg)) {
            // check if the token is received and expiry time is received and not expired
            if (AppController.exipryTime != null && !AppController.token.equals("") && AppController.exipryTime.getTime() > System.currentTimeMillis()) {
                addMsg(msg, 1);
                etMsg.setText("");
                apiRequest.callAPI(this, AppController.token, AppController.exipryTime, type, msg);
            } else {
                // get new token if expired or not received
                getNewToken();
            }
        } else {
            Toast.makeText(this, "Please enter or say some message to send.", Toast.LENGTH_SHORT).show();
        }
    }

    private void getNewToken() {
        progressDialog.show();
        AppController.callFirebaseFunction();
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
                sendMsg(type, result.get(0));
            }
        }
    }

    public void checkPermissions() {

        ArrayList<String> arrPerm = new ArrayList<>();
        arrPerm.add(Manifest.permission.INTERNET);
        arrPerm.add(Manifest.permission.RECORD_AUDIO);


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

            }
        }
    }
}
