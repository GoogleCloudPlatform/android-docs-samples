package com.example.mlapitest.service;

import android.content.Intent;
import android.util.Log;

import com.example.mlapitest.AppController;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.e("FirebaseMessage", "From: " + remoteMessage.getFrom());

        if (remoteMessage == null)
            return;

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }
    }

    /**
     * function to save the token data in the AppController
     * @param title     :   expiry time received from FCM
     * @param message   :   token received from FCM
     */
    private void handleNotification(String title, String message) {
        AppController.exipryTime = AppController.getDateFromStrinng(title);
        AppController.token = message;

        Intent intent = new Intent(AppController.TOKEN_RECEIVED);
        sendBroadcast(intent);
    }

    private void handleDataMessage(JSONObject json) {
        Log.e(TAG, "push json: " + json.toString());
    }
}