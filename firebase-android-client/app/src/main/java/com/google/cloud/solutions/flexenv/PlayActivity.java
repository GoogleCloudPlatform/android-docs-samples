/*
 * Copyright 2016 Google LLC.
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

package com.google.cloud.solutions.flexenv;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.cloud.solutions.flexenv.common.Base64EncodingHelper;
import com.google.cloud.solutions.flexenv.common.BaseMessage;
import com.google.cloud.solutions.flexenv.common.GcsDownloadHelper;
import com.google.cloud.solutions.flexenv.common.RecordingHelper;
import com.google.cloud.solutions.flexenv.common.SpeechMessage;
import com.google.cloud.solutions.flexenv.common.SpeechTranslationHelper;
import com.google.cloud.solutions.flexenv.common.TextMessage;
import com.google.cloud.solutions.flexenv.common.Translation;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.chromium.net.CronetEngine;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Main activity to select a channel and exchange messages with other users
 * The app expects users to authenticate with Google ID. It also sends user
 * activity logs to a servlet instance through Firebase.
 */
public class PlayActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener,
        AdapterView.OnItemClickListener,
        View.OnKeyListener,
        View.OnClickListener {

    // Firebase keys commonly used with backend servlet instances
    private static final String IBX = "inbox";
    private static final String CHS = "channels";
    private static final String REQLOG = "requestLogger";

    private static final int RC_SIGN_IN = 9001;

    private static final String TAG = "PlayActivity";
    private static final String CURRENT_CHANNEL_KEY = "CURRENT_CHANNEL_KEY";
    private static final String INBOX_KEY = "INBOX_KEY";
    private static final String FIREBASE_LOGGER_PATH_KEY = "FIREBASE_LOGGER_PATH_KEY";
    private static FirebaseLogger fbLog;

    private GoogleApiClient mGoogleApiClient;
    private String firebaseLoggerPath;
    private String inbox;
    private String currentChannel;
    private ChildEventListener channelListener;
    private SimpleDateFormat fmt;
    private CronetEngine cronetEngine;

    private Menu channelMenu;
    private TextView channelLabel;
    private List<Map<String, String>> messages;
    private SimpleAdapter messageAdapter;
    private EditText messageText;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ListView messageHistory;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        channelMenu = navigationView.getMenu();
        navigationView.setNavigationItemSelectedListener(this);
        initChannels();

        GoogleSignInOptions.Builder gsoBuilder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail();

        GoogleSignInOptions gso = gsoBuilder.build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(this);
        channelLabel = findViewById(R.id.channelLabel);
        Button signOutButton = findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);

        ImageButton microphoneButton = findViewById(R.id.microphone_button);
        microphoneButton.setOnClickListener(this);

        messages = new ArrayList<>();
        messageAdapter = new SimpleAdapter(this, messages, android.R.layout.simple_list_item_2,
                new String[]{"message", "meta"},
                new int[]{android.R.id.text1, android.R.id.text2});

        messageHistory = findViewById(R.id.messageHistory);
        messageHistory.setOnItemClickListener(this);
        messageHistory.setAdapter(messageAdapter);
        messageText = findViewById(R.id.messageText);
        messageText.setOnKeyListener(this);
        fmt = new SimpleDateFormat("yy.MM.dd HH:mm z", Locale.US);

        status = findViewById(R.id.status);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.d(TAG, "Google authentication status: " + result.getStatus().getStatusMessage());
            // If Google ID authentication is successful, obtain a token for Firebase authentication.
            if (result.isSuccess() && result.getSignInAccount() != null) {
                status.setText(getResources().getString(R.string.authenticating_label));
                AuthCredential credential = GoogleAuthProvider.getCredential(
                        result.getSignInAccount().getIdToken(), null);
                FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            Log.d(TAG, "signInWithCredential:onComplete Successful: " + task.isSuccessful());
                            if (task.isSuccessful()) {
                                final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    inbox = "client-" + Integer.toString(Math.abs(currentUser.getUid().hashCode()));
                                    requestLogger(() -> {
                                        Log.d(TAG, "onLoggerAssigned logger id: " + inbox);
                                        fbLog.log(inbox, "Signed in");
                                        updateUI();
                                    });
                                } else {
                                    updateUI();
                                }
                            } else {
                                Log.w(TAG, "signInWithCredential:onComplete", task.getException());
                                status.setText(String.format(
                                        getResources().getString(R.string.authentication_failed),
                                        task.getException())
                                );
                            }
                        });
            } else if (result.getStatus().isCanceled()) {
                String message = "Google authentication was canceled. "
                        + "Verify the SHA certificate fingerprint in the Firebase console.";
                Log.d(TAG, message);
                showErrorToast(new Exception(message));
            } else {
                Log.d(TAG, "Google authentication status: " + result.getStatus().toString());
                showErrorToast(new Exception(result.getStatus().toString()));
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.sign_in_button:
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                // Start authenticating with Google ID first.
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;
            case R.id.microphone_button:
                translateAudioMessage(v);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getAdapter().getItem(position) instanceof Map) {
            Map map = (Map) parent.getAdapter().getItem(position);
            if (map.containsKey("gcsBucket") && map.containsKey("gcsPath")) {
                String gcsBucket = map.get("gcsBucket").toString();
                String gcsPath = map.get("gcsPath").toString();
                playMessage(gcsBucket, gcsPath);
            }
        }
    }

    private void playMessage(String gcsBucket, String gcsPath) {
        String filePath = gcsBucket + "/" + gcsPath;
        File file = new File(getFilesDir(), filePath);

        if(file.exists()) {
            MediaPlayer mediaPlayer = MediaPlayer.create(
                    getApplicationContext(), Uri.fromFile(file));
            mediaPlayer.start();
        } else {
            GcsDownloadHelper.getInstance().downloadGcsFile(
                    getApplicationContext(), getCronetEngine(), gcsBucket, gcsPath,
                    new GcsDownloadHelper.GcsDownloadListener() {
                        @Override
                        public void onDownloadSucceeded(File file) {
                            MediaPlayer mediaPlayer = MediaPlayer.create(
                                    getApplicationContext(), Uri.fromFile(file));
                            mediaPlayer.start();
                        }

                        @Override
                        public void onDownloadFailed(Exception e) {
                            showErrorToast(e);
                            Log.e(TAG, e.getLocalizedMessage());
                        }
                    }
            );
        }
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                status -> {
                    FirebaseAuth.getInstance().signOut();
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    databaseReference.removeEventListener(channelListener);
                    databaseReference.onDisconnect();
                    inbox = null;
                    runOnUiThread(PlayActivity.this::updateUI);
                    fbLog.log(inbox, "Signed out");
                });
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                FirebaseDatabase.getInstance().getReference().child(CHS + "/" + currentChannel)
                        .push()
                        .setValue(new TextMessage(messageText.getText().toString(), currentUser.getDisplayName(), BaseMessage.MESSAGE_TYPE_TEXT));
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private void addMessage(String msgString, String meta) {
        Map<String, String> message = new HashMap<>();
        message.put("message", msgString);
        message.put("meta", meta);
        messages.add(message);

        messageAdapter.notifyDataSetChanged();
        messageText.setText("");
    }
    private void addMessage(String msgString, String meta, String gcsBucket, String gcsPath) {
        Map<String, String> message = new HashMap<>();
        // 🔈 Prepend a speaker emoji to text.
        message.put("message", "\uD83D\uDD08" + msgString);
        message.put("meta", meta);
        message.put("gcsBucket", gcsBucket);
        message.put("gcsPath", gcsPath);
        messages.add(message);

        messageAdapter.notifyDataSetChanged();
    }

    private void translateAudioMessage(View v) {
        ImageButton microphoneButton = (ImageButton)v;
        if (RecordingHelper.getInstance().hasRequiredPermissions(getApplicationContext())) {
            if (!RecordingHelper.getInstance().isRecording()) {
                RecordingHelper.getInstance().startRecording(new RecordingHelper.RecordingListener() {
                    @Override
                    public void onRecordingSucceeded(File output) {
                        String base64EncodedAudioMessage;
                        try {
                            base64EncodedAudioMessage = Base64EncodingHelper.encode(output);
                            SpeechTranslationHelper.getInstance().translateAudioMessage(
                                    getApplicationContext(),
                                    getCronetEngine(),
                                    base64EncodedAudioMessage,
                                    16000,
                                    new SpeechTranslationHelper.SpeechTranslationListener() {
                                        @Override
                                        public void onTranslationSucceeded(String responseBody) {
                                            Log.i(TAG, responseBody);
                                            try {
                                                final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                                if(currentUser != null) {
                                                    SpeechMessage speechMessage = new SpeechMessage(
                                                            new JSONObject(responseBody),
                                                            currentUser.getDisplayName(),
                                                            BaseMessage.MESSAGE_TYPE_SPEECH
                                                            );
                                                    FirebaseDatabase.getInstance().getReference().child(CHS + "/" + currentChannel)
                                                            .push()
                                                            .setValue(speechMessage);
                                                }
                                            } catch (JSONException e) {
                                                showErrorToast(e);
                                                Log.e(TAG, e.getLocalizedMessage());
                                            }
                                            microphoneButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_mic_none_24px));
                                        }

                                        @Override
                                        public void onTranslationFailed(Exception e) {
                                            showErrorToast(e);
                                            Log.e(TAG, e.getLocalizedMessage());
                                        }
                                    });
                        } catch (IOException e) {
                            showErrorToast(e);
                            Log.e(TAG, e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onRecordingFailed(Exception e) {
                        showErrorToast(e);
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                });
                microphoneButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_mic_24px));
            } else {
                RecordingHelper.getInstance().stopRecording();
            }
        } else {
            RecordingHelper.getInstance().requestRequiredPermissions(this);
        }
    }

    /**
     * Creates an instance of the CronetEngine class.
     * Instances of CronetEngine require a lot of resources. Additionally, their creation is slow
     * and expensive. It's recommended to delay the creation of CronetEngine instances until they
     * are required and reuse them as much as possible.
     * @return An instance of CronetEngine.
     */
    private synchronized CronetEngine getCronetEngine() {
        if(cronetEngine == null) {
            CronetEngine.Builder myBuilder = new CronetEngine.Builder(this);
            cronetEngine = myBuilder.build();
        }
        return cronetEngine;
    }

    private void updateUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
            findViewById(R.id.channelLabel).setVisibility(View.VISIBLE);
            findViewById(R.id.messageText).setVisibility(View.VISIBLE);
            findViewById(R.id.messageHistory).setVisibility(View.VISIBLE);

            if(speechTranslationEnabled()) {
                findViewById(R.id.microphone_button).setVisibility(View.VISIBLE);
            }

            status.setText(
                    String.format(getResources().getString(R.string.signed_in_label),
                    currentUser.getDisplayName())
            );
            findViewById(R.id.status).setVisibility(View.VISIBLE);

            // Select the first channel in the array if there's no channel selected
            switchChannel(currentChannel != null ? currentChannel :
                    getResources().getStringArray(R.array.channels)[0]);
        } else {
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            findViewById(R.id.channelLabel).setVisibility(View.GONE);
            findViewById(R.id.messageText).setVisibility(View.GONE);
            findViewById(R.id.microphone_button).setVisibility(View.GONE);
            findViewById(R.id.messageHistory).setVisibility(View.GONE);
            findViewById(R.id.status).setVisibility(View.GONE);
            ((TextView)findViewById(R.id.status)).setText("");
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        switchChannel(item.toString());

        return true;
    }

    private void switchChannel(String channel) {
        messages.clear();

        String msg = "Switching channel to '" + channel + "'";
        fbLog.log(inbox, msg);

        // Switching a listener to the selected channel.
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child(CHS + "/" + currentChannel).removeEventListener(channelListener);
        currentChannel = channel;
        databaseReference.child(CHS + "/" + currentChannel).addChildEventListener(channelListener);

        channelLabel.setText(currentChannel);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(CURRENT_CHANNEL_KEY, currentChannel);
        outState.putString(INBOX_KEY, inbox);
        outState.putString(FIREBASE_LOGGER_PATH_KEY, firebaseLoggerPath);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        currentChannel = savedInstanceState.getString(CURRENT_CHANNEL_KEY);
        inbox = savedInstanceState.getString(INBOX_KEY);
        firebaseLoggerPath = savedInstanceState.getString(FIREBASE_LOGGER_PATH_KEY);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            fbLog = new FirebaseLogger(firebaseLoggerPath);
            updateUI();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

// [START request_logger]
    /*
     * Request that a servlet instance be assigned.
     */
    private void requestLogger(final LoggerListener loggerListener) {
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child(IBX + "/" + inbox).addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue(String.class) != null) {
                    firebaseLoggerPath = IBX + "/" + snapshot.getValue(String.class) + "/logs";
                    fbLog = new FirebaseLogger(firebaseLoggerPath);
                    databaseReference.child(IBX + "/" + inbox).removeEventListener(this);
                    loggerListener.onLoggerAssigned();
                }
            }

            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getDetails());
            }
        });

        databaseReference.child(REQLOG).push().setValue(inbox);
    }
// [END request_logger]

    /*
     * Initialize predefined channels as activity menu.
     * Once a channel is selected, ChildEventListener is attached and
     * waits for messages.
     */
    private void initChannels() {
        String[] channelArray = getResources().getStringArray(R.array.channels);
        Log.d(TAG, "Channels : " + Arrays.toString(channelArray));
        for (String topic : channelArray) {
            channelMenu.add(topic);
        }

        channelListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prevKey) {
                if(snapshot.hasChild("/messageType")) {
                    String messageType = snapshot.child("/messageType").getValue(String.class);
                    if(messageType != null) {
                        // Extract attributes from appropriate message object to display on the screen.
                        if (messageType.equals(BaseMessage.MESSAGE_TYPE_TEXT)) {
                            TextMessage message = snapshot.getValue(TextMessage.class);
                            if(message != null) {
                                addMessage(message.getText(),fmt.format(new Date(message.getTimeLong())) + " "
                                        + message.getDisplayName());
                            }
                        } else if (messageType.equals(BaseMessage.MESSAGE_TYPE_SPEECH)) {
                            SpeechMessage message = snapshot.getValue(SpeechMessage.class);
                            String language = getApplicationContext()
                                    .getResources()
                                    .getConfiguration()
                                    .getLocales()
                                    .get(0).getLanguage();
                            if(message != null) {
                                Translation translation = message.getTranslation(language);
                                addMessage(translation.getText(), fmt.format(new Date(message.getTimeLong())) + " "
                                        + message.getDisplayName(), message.getGcsBucket(), translation.getGcsPath());
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getDetails());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String prevKey) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String prevKey) {}
        };
    }

    private boolean speechTranslationEnabled() {
        String speechEndpoint = getString(R.string.speechToSpeechEndpoint);
        return !speechEndpoint.contains("YOUR-PROJECT-ID");
    }

    private void showErrorToast(Exception e) {
        runOnUiThread(
                () -> Toast.makeText(
                        getApplicationContext(),
                        e.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show()
        );
    }

    /**
     * A listener to get notifications about server-side loggers.
     */
    private interface LoggerListener {
        /**
         * Called when a logger has been assigned to this client.
         */
        void onLoggerAssigned();
    }
}
