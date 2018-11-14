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
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import java.io.*

@SmallTest
class Base64EncodingHelperAndroidTest {

    @Test
    @Throws(IOException::class)
    fun base64Encode_16Khz_Success() {
        val expectedFilePath = "assets/speech-recording-16khz.b64"
        val expectedInputStream = this.javaClass.getClassLoader()!!.getResourceAsStream(expectedFilePath)
        val expectedInputStreamReader = InputStreamReader(expectedInputStream)
        val expectedBufferedReader = BufferedReader(expectedInputStreamReader)
        val expected = StringBuilder()

        var line: String?
        line = expectedBufferedReader.readLine()
        while (line != null) {
            expected.append(line)
            line = expectedBufferedReader.readLine()
        }

        val actualFilePath = "assets/speech-recording-16khz.wav"
        val actualInputStream = this.javaClass.getClassLoader()!!.getResourceAsStream(actualFilePath)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actualFile = File(context.cacheDir, "speech-recording-16khz.wav")
        val output = FileOutputStream(actualFile)
        val buffer = ByteArray(1024)
        var read: Int
        read = actualInputStream.read(buffer)
        while (read != -1) {
            output.write(buffer, 0, read)
            read = actualInputStream.read(buffer)
        }
        output.flush()
        output.close()
        actualInputStream.close()

        val actual = Base64EncodingHelper.encode(actualFile)

        Assert.assertEquals(expected.toString(), actual)
    }

    @Test
    @Throws(IOException::class)
    fun base64Encode_24Khz_Success() {
        val expectedFilePath = "assets/speech-recording-24khz.b64"
        val expectedInputStream = this.javaClass.getClassLoader()!!.getResourceAsStream(expectedFilePath)
        val expectedInputStreamReader = InputStreamReader(expectedInputStream)
        val expectedBufferedReader = BufferedReader(expectedInputStreamReader)
        val expected = StringBuilder()

        var line: String?
        line = expectedBufferedReader.readLine()
        while (line != null) {
            expected.append(line)
            line = expectedBufferedReader.readLine()
        }

        val actualFilePath = "assets/speech-recording-24khz.wav"
        val actualInputStream = this.javaClass.getClassLoader()!!.getResourceAsStream(actualFilePath)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actualFile = File(context.cacheDir, "speech-recording-24khz.wav")
        val output = FileOutputStream(actualFile)
        val buffer = ByteArray(1024)
        var read: Int
        read = actualInputStream.read(buffer)
        while (read != -1) {
            output.write(buffer, 0, read)
            read = actualInputStream.read(buffer)
        }
        output.flush()
        output.close()
        actualInputStream.close()

        val actual = Base64EncodingHelper.encode(actualFile)

        Assert.assertEquals(expected.toString(), actual)
    }

    @Test
    @Throws(IOException::class)
    fun base64Encode_Fail() {
        val expectedFilePath = "assets/speech-recording-24khz.b64"
        val expectedInputStream = this.javaClass.getClassLoader()!!.getResourceAsStream(expectedFilePath)
        val expectedInputStreamReader = InputStreamReader(expectedInputStream)
        val expectedBufferedReader = BufferedReader(expectedInputStreamReader)
        val expected = StringBuilder()

        var line: String?
        line = expectedBufferedReader.readLine()
        while (line != null) {
            expected.append(line)
            line = expectedBufferedReader.readLine()
        }

        val actualFilePath = "assets/speech-recording-16khz.wav"
        val actualInputStream = this.javaClass.getClassLoader()!!.getResourceAsStream(actualFilePath)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actualFile = File(context.cacheDir, "speech-recording-16khz.wav")
        val output = FileOutputStream(actualFile)
        val buffer = ByteArray(1024)
        var read: Int
        read = actualInputStream.read(buffer)
        while (read != -1) {
            output.write(buffer, 0, read)
            read = actualInputStream.read(buffer)
        }
        output.flush()
        output.close()
        actualInputStream.close()

        val actual = Base64EncodingHelper.encode(actualFile)

        Assert.assertNotEquals(expected.toString(), actual)
    }
}
