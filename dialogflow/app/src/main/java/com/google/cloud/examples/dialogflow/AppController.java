package com.google.cloud.examples.dialogflow;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AppController extends Application {

    public static final String TOKEN_RECEIVED = "TOKEN_RECEIVED";
    public static final String SESSION_ID = "sessionId";
    public static String PROJECT_ID = "";
    public static String firebaseInstanceId = "";
    public static FirebaseAuth firebaseAuth;

    public static String token = "";
    public static Date expiryTime;

    public static Context context;

    /**
     * function to call the firebase function which will send the fcm message containing token and expiry time to the device
     */
    public static void callFirebaseFunction() {
        Map<String, String> data = new HashMap<>();
        data.put("deviceID", AppController.firebaseInstanceId);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("getOAuthToken")
                .call(data);
    }

    public static void signInAnonymously(final Activity activity) {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(activity, "Sign In was successful",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(activity, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    /**
     * function to check the user is logged in
     *
     * @return boolean  : returns true if user is logged inn
     */
    public static boolean checkSignIn() {
        return firebaseAuth != null && firebaseAuth.getCurrentUser() != null;
    }

    public static void getFirebaseInstanceId() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String deviceToken = instanceIdResult.getToken();
                AppController.firebaseInstanceId = deviceToken;
                Log.i("fcmId", deviceToken);
            }
        });
    }

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
        context = getApplicationContext();
        PROJECT_ID = getApplicationContext().getString(R.string.gcp_project_id);
    }

}
