/*
 * Copyright 2018 Google LLC.
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

package com.google.cloud.examples.speechtranslation

import android.content.Context
import android.util.Log
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.chromium.net.CronetEngine
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

@LargeTest
class SpeechTranslationHelperAndroidTest {

    @Before
    @Throws(IOException::class)
    fun readSpeechRecording16khzb64File() {
        val file = "assets/speech-recording-16khz.b64"
        val `in` = this.javaClass.getClassLoader()!!.getResourceAsStream(file)
        val inputStreamReader = InputStreamReader(`in`)
        val bufferedReader = BufferedReader(inputStreamReader)

        val stringBuilder = StringBuilder()

        var line: String?
        line = bufferedReader.readLine()
        while (line != null) {
            stringBuilder.append(line)
            line = bufferedReader.readLine()
        }
        base64EncodedAudioMessage = stringBuilder.toString()
    }

    @Test
    @Throws(InterruptedException::class)
    fun translateAudioMessage_Success() {
        val waiter = Object()

        synchronized(waiter) {
            SpeechTranslationHelper
                    .translateAudioMessage(context, cronetEngine, base64EncodedAudioMessage,
                            16000, object : SpeechTranslationHelper.SpeechTranslationListener {
                        override fun onTranslationSucceeded(responseBody: String) {
                            try {
                                Log.i(TAG, responseBody)
                                val response = JSONObject(responseBody)
                                assertTrue(response.has("transcription"))
                            } catch (e: JSONException) {
                                Assert.fail()
                            } finally {
                                synchronized(waiter) {
                                    waiter.notify()
                                }
                            }
                        }

                        override fun onTranslationFailed(e: Exception) {
                            Assert.fail()
                            synchronized(waiter) {
                                waiter.notify()
                            }
                        }
                    })

            synchronized(waiter) {
                waiter.wait()
            }
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun translateAudioMessage_Wrong_SampleRate() {
        val waiter = Object()

        synchronized(waiter) {
            SpeechTranslationHelper
                    .translateAudioMessage(context, cronetEngine, base64EncodedAudioMessage,
                            24000, object : SpeechTranslationHelper.SpeechTranslationListener {
                        override fun onTranslationSucceeded(responseBody: String) {
                            Assert.fail()
                        }

                        override fun onTranslationFailed(e: Exception) {
                            val errorMessage: String? = e.message
                            if(errorMessage != null) {
                                Assert.assertTrue(errorMessage.contains("INVALID_ARGUMENT: sample_rate_hertz"))
                            }

                            synchronized(waiter) {
                                waiter.notify()
                            }
                        }
                    })

            synchronized(waiter) {
                waiter.wait()
            }
        }
    }

    companion object {
        private val TAG = "SpeechTranslationHelperAndroidTest"
        private var base64EncodedAudioMessage: String = ""
        private var context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        private var cronetEngine: CronetEngine = CronetEngine.Builder(context).build()
    }
}
