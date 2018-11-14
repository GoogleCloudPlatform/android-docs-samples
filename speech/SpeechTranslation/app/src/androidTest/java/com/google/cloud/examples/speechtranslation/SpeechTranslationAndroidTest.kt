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

import androidx.test.filters.SmallTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader

@SmallTest
class SpeechTranslationAndroidTest {
    private var successfulResponse = JSONObject()
    private var noTranscriptionResponse = JSONObject()
    private var noTranslationsResponse = JSONObject()

    @Before
    fun loadResponses() {
        var responseStream = this.javaClass.getClassLoader()!!.getResourceAsStream("assets/successful-response.json")
        var responseStreamReader = InputStreamReader(responseStream)
        var responseBufferedReader = BufferedReader(responseStreamReader)
        var responseText = StringBuilder()

        var line: String?
        line = responseBufferedReader.readLine()
        while (line != null) {
            responseText.append(line)
            line = responseBufferedReader.readLine()
        }
        successfulResponse = JSONObject(responseText.toString())

        responseStream = this.javaClass.getClassLoader()!!.getResourceAsStream("assets/no-transcription-response.json")
        responseStreamReader = InputStreamReader(responseStream)
        responseBufferedReader = BufferedReader(responseStreamReader)
        responseText = StringBuilder()

        line = responseBufferedReader.readLine()
        while (line != null) {
            responseText.append(line)
            line = responseBufferedReader.readLine()
        }
        noTranscriptionResponse = JSONObject(responseText.toString())

        responseStream = this.javaClass.getClassLoader()!!.getResourceAsStream("assets/no-translations-response.json")
        responseStreamReader = InputStreamReader(responseStream)
        responseBufferedReader = BufferedReader(responseStreamReader)
        responseText = StringBuilder()

        line = responseBufferedReader.readLine()
        while (line != null) {
            responseText.append(line)
            line = responseBufferedReader.readLine()
        }
        noTranslationsResponse = JSONObject(responseText.toString())
    }

    @Test
    fun speechTranslation_LoadSuccessfulResponse() {
        val speechTranslation = SpeechTranslation(successfulResponse)
        assertTrue(speechTranslation.transcription == "this is a test please translate this message")
        assertTrue(speechTranslation.gcsBucket == "speech-to-speech-output")
        assertTrue(speechTranslation.translations.count() == 3)
        assertTrue(speechTranslation.getTranslation("fr").text == "ceci est un test s'il vous plaît traduire ce message")
        assertTrue(speechTranslation.getTranslation("es").gcsPath == "es/a5cec63a-35d5-480c-8f1f-6c2fa9bcfd8f.mp3")
    }

    @Test(expected = NoSuchElementException::class)
    fun speechTranslation_NoTranslationFound() {
        val speechTranslation = SpeechTranslation(successfulResponse)
        speechTranslation.getTranslation("xx")
    }

    @Test(expected = JSONException::class)
    fun speechTranslation_NoTranscription() {
        SpeechTranslation(noTranscriptionResponse)
    }

    @Test(expected = JSONException::class)
    fun speechTranslation_NoTranslations() {
        SpeechTranslation(noTranslationsResponse)
    }
}