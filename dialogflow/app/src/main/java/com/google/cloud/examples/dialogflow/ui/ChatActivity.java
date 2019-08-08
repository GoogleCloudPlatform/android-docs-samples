package com.google.cloud.examples.dialogflow.ui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private static TextToSpeech textToSpeech;
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
     * function to addMessage in the recyclerview
     *
     * @param msg           : message to add
     * @param type          : Type of message (sent|received)
     * @param voiceFeedback : Whether to output response as voice
     */
    public static void addMsg(String msg, int type, boolean voiceFeedback) {
        chatMsgModels.add(new ChatMsgModel(msg, type));
        chatRecyclerViewAdapter.notifyDataSetChanged();
        scrollToBottom();
        if (voiceFeedback) {
            voiceOutput(msg);
        }
    }

    /**
     * function to speak the message
     *
     * @param msg :   message to speak
     */
    private static void voiceOutput(String msg) {
        textToSpeech.speak(msg, TextToSpeech.QUEUE_ADD, null, null);
    }

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
     * function to show the progress dialog
     */
    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please wait...");
        builder.setCancelable(false);

        alert = builder.create();
        alert.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        checkPermissions();

        AppController.signInAnonymously(this);
        AppController.getFirebaseInstanceId();

        initViews();
        setupRecyclerView();
        initListeners();

        apiRequest = new ApiRequest();

        initTextToSpeech();

    }

    /**
     * function to initialize the Text To Speech for voice output
     */
    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int ttsLang = textToSpeech.setLanguage(Locale.US);

                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language is not supported!");
                    } else {
                        Log.i("TTS", "Language Supported.");
                    }
                    Log.i("TTS", "Initialization success.");
                } else {
                    Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

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
                if (AppController.checkSignIn()) {
                    sendMsg(etMsg.getText().toString());
                    scrollToBottom();
                } else {
                    AppController.signInAnonymously(ChatActivity.this);
                }
            }
        });

        btnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AppController.checkSignIn()) {
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
            if (AppController.expiryTime != null && !AppController.token.equals("") && AppController.expiryTime.getTime() > System.currentTimeMillis()) {
                addMsg(msg, 1, false);
                etMsg.setText("");
                voiceInput = "";
                new APIRequest(this, AppController.token, AppController.expiryTime, msg, tts, sentiment, knowledge).execute();
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
                voiceInput = result.get(0);
                sendMsg(result.get(0));
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
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
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
        private Context context;
        private String token;
        private Date expiryTime;
        private String msg;
        private boolean tts;
        private boolean sentiment;
        private boolean knowledge;

        public APIRequest(Context context, String token, Date expiryTime, String msg, boolean tts, boolean sentiment, boolean knowledge) {
            this.context = context;
            this.token = token;
            this.expiryTime = expiryTime;
            this.msg = msg;
            this.tts = tts;
            this.sentiment = sentiment;
            this.knowledge = knowledge;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
            disableInputPanel();
        }

        @Override
        protected String doInBackground(Void... voids) {
            return apiRequest.callAPI(context, token, expiryTime, msg, tts, sentiment, knowledge);
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            alert.dismiss();
            enableInputPanel();
            addMsg(response, 0, tts);
        }
    }

    /**
     * disables the input panel (EditText, Mic, Send button)
     */
    private void disableInputPanel() {
        etMsg.setEnabled(false);
        btnMic.setEnabled(false);
        btnSend.setEnabled(false);
        etMsg.setBackgroundColor(Color.GRAY);
        btnMic.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        btnSend.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
    }

    /**
     * enables the input panel (EditText, Mic, Send button)
     */
    private void enableInputPanel() {
        etMsg.setEnabled(true);
        btnMic.setEnabled(true);
        btnSend.setEnabled(true);
        etMsg.setBackgroundColor(Color.TRANSPARENT);
        btnMic.clearColorFilter();
        btnSend.clearColorFilter();

    }

}
