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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SpeechMessage represents a speech-based message that includes translations provided by a
 * Google Cloud Function.
 * For more information about the function implementation, see
 * https://github.com/GoogleCloudPlatform/nodejs-docs-samples/tree/master/functions/speech-to-speech
 */
@SuppressWarnings({"WeakerAccess", "unused", "SameReturnValue"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpeechMessage extends BaseMessage {
    private static final String TAG = "SpeechMessage";

    private String gcsBucket;
    private String transcription;
    private List<Translation> translations;

    public SpeechMessage() { }

    public SpeechMessage(JSONObject jsonAudioMessage, String displayName, String messageType) throws JSONException {
        setDisplayName(displayName);
        setMessageType(messageType);
        setTranscription(jsonAudioMessage.getString("transcription"));
        setGcsBucket(jsonAudioMessage.getString("gcsBucket"));

        JSONArray translationArray = jsonAudioMessage.getJSONArray("translations");
        List<Translation> translations = new ArrayList<>();
        for (int i = 0; i < translationArray.length(); i++) {
            Translation translation = new Translation();
            translation.setLanguageCode(translationArray.getJSONObject(i).getString("languageCode"));
            translation.setText(translationArray.getJSONObject(i).getString("text"));
            translation.setGcsPath(translationArray.getJSONObject(i).getString("gcsPath"));
            translations.add(translation);
        }
        this.translations = translations;
    }

    public String getGcsBucket() { return gcsBucket; }
    public void setGcsBucket(String gcsBucket) { this.gcsBucket = gcsBucket; }

    public String getTranscription() {
        return transcription;
    }
    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    public Translation getTranslation(String languageCode) {
        Translation translation = null;
        for (Translation item : translations) {
            if (item.getLanguageCode().equals(languageCode)) {
                translation = item;
                break;
            }
        }
        return translation;
    }
    public List<Translation> getTranslations() {
        return translations;
    }
    public void setTranslations(List<Translation> translations) { this.translations = translations; }
}
