/*
 * Copyright 2019 Google LLC
 *
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

package com.google.cloud.examples.dialogflow;

import android.app.Application;
import android.media.MediaPlayer;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AppController extends Application {

    public static final String TOKEN_RECEIVED = "TOKEN_RECEIVED";
    public static final String SESSION_ID = "sessionId";
    public static String PROJECT_ID = "";

    public static void playAudio(byte[] byteArray) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            File tempFile = File.createTempFile("dialogFlow", null,
                    Environment.getExternalStorageDirectory());
            tempFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(byteArray);
            fos.close();
            mediaPlayer.reset();
            FileInputStream fis = new FileInputStream(tempFile);
            mediaPlayer.setDataSource(fis.getFD());

            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PROJECT_ID = getApplicationContext().getString(R.string.gcp_project_id);
    }

}
