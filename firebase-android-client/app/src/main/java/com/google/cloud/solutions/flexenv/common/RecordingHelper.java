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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Singleton class that records audio from the microphone on the device and writes it to a file in
 * PCM-16 (wave) format.
 */
public class RecordingHelper {
    // [START recording_parameters]
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.UNPROCESSED;
    private static final int SAMPLE_RATE_IN_HZ = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    // [END recording_parameters]
    private static final int RECORD_PERMISSIONS_REQUEST_CODE = 15623;
    private static final String TAG = "RecordingHelper";

    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT);
    private static final String RAW_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/speech-recording.raw";
    private static final String WAV_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/speech-recording.wav";

    private static RecordingHelper _instance;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private BufferedOutputStream outputStream;

    private RecordingHelper() { }

    public static RecordingHelper getInstance() {
        if(_instance == null) {
            _instance = new RecordingHelper();
        }
        return _instance;
    }

    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Starts recording audio from the device microphone. The client must call stopRecording()
     * before this method can process the recorded audio and write the audio file to disk.
     * @param recordingListener The callback to deliver the results to.
     */
    public void startRecording(final RecordingListener recordingListener) {
        isRecording = true;

        new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            byte data[] = new byte[BUFFER_SIZE];
            audioRecord = new AudioRecord(
                    AUDIO_SOURCE,
                    SAMPLE_RATE_IN_HZ,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
            );

            audioRecord.startRecording();

            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(RAW_FILE_PATH));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Couldn't find file: " + RAW_FILE_PATH, e);
                recordingListener.onRecordingFailed(e);
            }

            // This loop runs until the client calls stopRecording().
            while (isRecording) {
                int status = audioRecord.read(data, 0, data.length);

                if (status == AudioRecord.ERROR_INVALID_OPERATION || status == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Couldn't read data");
                    recordingListener.onRecordingFailed(new IOException());
                }

                try {
                    outputStream.write(data, 0, data.length);
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't save data", e);
                    recordingListener.onRecordingFailed(e);
                }
            }

            // After the client calls stopRecording(), this method processes the recorded audio.
            try {
                outputStream.close();
                audioRecord.stop();
                audioRecord.release();

                Log.v(TAG, "Recording stopped");

                File rawFile = new File(RAW_FILE_PATH);
                File wavFile = new File(WAV_FILE_PATH);
                saveAsWave(rawFile, wavFile);
                recordingListener.onRecordingSucceeded(wavFile);
            } catch (IOException e) {
                Log.e(TAG, "File error", e);
                recordingListener.onRecordingFailed(e);
            }
        }).start();
    }

    public void stopRecording() {
        isRecording = false;
    }

    public boolean hasRequiredPermissions(Context context) {
        int recordAudioPermissionCheck = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO);
        int writeExternalStoragePermissionCheck = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return recordAudioPermissionCheck == PackageManager.PERMISSION_GRANTED &&
                writeExternalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    public void requestRequiredPermissions(Activity activity) {
        activity.requestPermissions(
                new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                RECORD_PERMISSIONS_REQUEST_CODE
        );
    }

    private void saveAsWave(final File rawFile, final File waveFile) throws IOException {
        byte[] rawData = new byte[(int) rawFile.length()];
        try (DataInputStream input = new DataInputStream(new FileInputStream(rawFile))) {
            int readBytes;
            do {
                readBytes = input.read(rawData);
            }
            while(readBytes != -1);
        }

        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(waveFile))) {
            // WAVE specification
            Charset asciiCharset = Charset.forName("US-ASCII");
            // Chunk ID: "RIFF" string in US-ASCII charset—4 bytes Big Endian
            output.write("RIFF".getBytes(asciiCharset));
            // Chunk size: The size of the actual sound data plus the rest
            //             of this header (36 bytes)—4 bytes Little Endian
            output.write(convertToLittleEndian(36 + rawData.length));
            // Format: "WAVE" string in US-ASCII charset—4 bytes Big Endian
            output.write("WAVE".getBytes(asciiCharset));
            // Subchunk 1 ID: "fmt " string in US-ASCII charset—4 bytes Big Endian
            output.write("fmt ".getBytes(asciiCharset));
            // Subchunk 1 size: The size of the subchunk.
            //                  It must be 16 for PCM—4 bytes Little Endian
            output.write(convertToLittleEndian(16));
            // Audio format: Use 1 for PCM—2 bytes Little Endian
            output.write(convertToLittleEndian((short)1));
            // Number of channels: This sample only supports one channel—2 bytes Little Endian
            output.write(convertToLittleEndian((short)1));
            // Sample rate: The sample rate in hertz—4 bytes Little Endian
            output.write(convertToLittleEndian(SAMPLE_RATE_IN_HZ));
            // Bit rate: SampleRate * NumChannels * BitsPerSample/8—4 bytes Little Endian
            output.write(convertToLittleEndian(SAMPLE_RATE_IN_HZ * 2));
            // Block align: NumChannels * BitsPerSample/8—2 bytes Little Endian
            output.write(convertToLittleEndian((short)2));
            // Bits per sample: 16 bits—2 bytes Little Endian
            output.write(convertToLittleEndian((short)16));
            // Subchunk 2 ID: "fmt " string in US-ASCII charset—4 bytes Big Endian
            output.write("data".getBytes(asciiCharset));
            // Subchunk 2 size: The size of the actual audio data—4 bytes Little Endian
            output.write(convertToLittleEndian(rawData.length));

            // Audio data:  Sound data bytes—Little Endian
            short[] rawShorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(rawShorts);
            ByteBuffer bytes = ByteBuffer.allocate(rawData.length);
            for (short s : rawShorts) {
                bytes.putShort(s);
            }

            output.write(readFile(rawFile));
        }
    }

    private byte[] readFile(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        try (FileInputStream fis = new FileInputStream(f)) {
            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }
        return bytes;
    }

    private byte[] convertToLittleEndian(Object value) {
        int size;
        if(value.getClass().equals(Integer.class)) {
            size = 4;
        } else if (value.getClass().equals(Short.class)) {
            size = 2;
        } else {
            throw new IllegalArgumentException("Only int and short types are supported");
        }

        byte[] littleEndianBytes = new byte[size];
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        if(value.getClass().equals(Integer.class)) {
            byteBuffer.putInt((int)value);
        } else if (value.getClass().equals(Short.class)) {
            byteBuffer.putShort((short)value);
        }

        byteBuffer.flip();
        byteBuffer.get(littleEndianBytes);

        return littleEndianBytes;
    }

    public interface RecordingListener {
        void onRecordingSucceeded(File output);
        void onRecordingFailed(Exception e);
    }
}
