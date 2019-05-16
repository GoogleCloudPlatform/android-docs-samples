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

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

@SmallTest
public class Base64EncodingHelperAndroidTest {

    @Test
    public void base64Encode_16Khz_Success() throws IOException {
        String expectedFilePath = "assets/speech-recording-16khz.b64";
        InputStream expectedInputStream = this.getClass().getClassLoader().getResourceAsStream(expectedFilePath);
        InputStreamReader expectedInputStreamReader = new InputStreamReader(expectedInputStream);
        BufferedReader expectedBufferedReader = new BufferedReader(expectedInputStreamReader);
        StringBuilder expected = new StringBuilder();

        String line;
        while((line = expectedBufferedReader.readLine()) != null) {
            expected.append(line);
        }

        String actualFilePath = "assets/speech-recording-16khz.wav";
        InputStream actualInputStream = this.getClass().getClassLoader().getResourceAsStream(actualFilePath);

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File actualFile = new File(context.getCacheDir(), "speech-recording-16khz.wav");
        OutputStream output = new FileOutputStream(actualFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = actualInputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            output.close();
        actualInputStream.close();

        String actual = Base64EncodingHelper.encode(actualFile);

        Assert.assertEquals(expected.toString(), actual);
    }

    @Test
    public void base64Encode_24Khz_Success() throws IOException {
        String expectedFilePath = "assets/speech-recording-24khz.b64";
        InputStream expectedInputStream = this.getClass().getClassLoader().getResourceAsStream(expectedFilePath);
        InputStreamReader expectedInputStreamReader = new InputStreamReader(expectedInputStream);
        BufferedReader expectedBufferedReader = new BufferedReader(expectedInputStreamReader);
        StringBuilder expected = new StringBuilder();

        String line;
        while((line = expectedBufferedReader.readLine()) != null) {
            expected.append(line);
        }

        String actualFilePath = "assets/speech-recording-24khz.wav";
        InputStream actualInputStream = this.getClass().getClassLoader().getResourceAsStream(actualFilePath);

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File actualFile = new File(context.getCacheDir(), "speech-recording-24khz.wav");
        OutputStream output = new FileOutputStream(actualFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = actualInputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
        output.close();
        actualInputStream.close();

        String actual = Base64EncodingHelper.encode(actualFile);

        Assert.assertEquals(expected.toString(), actual);
    }

    @Test
    public void base64Encode_Fail() throws IOException {
        String expectedFilePath = "assets/speech-recording-24khz.b64";
        InputStream expectedInputStream = this.getClass().getClassLoader().getResourceAsStream(expectedFilePath);
        InputStreamReader expectedInputStreamReader = new InputStreamReader(expectedInputStream);
        BufferedReader expectedBufferedReader = new BufferedReader(expectedInputStreamReader);
        StringBuilder expected = new StringBuilder();

        String line;
        while((line = expectedBufferedReader.readLine()) != null) {
            expected.append(line);
        }

        String actualFilePath = "assets/speech-recording-16khz.wav";
        InputStream actualInputStream = this.getClass().getClassLoader().getResourceAsStream(actualFilePath);

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File actualFile = new File(context.getCacheDir(), "speech-recording-16khz.wav");
        OutputStream output = new FileOutputStream(actualFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = actualInputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
        output.close();
        actualInputStream.close();

        String actual = Base64EncodingHelper.encode(actualFile);

        Assert.assertNotEquals(expected.toString(), actual);
    }
}
