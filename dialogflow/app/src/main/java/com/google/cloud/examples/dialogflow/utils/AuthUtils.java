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

    public static void getFirebaseInstanceId() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String deviceToken = instanceIdResult.getToken();
                firebaseInstanceId = deviceToken;
                Log.i("fcmId", deviceToken);
            }
        });
    }

}
