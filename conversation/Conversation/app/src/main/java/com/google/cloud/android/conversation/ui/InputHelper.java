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

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.cloud.android.conversation.R;
import com.google.cloud.android.conversation.util.VoiceRecorder;


/**
 * Helper class for managing 2 input modes; software keyboard and audio.
 */
public class InputHelper {

    /** Software keyboard mode; input is empty */
    private static final int MODE_TEXT_EMPTY = 1;
    /** Software keyboard mode; user is typing */
    private static final int MODE_TEXT_TYPING = 2;
    /** Audio input mode */
    private static final int MODE_AUDIO = 3;

    private final EditText mText;
    private final ImageButton mToggle;
    private final AudioIndicatorView mIndicator;

    private final VoiceRecorder mVoiceRecorder;

    private final Callback mCallback;

    private int mMode = MODE_TEXT_EMPTY;

    private final String mDescriptionVoice;
    private final String mDescriptionSend;
    private final String mDescriptionKeyboard;

    private boolean mEnabled;

    /**
     * Instantiates a new instance of {@link InputHelper}.
     *
     * @param text     The text input for the software keyboard. This is also used to show
     *                 transcript of audio input.
     * @param toggle   The button to toggle the modes.
     * @param callback The callback.
     */
    public InputHelper(EditText text, ImageButton toggle, AudioIndicatorView indicator,
            Callback callback) {
        mText = text;
        mToggle = toggle;
        mIndicator = indicator;
        mCallback = callback;
        mVoiceRecorder = new VoiceRecorder(wrapVoiceCallback(callback));
        // Bind event handlers
        text.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mMode == MODE_AUDIO) {
                    return;
                }
                changeTextMode(s);
            }
        });
        text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                send();
                return true;
            }
        });
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMode == MODE_TEXT_EMPTY) {
                    startAudio();
                } else if (mMode == MODE_TEXT_TYPING) {
                    send();
                } else if (mMode == MODE_AUDIO) {
                    startText();
                }
            }
        });
        // String resources
        final Context context = mText.getContext();
        mDescriptionVoice = context.getString(R.string.description_voice);
        mDescriptionSend = context.getString(R.string.description_send);
        mDescriptionKeyboard = context.getString(R.string.description_keyboard);
        // Initial view states
        mText.setEnabled(false);
        mToggle.setEnabled(false);
        changeTextMode(text.getText());
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled == enabled) {
            return;
        }
        mEnabled = enabled;
        mText.setEnabled(enabled);
        mToggle.setEnabled(enabled);
    }

    /**
     * Releases all the retained resources.
     */
    public void release() {
        mVoiceRecorder.stop();
    }

    /**
     * @return The sample rate for the audio input.
     */
    public int getSampleRate() {
        return mVoiceRecorder.getSampleRate();
    }

    /**
     * Resumes audio input.
     */
    public void resumeAudio() {
        mVoiceRecorder.start();
    }

    /**
     * Falls back to text input mode.
     */
    public void fallbackToText() {
        startText();
    }

    /**
     * Shows a real-time audio recognition result. This is only available when the user is using
     * audio input.
     *
     * @param transcript The transcript. Pass {@code null} to clear.
     */
    public void showTranscript(@Nullable String transcript) {
        if (mMode != MODE_AUDIO) {
            return;
        }
        mText.setText(transcript);
    }

    private void startAudio() {
        if (mCallback.ensureRecordAudioPermission()) {
            mIndicator.setVisibility(View.VISIBLE);
            mText.getText().clear();
            mText.setEnabled(false);
            mText.setHint(R.string.hint_audio);
            mToggle.setImageResource(R.drawable.ic_keyboard);
            mToggle.setContentDescription(mDescriptionKeyboard);
            mMode = MODE_AUDIO;
            mVoiceRecorder.start();
        }
    }

    private void startText() {
        mVoiceRecorder.stop();
        mIndicator.setVisibility(View.INVISIBLE);
        mText.getText().clear();
        mText.setEnabled(true);
        mText.setHint(R.string.hint_text);
        mToggle.setImageResource(R.drawable.ic_mic);
        mToggle.setContentDescription(mDescriptionVoice);
        mMode = MODE_TEXT_EMPTY;
    }

    private void send() {
        if (!mEnabled) {
            return;
        }
        final Editable content = mText.getText();
        if (TextUtils.isEmpty(content)) {
            return;
        }
        mCallback.onText(content.toString());
        content.clear();
        changeTextMode(null);
    }

    private void changeTextMode(CharSequence s) {
        if (TextUtils.isEmpty(s)) {
            mToggle.setImageResource(R.drawable.ic_mic);
            mToggle.setContentDescription(mDescriptionVoice);
            mMode = MODE_TEXT_EMPTY;
        } else {
            mToggle.setImageResource(R.drawable.ic_send);
            mToggle.setContentDescription(mDescriptionSend);
            mMode = MODE_TEXT_TYPING;
        }
    }

    private VoiceRecorder.Callback wrapVoiceCallback(final Callback callback) {
        return new VoiceRecorder.Callback() {
            @Override
            public void onVoiceStart() {
                callback.onVoiceStart();
                mIndicator.post(new Runnable() {
                    @Override
                    public void run() {
                        mIndicator.setHearingVoice(true);
                    }
                });
            }

            @Override
            public void onVoice(byte[] data, int size) {
                callback.onVoice(data, size);
            }

            @Override
            public void onVoiceEnd() {
                mIndicator.post(new Runnable() {
                    @Override
                    public void run() {
                        mIndicator.setHearingVoice(false);
                    }
                });
                callback.onVoiceEnd();
            }
        };
    }

    /**
     * Callbacks for {@link InputHelper}.
     */
    public static abstract class Callback extends VoiceRecorder.Callback {

        /**
         * Called when a new text is input from the keyboard.
         *
         * @param text A new text input.
         */
        public void onText(String text) {
        }

        /**
         * Called when the {@link InputHelper} needs to make sure that the app has permission for
         * audio recording.
         *
         * @return {@code true} if audio recording permission is available. If the implementation
         * returns {@code false}, it is responsible for calling {@link #resumeAudio()} after the
         * permission is granted.
         */
        public boolean ensureRecordAudioPermission() {
            return false;
        }

    }

    /**
     * A convenience class for overriding some methods of {@link TextWatcher}.
     */
    private static abstract class TextWatcherAdapter implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }

    }

}
