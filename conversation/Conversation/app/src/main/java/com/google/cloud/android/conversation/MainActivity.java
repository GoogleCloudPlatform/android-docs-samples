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

package com.google.cloud.android.conversation;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.cloud.android.conversation.api.ConversationService;
import com.google.cloud.android.conversation.api.Utterance;
import com.google.cloud.android.conversation.ui.AudioIndicatorView;
import com.google.cloud.android.conversation.ui.ConversationHistoryAdapter;
import com.google.cloud.android.conversation.ui.InputHelper;
import com.google.cloud.android.conversation.ui.MessageDialogFragment;


public class MainActivity extends AppCompatActivity {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private static final String STATE_HISTORY = "history";

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    ConversationService mConversationService;

    private LinearLayoutManager mLayoutManager;
    private ConversationHistoryAdapter mAdapter;

    private InputHelper mInputHelper;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mConversationService = ConversationService.from(binder);
            mConversationService.addListener(mSpeechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mConversationService = null;
        }

    };

    private ConversationService.Listener mSpeechServiceListener
            = new ConversationService.Listener() {

        @Override
        public void onApiReady() {
            mInputHelper.setEnabled(true);
        }

        @Override
        public void onNewUtterance(Utterance utterance) {
            if (mInputHelper != null && utterance.direction == Utterance.OUTGOING) {
                mInputHelper.showTranscript(null);
            }
            mAdapter.addUtterance(utterance);
            mLayoutManager.scrollToPosition(mAdapter.getItemCount() - 1);
        }

        @Override
        public void onNewRecognition(String text) {
            if (mInputHelper != null) {
                mInputHelper.showTranscript(text);
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // The main conversation view
        final RecyclerView history = findViewById(R.id.history);
        history.setItemAnimator(new DefaultItemAnimator());
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);
        history.setLayoutManager(mLayoutManager);
        mAdapter = new ConversationHistoryAdapter();
        if (savedInstanceState != null) {
            mAdapter.restoreHistory(
                    savedInstanceState.<Utterance>getParcelableArrayList(STATE_HISTORY));
        }
        history.setAdapter(mAdapter);
        // User input
        mInputHelper = new InputHelper((EditText) findViewById(R.id.text),
                (ImageButton) findViewById(R.id.toggle),
                (AudioIndicatorView) findViewById(R.id.indicator), new InputHelper.Callback() {
            @Override
            public void onText(String text) {
                if (mConversationService != null) {
                    mConversationService.detectIntentByText(text);
                }
            }

            @Override
            public void onVoiceStart() {
                if (mConversationService != null) {
                    mConversationService.startDetectIntentByVoice(mInputHelper.getSampleRate());
                }
            }

            @Override
            public void onVoice(byte[] data, int size) {
                if (mConversationService != null) {
                    mConversationService.detectIntentByVoice(data, size);
                }
            }

            @Override
            public void onVoiceEnd() {
                if (mConversationService != null) {
                    mConversationService.finishDetectIntentByVoice();
                }
            }

            @Override
            public boolean ensureRecordAudioPermission() {
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    return true;
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO)) {
                    showPermissionMessageDialog();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_AUDIO_PERMISSION);
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        mInputHelper.release();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Conversation Engine
        bindService(new Intent(this, ConversationService.class), mServiceConnection,
                BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        // Stop Cloud Conversation Engine
        if (mConversationService != null) {
            mConversationService.removeListener(mSpeechServiceListener);
            unbindService(mServiceConnection);
            mConversationService = null;
        }

        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            outState.putParcelableArrayList(STATE_HISTORY, mAdapter.getHistory());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mInputHelper.resumeAudio();
            } else {
                mInputHelper.fallbackToText();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

}
