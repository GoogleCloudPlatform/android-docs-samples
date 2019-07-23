package com.google.cloud.examples.dialogflow;

import android.app.Application;

import com.google.firebase.functions.FirebaseFunctions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AppController extends Application {

    public static final String TOKEN_RECEIVED = "TOKEN_RECEIVED";
    public static String PROJECT_ID = "";
    public static final String SESSION_ID = "sessionId";
    public static String firebaseInstanceId = "";

    public static String token = "";
    public static Date expiryTime;

    @Override
    public void onCreate() {
        super.onCreate();
        PROJECT_ID = getApplicationContext().getString(R.string.gcp_project_id);
    }

    /**
     * function to call the firebase function which will send the fcm message containing token and expiry time to the device
     */
    public static void callFirebaseFunction() {
        Map<String, String> data = new HashMap<>();
        data.put("deviceId", AppController.firebaseInstanceId);

        FirebaseFunctions firebaseFunctions;
        firebaseFunctions = FirebaseFunctions.getInstance();

        firebaseFunctions
                .getHttpsCallable("getOAuthToken")
                .call(data);
    }


}
