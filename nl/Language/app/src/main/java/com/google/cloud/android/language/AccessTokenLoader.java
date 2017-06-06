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

package com.google.cloud.android.language;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.language.v1.CloudNaturalLanguageScopes;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;


public class AccessTokenLoader extends AsyncTaskLoader<String> {

    private static final String TAG = "AccessTokenLoader";

    private static final String PREFS = "AccessTokenLoader";
    private static final String PREF_ACCESS_TOKEN = "access_token";

    public AccessTokenLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public String loadInBackground() {

        final SharedPreferences prefs =
                getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String currentToken = prefs.getString(PREF_ACCESS_TOKEN, null);

        // Check if the current token is still valid for a while
        if (currentToken != null) {
            final GoogleCredential credential = new GoogleCredential()
                    .setAccessToken(currentToken)
                    .createScoped(CloudNaturalLanguageScopes.all());
            final Long seconds = credential.getExpiresInSeconds();
            if (seconds != null && seconds > 3600) {
                return currentToken;
            }
        }

        // ***** WARNING *****
        // In this sample, we load the credential from a JSON file stored in a raw resource folder
        // of this client app. You should never do this in your app. Instead, store the file in your
        // server and obtain an access token from there.
        // *******************
        final InputStream stream = getContext().getResources().openRawResource(R.raw.credential);
        try {
            final GoogleCredential credential = GoogleCredential.fromStream(stream)
                    .createScoped(CloudNaturalLanguageScopes.all());
            credential.refreshToken();
            final String accessToken = credential.getAccessToken();
            prefs.edit().putString(PREF_ACCESS_TOKEN, accessToken).apply();
            return accessToken;
        } catch (IOException e) {
            Log.e(TAG, "Failed to obtain access token.", e);
        }
        return null;
    }

}
