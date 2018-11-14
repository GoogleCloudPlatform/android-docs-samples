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

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

/**
 * Singleton class that records audio from the microphone on the device and writes it to a file in
 * PCM-16 (wave) format.
 */
object RecordingHelper {
    private const val TAG = "RecordingHelper"
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val AUDIO_SOURCE = MediaRecorder.AudioSource.UNPROCESSED
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val SAMPLE_RATE_IN_HZ = 16000

    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
    private val RAW_FILE_PATH = Environment.getExternalStorageDirectory().path + "/speech-recording.raw"
    private val WAV_FILE_PATH = Environment.getExternalStorageDirectory().path + "/speech-recording.wav"

    private var audioRecord: AudioRecord = AudioRecord(
            AUDIO_SOURCE,
            SAMPLE_RATE_IN_HZ,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
    )

    private lateinit var outputStream: BufferedOutputStream
    var isRecording = false

    /**
     * Starts recording audio from the device microphone. The client must call stopRecording()
     * before this method can process the recorded audio and write the audio file to disk.
     * @param recordingListener The callback to deliver the results to.
     */
    fun startRecording(recordingListener: RecordingListener) {
        isRecording = true

        outputStream = BufferedOutputStream(FileOutputStream(RAW_FILE_PATH))

        Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)

            val data = ByteArray(BUFFER_SIZE)


            audioRecord.startRecording()

            // This loop runs until the client calls stopRecording().
            while (isRecording) {
                val status = audioRecord.read(data, 0, data.size)

                if (status == AudioRecord.ERROR_INVALID_OPERATION || status == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Couldn't read data")
                    recordingListener.onRecordingFailed(IOException())
                }

                try {
                    outputStream.write(data, 0, data.size)
                } catch (e: IOException) {
                    Log.e(TAG, "Couldn't save data", e)
                    recordingListener.onRecordingFailed(e)
                }

            }

            // After the client calls stopRecording(), this method processes the recorded audio.
            try {
                outputStream.close()
                audioRecord.stop()
                audioRecord.release()

                Log.v(TAG, "Recording stopped")

                val rawFile = File(RAW_FILE_PATH)
                val wavFile = File(WAV_FILE_PATH)
                saveAsWave(rawFile, wavFile)
                recordingListener.onRecordingSucceeded(wavFile)
            } catch (e: IOException) {
                Log.e(TAG, "File error", e)
                recordingListener.onRecordingFailed(e)
            }
        }.start()
    }

    fun stopRecording() {
        isRecording = false
    }

    @Throws(IOException::class)
    private fun saveAsWave(rawFile: File, waveFile: File) {
        val rawData = ByteArray(rawFile.length().toInt())
        DataInputStream(FileInputStream(rawFile)).use { input ->
            var readBytes: Int
            do {
                readBytes = input.read(rawData)
            } while (readBytes != -1)
        }

        DataOutputStream(FileOutputStream(waveFile)).use { output ->
            // WAVE specification
            val asciiCharset = Charset.forName("US-ASCII")
            // Chunk ID: "RIFF" string in US-ASCII charset—4 bytes Big Endian
            output.write("RIFF".toByteArray(asciiCharset))
            // Chunk size: The size of the actual sound data plus the rest
            //             of this header (36 bytes)—4 bytes Little Endian
            output.write(convertToLittleEndian(36 + rawData.size))
            // Format: "WAVE" string in US-ASCII charset—4 bytes Big Endian
            output.write("WAVE".toByteArray(asciiCharset))
            // Subchunk 1 ID: "fmt " string in US-ASCII charset—4 bytes Big Endian
            output.write("fmt ".toByteArray(asciiCharset))
            // Subchunk 1 size: The size of the subchunk.
            //                  It must be 16 for PCM—4 bytes Little Endian
            output.write(convertToLittleEndian(16))
            // Audio format: Use 1 for PCM—2 bytes Little Endian
            output.write(convertToLittleEndian(1.toShort()))
            // Number of channels: This sample only supports one channel—2 bytes Little Endian
            output.write(convertToLittleEndian(1.toShort()))
            // Sample rate: The sample rate in hertz—4 bytes Little Endian
            output.write(convertToLittleEndian(SAMPLE_RATE_IN_HZ))
            // Bit rate: SampleRate * NumChannels * BitsPerSample/8—4 bytes Little Endian
            output.write(convertToLittleEndian(SAMPLE_RATE_IN_HZ * 2))
            // Block align: NumChannels * BitsPerSample/8—2 bytes Little Endian
            output.write(convertToLittleEndian(2.toShort()))
            // Bits per sample: 16 bits—2 bytes Little Endian
            output.write(convertToLittleEndian(16.toShort()))
            // Subchunk 2 ID: "fmt " string in US-ASCII charset—4 bytes Big Endian
            output.write("data".toByteArray(asciiCharset))
            // Subchunk 2 size: The size of the actual audio data—4 bytes Little Endian
            output.write(convertToLittleEndian(rawData.size))

            // Audio data:  Sound data bytes—Little Endian
            val rawShorts = ShortArray(rawData.size / 2)
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(rawShorts)
            val bytes = ByteBuffer.allocate(rawData.size)
            for (s in rawShorts) {
                bytes.putShort(s)
            }

            output.write(readFile(rawFile))
        }
    }

    @Throws(IOException::class)
    private fun readFile(f: File): ByteArray {
        val size = f.length().toInt()
        val bytes = ByteArray(size)
        val tmpBuff = ByteArray(size)
        FileInputStream(f).use { fis ->
            var read = fis.read(bytes, 0, size)
            if (read < size) {
                var remain = size - read
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain)
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read)
                    remain -= read
                }
            }
        }
        return bytes
    }

    private fun convertToLittleEndian(value: Any): ByteArray {
        val size: Int = when (value.javaClass) {
            Short::class.javaObjectType -> 2
            Int::class.javaObjectType -> 4
            else -> throw IllegalArgumentException("Only int and short types are supported")
        }

        val littleEndianBytes = ByteArray(size)
        val byteBuffer = ByteBuffer.allocate(size)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        if (value.javaClass == Int::class.javaObjectType) {
            byteBuffer.putInt(value as Int)
        } else if (value.javaClass == Short::class.javaObjectType) {
            byteBuffer.putShort(value as Short)
        }

        byteBuffer.flip()
        byteBuffer.get(littleEndianBytes)

        return littleEndianBytes
    }

    interface RecordingListener {
        fun onRecordingSucceeded(output: File)
        fun onRecordingFailed(e: Exception)
    }
}
