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

package com.google.cloud.solutions.flexenv.common;

import android.content.Context;
import android.util.Log;

import org.chromium.net.CronetEngine;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertTrue;

@LargeTest
public class SpeechTranslationHelperAndroidTest {
    private static final String TAG = "SpeechTranslationHelperAndroidTest";
    private static String base64EncodedAudioMessage;
    private static Context context;
    private static CronetEngine cronetEngine;

    @Before
    public void readSpeechRecording16khzb64File() throws IOException {
        String file = "assets/speech-recording-16khz.b64";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        StringBuilder stringBuilder = new StringBuilder();

        String line;
        while((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        base64EncodedAudioMessage = stringBuilder.toString();

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        CronetEngine.Builder myBuilder = new CronetEngine.Builder(context);
        cronetEngine = myBuilder.build();
    }

    @Test
    public void translateAudioMessage_Success() throws InterruptedException {
        final Object waiter = new Object();

        synchronized (waiter) {
            SpeechTranslationHelper.getInstance()
                    .translateAudioMessage(context, cronetEngine, base64EncodedAudioMessage,
                    16000, new SpeechTranslationHelper.SpeechTranslationListener() {
                        @Override
                        public void onTranslationSucceeded(String responseBody) {
                            try {
                                Log.i(TAG, responseBody);
                                JSONObject response = new JSONObject(responseBody);
                                assertTrue(response.has("transcription"));
                            } catch (JSONException e) {
                                Assert.fail();
                            } finally {
                                synchronized (waiter) {
                                    waiter.notify();
                                }
                            }
                        }

                        @Override
                        public void onTranslationFailed(Exception e) {
                            Assert.fail();
                            synchronized (waiter) {
                                waiter.notify();
                            }
                        }
                    });

            synchronized (waiter) {
                waiter.wait();
            }
        }
    }

    @Test
    public void translateAudioMessage_Wrong_SampleRate() throws InterruptedException {
        final Object waiter = new Object();

        synchronized (waiter) {
            SpeechTranslationHelper.getInstance()
                    .translateAudioMessage(context, cronetEngine, base64EncodedAudioMessage,
                            24000, new SpeechTranslationHelper.SpeechTranslationListener() {
                                @Override
                                public void onTranslationSucceeded(String responseBody) {
                                    Assert.fail();
                                }

                                @Override
                                public void onTranslationFailed(Exception e) {
                                    Assert.assertTrue(e.getMessage().contains("INVALID_ARGUMENT: sample_rate_hertz"));
                                    synchronized (waiter) {
                                        waiter.notify();
                                    }
                                }
                            });

            synchronized (waiter) {
                waiter.wait();
            }
        }
    }
}
