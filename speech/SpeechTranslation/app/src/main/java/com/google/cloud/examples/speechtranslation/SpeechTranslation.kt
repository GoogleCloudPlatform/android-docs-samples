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

import org.json.JSONObject
import java.util.ArrayList
import kotlin.NoSuchElementException
import kotlin.String

/**
 * SpeechTranslation represents a speech-based message that includes translations provided by a
 * Google Cloud Function.
 */

class SpeechTranslation (jsonAudioMessage: JSONObject) {

    val gcsBucket: String = jsonAudioMessage.getString("gcsBucket")
    val transcription: String = jsonAudioMessage.getString("transcription")
    val translations: List<Translation>

    init {
        val translationArray = jsonAudioMessage.getJSONArray("translations")
        val translations = ArrayList<Translation>()
        for (i in 0 until translationArray.length()) {
            val translation = Translation(
                    translationArray.getJSONObject(i).getString("languageCode"),
                    translationArray.getJSONObject(i).getString("text"),
                    translationArray.getJSONObject(i).getString("gcsPath")
            )
            translations.add(translation)
        }
        this.translations = translations
    }

    fun getTranslation(languageCode: String): Translation {
        for (item in translations) {
            if (item.languageCode == languageCode) {
                return item
            }
        }
        throw NoSuchElementException()
    }

    class Translation(val languageCode: String,
                      val text: String,
                      val gcsPath: String)

    companion object {
        private const val TAG = "SpeechTranslation"
    }
}
