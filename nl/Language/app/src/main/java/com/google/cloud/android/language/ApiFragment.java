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
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.language.v1.CloudNaturalLanguage;
import com.google.api.services.language.v1.CloudNaturalLanguageRequest;
import com.google.api.services.language.v1.CloudNaturalLanguageScopes;
import com.google.api.services.language.v1.model.AnalyzeEntitiesRequest;
import com.google.api.services.language.v1.model.AnalyzeEntitiesResponse;
import com.google.api.services.language.v1.model.AnalyzeSentimentRequest;
import com.google.api.services.language.v1.model.AnalyzeSentimentResponse;
import com.google.api.services.language.v1.model.AnnotateTextRequest;
import com.google.api.services.language.v1.model.AnnotateTextResponse;
import com.google.api.services.language.v1.model.Document;
import com.google.api.services.language.v1.model.Entity;
import com.google.api.services.language.v1.model.Features;
import com.google.api.services.language.v1.model.Token;
import com.google.cloud.android.language.model.EntityInfo;
import com.google.cloud.android.language.model.SentimentInfo;
import com.google.cloud.android.language.model.TokenInfo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * Handles all the API requests of Cloud Natural Language API.
 *
 * <p>This is a <em>retained</em> Fragment. This should not hold any reference to Context or Views.
 * </p>
 */
public class ApiFragment extends Fragment {

    public interface Callback {

        /**
         * Called when an "entities" API request is complete.
         *
         * @param entities The entities.
         */
        void onEntitiesReady(EntityInfo[] entities);

        /**
         * Called when a "sentiment" API request is complete.
         *
         * @param sentiment The sentiment.
         */
        void onSentimentReady(SentimentInfo sentiment);

        /**
         * Called when a "syntax" API request is complete.
         *
         * @param tokens The tokens.
         */
        void onSyntaxReady(TokenInfo[] tokens);
    }

    private static final String TAG = "ApiFragment";

    private GoogleCredential mCredential;

    private CloudNaturalLanguage mApi = new CloudNaturalLanguage.Builder(
            new NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) throws IOException {
                    mCredential.initialize(request);
                }
            }).build();

    private final BlockingQueue<CloudNaturalLanguageRequest<? extends GenericJson>> mRequests
            = new ArrayBlockingQueue<>(3);

    private Thread mThread;

    private Callback mCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Fragment parent = getParentFragment();
        mCallback = parent != null ? (Callback) parent : (Callback) context;
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
    }

    public void setAccessToken(String token) {
        mCredential = new GoogleCredential()
                .setAccessToken(token)
                .createScoped(CloudNaturalLanguageScopes.all());
        startWorkerThread();
    }

    public void analyzeEntities(String text) {
        try {
            // Create a new entities API call request and add it to the task queue
            mRequests.add(mApi
                    .documents()
                    .analyzeEntities(new AnalyzeEntitiesRequest()
                            .setDocument(new Document()
                                    .setContent(text)
                                    .setType("PLAIN_TEXT"))));
        } catch (IOException e) {
            Log.e(TAG, "Failed to create analyze request.", e);
        }
    }

    public void analyzeSentiment(String text) {
        try {
            mRequests.add(mApi
                    .documents()
                    .analyzeSentiment(new AnalyzeSentimentRequest()
                            .setDocument(new Document()
                                    .setContent(text)
                                    .setType("PLAIN_TEXT"))));
        } catch (IOException e) {
            Log.e(TAG, "Failed to create analyze request.", e);
        }
    }

    public void analyzeSyntax(String text) {
        try {
            mRequests.add(mApi
                    .documents()
                    .annotateText(new AnnotateTextRequest()
                            .setDocument(new Document()
                                    .setContent(text)
                                    .setType("PLAIN_TEXT"))
                            .setFeatures(new Features()
                                    .setExtractSyntax(true))));
        } catch (IOException e) {
            Log.e(TAG, "Failed to create analyze request.", e);
        }
    }

    private void startWorkerThread() {
        if (mThread != null) {
            return;
        }
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (mThread == null) {
                        break;
                    }
                    try {
                        // API calls are executed here in this worker thread
                        deliverResponse(mRequests.take().execute());
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted.", e);
                        break;
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to execute a request.", e);
                    }
                }
            }
        });
        mThread.start();
    }

    private void deliverResponse(GenericJson response) {
        final Activity activity = getActivity();
        if (response instanceof AnalyzeEntitiesResponse) {
            final List<Entity> entities = ((AnalyzeEntitiesResponse) response).getEntities();
            final int size = entities.size();
            final EntityInfo[] array = new EntityInfo[size];
            for (int i = 0; i < size; i++) {
                array[i] = new EntityInfo(entities.get(i));
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onEntitiesReady(array);
                    }
                }
            });
        } else if (response instanceof AnalyzeSentimentResponse) {
            final SentimentInfo sentiment = new SentimentInfo(((AnalyzeSentimentResponse) response)
                    .getDocumentSentiment());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onSentimentReady(sentiment);
                    }
                }
            });
        } else if (response instanceof AnnotateTextResponse) {
            final List<Token> tokens = ((AnnotateTextResponse) response).getTokens();
            final int size = tokens.size();
            final TokenInfo[] array = new TokenInfo[size];
            for (int i = 0; i < size; i++) {
                array[i] = new TokenInfo(tokens.get(i));
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onSyntaxReady(array);
                    }
                }
            });
        }
    }

}
