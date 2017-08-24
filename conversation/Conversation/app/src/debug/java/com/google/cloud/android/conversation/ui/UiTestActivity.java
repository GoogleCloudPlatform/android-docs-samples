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

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.google.cloud.android.conversation.R;


/**
 * This is only for testing purpose. This activity has all UI components used in this app, and
 * the testing code can use this to test UI components without actually calling APIs.
 */
public class UiTestActivity extends AppCompatActivity implements MessageDialogFragment.Listener {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private MessageDialogFragment.Listener mListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_test);
        requestAudioPermission();
    }

    public void setMessageDialogFragmentListener(MessageDialogFragment.Listener listener) {
        mListener = listener;
    }

    private void requestAudioPermission() {
        // The test code deals with the dialog. The permission is always granted.
        if (ActivityCompat.checkSelfPermission(UiTestActivity.this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UiTestActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onMessageDialogDismissed() {
        if (mListener != null) {
            mListener.onMessageDialogDismissed();
        }
    }

}
