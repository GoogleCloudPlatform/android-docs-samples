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

    public static void playAudio(byte[] mp3SoundByteArray) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            File tempMp3 = File.createTempFile("dialogFlow", "mp3", Environment.getExternalStorageDirectory());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(mp3SoundByteArray);
            fos.close();
            mediaPlayer.reset();
            FileInputStream fis = new FileInputStream(tempMp3);
            mediaPlayer.setDataSource(fis.getFD());

            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PROJECT_ID = getApplicationContext().getString(R.string.gcp_project_id);
    }

}
