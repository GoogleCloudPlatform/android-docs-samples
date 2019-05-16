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

/**
 * Represents a translation included in a speech-based message. The translation is provided by a
 * Google Cloud Function. For more information about the function implementation, see
 * https://github.com/GoogleCloudPlatform/nodejs-docs-samples/tree/master/functions/speech-to-speech
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Translation {
    private String gcsPath;
    private String languageCode;
    private String text;

    public Translation() { }

    public String getGcsPath() { return gcsPath; }
    public void setGcsPath(String gcsPath) { this.gcsPath = gcsPath; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}