/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.examples.language;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.language.v1.AnalyzeEntitiesRequest;
import com.google.cloud.language.v1.AnalyzeEntitiesResponse;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;

import java.io.IOException;

/**
 * This example demonstrates using the Cloud Natural Language client library to
 * perform text analysis.
 */
public class MainActivity extends AppCompatActivity {

    private LanguageServiceClient mLanguageClient;
    private TextView mResultText;
    private TextView mWaitText;
    private View mProgressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultText = findViewById(R.id.result_text);
        mWaitText = findViewById(R.id.wait_text);
        mProgressView = findViewById(R.id.progress);

        // create the language client
        try {
            // NOTE: The line below uses an embedded credential (res/raw/credential.json).
            //       You should not package a credential with real application.
            //       Instead, you should get a credential securely from a server.
            mLanguageClient = LanguageServiceClient.create(
                    LanguageServiceSettings.newBuilder()
                            .setCredentialsProvider(() ->
                                    GoogleCredentials.fromStream(getApplicationContext()
                                            .getResources()
                                            .openRawResource(R.raw.credential)))
                            .build());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create a language client", e);
        }

        // get the text to analyze
        String text = "Hi there Sunnyvale California!";
        mWaitText.setText(getString(R.string.waiting_for_response, text));

        // call the API
        new AnalyzeTask().execute(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // shutdown the connection
        mLanguageClient.shutdown();
    }

    private class AnalyzeTask extends AsyncTask<String, Void, AnalyzeEntitiesResponse> {

        @Override
        protected AnalyzeEntitiesResponse doInBackground(String... args) {
            AnalyzeEntitiesRequest request = AnalyzeEntitiesRequest.newBuilder()
                    .setDocument(Document.newBuilder()
                            .setContent(args[0])
                            .setType(Document.Type.PLAIN_TEXT)
                            .build())
                    .build();
            return mLanguageClient.analyzeEntities(request);
        }

        @Override
        protected void onPostExecute(AnalyzeEntitiesResponse analyzeEntitiesResponse) {
            // update UI with results
            mProgressView.setVisibility(View.GONE);
            mResultText.setText(analyzeEntitiesResponse.toString());
        }
    }

}
