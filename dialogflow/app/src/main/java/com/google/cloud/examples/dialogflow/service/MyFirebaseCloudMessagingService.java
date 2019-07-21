package com.google.cloud.examples.dialogflow.service;

import android.content.Intent;
import android.util.Log;

import com.google.cloud.examples.dialogflow.AppController;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class MyFirebaseCloudMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseCloudMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.e("FirebaseMessage", "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }
    }

    /**
     * function to save the token data in the AppController
     * @param expiryTime     :   expiry time received from FCM
     * @param token   :   token received from FCM
     */
    private void handleNotification(String expiryTime, String token) {
        try {
            AppController.expiryTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(expiryTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        AppController.token = token;

        Intent intent = new Intent(AppController.TOKEN_RECEIVED);
        sendBroadcast(intent);
    }

}