/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.android.speech;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;


/**
 * Retrieves an access token needed to call the Cloud Speech API.
 */
public class AccessTokenLoader extends AsyncTaskLoader<AccessToken> {

    private static final String TAG = "AccessTokenLoader";

    private static final String PREFS = "AccessTokenLoader";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    public AccessTokenLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public AccessToken loadInBackground() {
        final SharedPreferences prefs =
                getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
        long expirationTimeMillis = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);

        // Check if the current token is still valid for a while
        if (tokenValue != null && expirationTimeMillis > 0) {
            Date expirationTime = new Date(expirationTimeMillis);
            if (expirationTime.after(aWhileFromNow())) {
                return new AccessToken(tokenValue, expirationTime);
            }
        }

        // ***** WARNING *****
        // In this sample, we load the credential from a JSON file stored in a raw resource folder
        // of this client app. You should never do this in your app. Instead, store the file in your
        // server and obtain an access token from there.
        // *******************
        final InputStream stream = getContext().getResources().openRawResource(R.raw.credential);
        try {
            final GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(ApiFragment.SCOPE);
            final AccessToken token = credentials.refreshAccessToken();
            prefs.edit()
                    .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                    .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, token.getExpirationTime().getTime())
                    .apply();
            return token;
        } catch (IOException e) {
            Log.e(TAG, "Failed to obtain access token.", e);
        }
        return null;
    }

    private static Date aWhileFromNow() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, 30);
        return calendar.getTime();
    }

}
