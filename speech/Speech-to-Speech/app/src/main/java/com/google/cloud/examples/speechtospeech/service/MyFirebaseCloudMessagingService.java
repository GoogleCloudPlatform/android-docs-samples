
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

package com.google.cloud.examples.speechtospeech.service;

import android.content.Intent;
import android.util.Log;

import com.google.cloud.examples.speechtospeech.AppController;
import com.google.cloud.examples.speechtospeech.utils.AuthUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MyFirebaseCloudMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseCloudMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i("FirebaseMessage", "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.i(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }
    }

    /**
     * function to save the token data in the AppController
     *
     * @param expiryTime :   expiry time received from FCM
     * @param token      :   token received from FCM
     */
    private void handleNotification(String expiryTime, String token) {
        try {
            AuthUtils.expiryTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(expiryTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        AuthUtils.token = token;

        Intent intent = new Intent(AppController.TOKEN_RECEIVED);
        intent.putExtra("type", "token");
        sendBroadcast(intent);
    }

}