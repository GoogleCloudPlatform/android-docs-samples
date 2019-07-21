package com.google.cloud.examples.dialogflow.service;

import com.google.cloud.examples.dialogflow.AppController;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseInstanceIDService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String refreshedToken) {
        super.onNewToken(refreshedToken);

        AppController.firebaseInstanceId = refreshedToken;

    }

}