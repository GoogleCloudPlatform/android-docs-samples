package com.google.cloud.examples.dialogflow;

import android.app.Application;

import com.google.firebase.functions.FirebaseFunctions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AppController extends Application {

    public static final String TOKEN_RECEIVED = "TOKEN_RECEIVED";
    public static final String PROJECT_ID = "PROJECT_ID";
    public static final String SESSION_ID = "sessionId";
    public static String firebaseInstanceId = "";

    public static String token = "";
    public static Date exipryTime;

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

    /**
     * function to convert string to date
     * @param dt    :   string date
     * @return      :   converted date object
     */
    public static Date getDateFromString(String dt) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
            Date date = format.parse(dt);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


}
