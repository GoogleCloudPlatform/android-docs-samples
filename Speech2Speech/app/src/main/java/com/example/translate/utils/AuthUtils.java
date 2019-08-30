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

package com.example.translate.utils;

import android.app.Activity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AuthUtils {

    public static String firebaseInstanceId = "";
    public static FirebaseAuth firebaseAuth;

    public static String token = "";
    public static Date expiryTime;

    /**
     * function to call the firebase function which will send the fcm message containing token and expiry time to the device
     */
    public static void callFirebaseFunction() {
        Map<String, String> data = new HashMap<>();
        data.put("deviceID", firebaseInstanceId);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("getOAuthToken")
                .call(data);
    }

    public static void signInAnonymously(final Activity activity, OnCompleteListener<AuthResult> onCompleteListener) {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(activity, onCompleteListener);
    }

    /**
     * function to check the user is logged in
     *
     * @return boolean  : returns true if user is logged inn
     */
    public static boolean checkSignIn() {
        return firebaseAuth != null && firebaseAuth.getCurrentUser() != null;
    }

    public static void getFirebaseInstanceId(OnSuccessListener<InstanceIdResult> onSuccessListener) {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(onSuccessListener);
    }

}
