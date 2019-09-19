package com.google.cloud.examples.speechtospeech.ui;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.translate.AppController;
import com.example.translate.R;
import com.example.translate.adapter.ChatRecyclerViewAdapter;
import com.example.translate.model.ChatMsgModel;
import com.example.translate.utils.ApiRequest;
import com.example.translate.utils.AuthUtils;

import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static ChatRecyclerViewAdapter chatRecyclerViewAdapter;
    private static ArrayList<ChatMsgModel> chatMsgModels;
    private static RecyclerView rvChats;
    private ApiRequest apiRequest;

    private EditText etMsg;
    private ImageButton btnSend;
    private ImageButton btnMic;
    private AlertDialog alert;
    private String message = "";
    private String fileName = "";

    private String sourceLanguageCode;
    private String targetLanguageCode;
    private int ssmlGenderValue;

    /**
     * Broadcast receiver to hide the progress dialog when token is received
     */
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (alert != null && alert.isShowing()) {
                alert.dismiss();
            }
            if (intent.getStringExtra("type").equals("token")) {
                if (!TextUtils.isEmpty(fileName)) {
                    sendMsg("", fileName);
                } else if (!TextUtils.isEmpty(message)) {
                    sendMsg(message, "");
                } else if (!etMsg.getText().toString().trim().equals("")) {
                    sendMsg(etMsg.getText().toString().trim(), "");
                }
            } else if (intent.getStringExtra("type").equals("addMsg")) {
                addMsg(intent.getStringExtra("message"), intent.getIntExtra("messageType", 0));
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
    public static void addMsg(String msg, int type) {
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
        setContentView(R.layout.activity_main);

        sourceLanguageCode = getIntent().getStringExtra("sourceLanguageCode");
        targetLanguageCode = getIntent().getStringExtra("targetLanguageCode");
        ssmlGenderValue = getIntent().getIntExtra("ssmlGenderValue", 0);

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
                sendMsg(etMsg.getText().toString(), "");
                scrollToBottom();
            }
        });

        btnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordAudio("temp.mp3");
            }
        });
    }


    private void getNewToken() {
        showProgressDialog();
        if (AuthUtils.checkSignIn()) {
            AuthUtils.callFirebaseFunction();
        }
    }

    /**
     * function to send the message
     *
     * @param msg :   message sent from user
     */
    private void sendMsg(String msg, String fileName) {
        if (!TextUtils.isEmpty(msg) || !TextUtils.isEmpty(fileName)) {
            // check if the token is received and expiry time is received and not expired
            if (AuthUtils.expiryTime != null && !AuthUtils.token.equals("") && AuthUtils.expiryTime.getTime() > System.currentTimeMillis()) {
                if (!TextUtils.isEmpty(msg)) {
                    addMsg(msg, 1);
                }
                new APIRequest(AuthUtils.token, AuthUtils.expiryTime, msg, fileName, sourceLanguageCode, targetLanguageCode, ssmlGenderValue).execute();
                etMsg.setText("");
                this.message = "";
                this.fileName = "";
            } else {
                // get new token if expired or not received
                getNewToken();
            }
        } else {
            Toast.makeText(this, "Please enter or say some message to send.", Toast.LENGTH_SHORT).show();
        }
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

    private void recordAudio(final String fileName) {
        final MediaRecorder recorder = new MediaRecorder();
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, fileName);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioSamplingRate(16000);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(Environment.getExternalStorageDirectory() + "/" + fileName);
        try {
            recorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Recording");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setButton("Stop recording", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mProgressDialog.dismiss();
                recorder.stop();
                recorder.release();
                sendMsg("", Environment.getExternalStorageDirectory() + "/" + fileName);
            }
        });

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface p1) {
                recorder.stop();
                recorder.release();
            }
        });
        recorder.start();
        mProgressDialog.show();
    }

    private class APIRequest extends AsyncTask<Void, Void, String> {
        private String token;
        private Date expiryTime;
        private String msg;
        private String fileName;
        private String sourceLanguageCode;
        private String targetLanguageCode;
        private int ssmlGenderValue;

        public APIRequest(String token, Date expiryTime, String msg, String fileName, String sourceLanguageCode, String targetLanguageCode, int ssmlGenderValue) {
            this.token = token;
            this.expiryTime = expiryTime;
            this.msg = msg;
            this.fileName = fileName;
            this.sourceLanguageCode = sourceLanguageCode;
            this.targetLanguageCode = targetLanguageCode;
            this.ssmlGenderValue = ssmlGenderValue;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return apiRequest.callAPI(token, expiryTime, msg, fileName, sourceLanguageCode, targetLanguageCode, ssmlGenderValue);
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if (response != null) {
                addMsg(response, 0);
            }
        }
    }

}
