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

import android.util.Base64;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Abstract class that provides a method to base64 encode a file. The app encodes speech data to
 * send it to a Google Cloud Function that translates the speech to other languages.
 */
public abstract class Base64EncodingHelper {
    private static final String TAG = "Base64EncodingHelper";

    /**
     * Encodes the contents to a file in base64 format and returns them as a string.
     * @param inputFile The file that contains the speech message to encode.
     * @return The base64 encoded data in a string.
     * @throws IOException An exception thrown when the input file is not found or can't be closed.
     */
    // [START encode]
    public static String encode(File inputFile) throws IOException {
        byte[] data = new byte[(int) inputFile.length()];
        DataInputStream input = new DataInputStream(new FileInputStream(inputFile));
        int readBytes = input.read(data);
        Log.i(TAG, readBytes + " read from input file.");
        input.close();
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
    // [END encode]
}
