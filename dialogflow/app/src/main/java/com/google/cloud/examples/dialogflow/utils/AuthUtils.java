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

package com.google.cloud.examples.dialogflow.utils;

import android.app.Activity;
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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AuthUtils {

    private static String firebaseInstanceId = "";
    private static FirebaseAuth firebaseAuth;

    public static String token = "";
    public static Date expiryTime;

    /**
     * function to call the firebase function which will send the fcm message containing token and
     * expiry time to the device
     */
    public static void callFirebaseFunction() {
        Map<String, String> data = new HashMap<>();
        data.put("deviceID", firebaseInstanceId);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("getOAuthToken")
                .call(data);
    }

    /**
     * function to store the token expiry time
     * @param expiryTime    :   expiry time in UTC timezone
     */
    public static void setExpiryTime(String expiryTime) {
            AuthUtils.expiryTime = getConvertedDateTime(expiryTime);
    }

    /**
     * function to convert the time from UTC to local TimeZone
     * @param expiryTime    :   expiry time in UTC timezone
     * @return  Date        :   converted datetime to local timezonne
     */
    private static Date getConvertedDateTime(String expiryTime) {
        try {
            final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
            DateTimeFormatter format = DateTimeFormatter.ofPattern(DATE_FORMAT);
            LocalDateTime ldt = LocalDateTime.parse(expiryTime, format);
            ZoneId fromZoneId = ZoneId.of(TimeZone.getTimeZone("UTC").getID());
            ZonedDateTime fromZoneDateTime = ldt.atZone(fromZoneId);
            ZoneId currentZoneId = TimeZone.getDefault().toZoneId();
            ZonedDateTime zonedDateTime = fromZoneDateTime.withZoneSameInstant(currentZoneId);
            return new SimpleDateFormat(DATE_FORMAT, Locale.US).parse(format.format(zonedDateTime));
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * function to signin to Firebase Anonymously
     * @param activity  :   Instance of the Activity
     */
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

    /**
     * function to get the firebase instance id
     */
    public static void getFirebaseInstanceId() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(
                new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String deviceToken = instanceIdResult.getToken();
                firebaseInstanceId = deviceToken;
                Log.i("fcmId", deviceToken);
            }
        });
    }

    /**
     * function to check if the token is valid
     * @return  boolean :   indicates the status of the signin
     */
    public static boolean isTokenValid() {
        return AuthUtils.expiryTime != null && !AuthUtils.token.equals("")
                && AuthUtils.expiryTime.getTime() > System.currentTimeMillis();
    }

}
