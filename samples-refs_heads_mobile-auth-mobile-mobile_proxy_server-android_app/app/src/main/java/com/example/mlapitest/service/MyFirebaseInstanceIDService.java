package com.example.mlapitest.service;

import com.example.mlapitest.AppController;
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