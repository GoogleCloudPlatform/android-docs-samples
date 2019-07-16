package com.google.cloud.examples.dialogflow.service;

import com.google.cloud.examples.dialogflow.AppController;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseInstanceIDService extends FirebaseMessagingService {
    private static final String TAG = MyFirebaseInstanceIDService.class.getSimpleName();

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        String refreshedToken = s;

        AppController.firebaseInstanceId = refreshedToken;

    }

}