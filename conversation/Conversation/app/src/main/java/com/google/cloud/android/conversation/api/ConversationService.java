/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.android.conversation.api;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.android.conversation.BuildConfig;
import com.google.cloud.android.conversation.R;
import com.google.cloud.conversation.v1alpha.AudioEncoding;
import com.google.cloud.conversation.v1alpha.ConversationServiceGrpc;
import com.google.cloud.conversation.v1alpha.DetectIntentRequest;
import com.google.cloud.conversation.v1alpha.DetectIntentResponse;
import com.google.cloud.conversation.v1alpha.InputAudioConfig;
import com.google.cloud.conversation.v1alpha.QueryInput;
import com.google.cloud.conversation.v1alpha.QueryResult;
import com.google.cloud.conversation.v1alpha.StreamingDetectIntentRequest;
import com.google.cloud.conversation.v1alpha.StreamingDetectIntentResponse;
import com.google.cloud.conversation.v1alpha.StreamingInputAudioConfig;
import com.google.cloud.conversation.v1alpha.StreamingQueryInput;
import com.google.cloud.conversation.v1alpha.StreamingQueryParameters;
import com.google.cloud.conversation.v1alpha.StreamingRecognitionResult;
import com.google.cloud.conversation.v1alpha.TextInput;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;


/**
 * This service interacts with Cloud Conversation Engine.
 */
public class ConversationService extends Service {

    /**
     * Callback listener for {@link ConversationService}.
     */
    public interface Listener {

        /**
         * Called when the API is ready.
         */
        void onApiReady();

        /**
         * Called when a new {@link Utterance} occurred from either side of the conversation.
         *
         * @param utterance A new {@link Utterance}.
         */
        void onNewUtterance(Utterance utterance);

        /**
         * Called when a new piece of text is recognized in the speech recognition.
         *
         * @param text A new recognition result.
         */
        void onNewRecognition(String text);

    }

    private static final String TAG = "ConversationService";

    private static final String PREFS = "ConversationService";

    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    /** We reuse an access token if its expiration time is longer than this. */
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes
    /** We refresh the current access token before it expires. */
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute

    private static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    private static final String HOSTNAME = "conversation.googleapis.com";
    private static final int PORT = 443;

    /** The unique ID for this conversation; this should be changed as the session changes. */
    private static final int SESSION_ID = 1234567;

    private final ConversationBinder mBinder = new ConversationBinder();

    private ConversationServiceGrpc.ConversationServiceStub mApi;

    private final ArrayList<Listener> mListeners = new ArrayList<>();

    private Handler mHandler;

    private final Runnable mFetchAccessTokenRunnable = new Runnable() {
        @Override
        public void run() {
            prepareApi();
        }
    };

    private AccessTokenTask mAccessTokenTask;

    private final StreamObserver<DetectIntentResponse> mTextResponseObserver
            = new StreamObserver<DetectIntentResponse>() {

        @Override
        public void onNext(DetectIntentResponse detectIntentResponse) {
            if (mHandler == null) {
                return;
            }
            final String text = detectIntentResponse.getQueryResult().getFulfillment().getText();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    dispatchNewUtterance(new Utterance(Utterance.INCOMING, text));
                }
            });
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(TAG, "onError: ", throwable);
        }

        @Override
        public void onCompleted() {
            Log.d(TAG, "onCompleted");
        }

    };

    private StreamObserver<StreamingDetectIntentResponse> mVoiceResponseObserver
            = new StreamObserver<StreamingDetectIntentResponse>() {
        @Override
        public void onNext(StreamingDetectIntentResponse response) {
            if (mHandler == null) {
                return;
            }
            final StreamingRecognitionResult recognitionResult = response.getRecognitionResult();
            if (recognitionResult != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (recognitionResult.getIsFinal()) {
                            dispatchNewUtterance(new Utterance(Utterance.OUTGOING,
                                    recognitionResult.getTranscript()));
                        } else {
                            dispatchNewRecognition(recognitionResult.getTranscript());
                        }
                    }
                });
            }
            final QueryResult queryResult = response.getQueryResult();
            if (queryResult != null && queryResult.hasIntent()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dispatchNewUtterance(new Utterance(Utterance.INCOMING,
                                queryResult.getFulfillment().getText()));
                    }
                });
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (throwable instanceof StatusRuntimeException) {
                if (((StatusRuntimeException) throwable).getStatus().getCode() ==
                        Status.Code.NOT_FOUND) {
                    // Probably the audio didn't contain speech; ignore.
                    return;
                }
            }
            Log.e(TAG, "Error while detecting intent by voice: ", throwable);
        }

        @Override
        public void onCompleted() {
            Log.d(TAG, "Detect intent by voice completed.");
        }
    };

    private StreamObserver<StreamingDetectIntentRequest> mRequestObserver;

    public static ConversationService from(IBinder binder) {
        return ((ConversationBinder) binder).getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prepareApi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseApi();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void addListener(@NonNull Listener listener) {
        mListeners.add(listener);
        if (mApi != null) {
            listener.onApiReady();
        }
    }

    public void removeListener(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    public boolean isApiReady() {
        return mApi != null;
    }

    public void detectIntentByText(String text) {
        if (mApi == null) {
            Log.w(TAG, "API not ready. Ignoring the request.");
            return;
        }
        dispatchNewUtterance(new Utterance(Utterance.OUTGOING, text));
        mApi.detectIntent(DetectIntentRequest.newBuilder()
                .setSession(createSessionName(BuildConfig.PROJECT_NAME,
                        BuildConfig.AGENT_NAME, SESSION_ID))
                .setQueryInput(QueryInput.newBuilder()
                        .setText(TextInput.newBuilder()
                                .setLanguageCode(BuildConfig.LANGUAGE_CODE)
                                .setText(text)))
                .build(), mTextResponseObserver);
    }

    public void startDetectIntentByVoice(int sampleRate) {
        if (mApi == null) {
            Log.w(TAG, "API not ready. Ignoring the request.");
            return;
        }
        mRequestObserver = mApi.streamingDetectIntent(mVoiceResponseObserver);
        mRequestObserver.onNext(StreamingDetectIntentRequest.newBuilder()
                .setQueryParams(StreamingQueryParameters.newBuilder()
                        .setSession(createSessionName(BuildConfig.PROJECT_NAME,
                                BuildConfig.AGENT_NAME, SESSION_ID)))
                .setQueryInput(StreamingQueryInput.newBuilder()
                        .setAudioConfig(StreamingInputAudioConfig.newBuilder()
                                .setConfig(InputAudioConfig.newBuilder()
                                        .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_LINEAR16)
                                        .setLanguageCode(BuildConfig.LANGUAGE_CODE)
                                        .setSampleRateHertz(sampleRate))))
                .build());
    }

    public void detectIntentByVoice(byte[] data, int size) {
        if (mRequestObserver == null) {
            return;
        }
        mRequestObserver.onNext(StreamingDetectIntentRequest.newBuilder()
                .setInputAudio(ByteString.copyFrom(data, 0, size))
                .build());
    }

    public void finishDetectIntentByVoice() {
        if (mRequestObserver == null) {
            return;
        }
        mRequestObserver.onCompleted();
        mRequestObserver = null;
    }

    private void prepareApi() {
        if (mAccessTokenTask != null) {
            return;
        }
        mHandler = new Handler();
        mAccessTokenTask = new AccessTokenTask();
        mAccessTokenTask.execute();
    }

    private void releaseApi() {
        mHandler.removeCallbacks(mFetchAccessTokenRunnable);
        mHandler = null;
        // Release the gRPC channel.
        if (mApi != null) {
            final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error shutting down the gRPC channel", e);
                }
            }
            mApi = null;
        }
    }

    private void dispatchApiReady() {
        for (int i = 0, size = mListeners.size(); i < size; i++) {
            mListeners.get(i).onApiReady();
        }
    }

    private void dispatchNewUtterance(Utterance utterance) {
        for (int i = 0, size = mListeners.size(); i < size; i++) {
            mListeners.get(i).onNewUtterance(utterance);
        }
    }

    private void dispatchNewRecognition(String text) {
        for (int i = 0, size = mListeners.size(); i < size; i++) {
            mListeners.get(i).onNewRecognition(text);
        }
    }

    private String createSessionName(String project, String agent, int id) {
        return String.format(Locale.US, "projects/%s/agents/%s/sessions/%d", project, agent, id);
    }

    private class ConversationBinder extends Binder {

        ConversationService getService() {
            return ConversationService.this;
        }

    }

    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

        @Override
        protected AccessToken doInBackground(Void... voids) {
            final SharedPreferences prefs = getApplication()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            final String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
            long expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);

            // Check if the current token is still valid for a while
            if (tokenValue != null && expirationTime > 0) {
                if (expirationTime
                        > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE) {
                    return new AccessToken(tokenValue, new Date(expirationTime));
                }
            }
            // ***** WARNING *****
            // In this sample, we load the credential from a JSON file stored in a raw resource
            // folder of this client app. You should never do this in your app. Instead, store
            // the file in your server and obtain an access token from there.
            // *******************
            final InputStream stream = getApplication().getResources()
                    .openRawResource(R.raw.credential);
            try {
                final GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                        .createScoped(SCOPE);
                final AccessToken token = credentials.refreshAccessToken();
                prefs.edit()
                        .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                        .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME,
                                token.getExpirationTime().getTime())
                        .apply();
                return token;
            } catch (IOException e) {
                Log.e(TAG, "Failed to obtain access token.", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AccessToken accessToken) {
            final ManagedChannel channel = new OkHttpChannelProvider()
                    .builderForAddress(HOSTNAME, PORT)
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .intercept(new GoogleCredentialsInterceptor(new GoogleCredentials(accessToken)
                            .createScoped(SCOPE)))
                    .build();
            mApi = ConversationServiceGrpc.newStub(channel);
            dispatchApiReady();

            // Schedule access token refresh before it expires
            if (mHandler != null) {
                mHandler.postDelayed(mFetchAccessTokenRunnable,
                        Math.max(accessToken.getExpirationTime().getTime()
                                        - System.currentTimeMillis() - ACCESS_TOKEN_FETCH_MARGIN,
                                ACCESS_TOKEN_EXPIRATION_TOLERANCE));
            }
        }

    }

    /**
     * Authenticates the gRPC channel using the specified {@link GoogleCredentials}.
     */
    private static class GoogleCredentialsInterceptor implements ClientInterceptor {

        private final Credentials mCredentials;

        private Metadata mCached;

        private Map<String, List<String>> mLastMetadata;

        GoogleCredentialsInterceptor(Credentials credentials) {
            mCredentials = credentials;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                final MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
                final Channel next) {
            return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {
                @Override
                protected void checkedStart(Listener<RespT> responseListener, Metadata headers)
                        throws StatusException {
                    Metadata cachedSaved;
                    URI uri = serviceUri(next, method);
                    synchronized (this) {
                        Map<String, List<String>> latestMetadata = getRequestMetadata(uri);
                        if (mLastMetadata == null || mLastMetadata != latestMetadata) {
                            mLastMetadata = latestMetadata;
                            mCached = toHeaders(mLastMetadata);
                        }
                        cachedSaved = mCached;
                    }
                    headers.merge(cachedSaved);
                    delegate().start(responseListener, headers);
                }
            };
        }

        /**
         * Generate a JWT-specific service URI. The URI is simply an identifier with enough
         * information for a service to know that the JWT was intended for it. The URI will
         * commonly be verified with a simple string equality check.
         */
        private URI serviceUri(Channel channel, MethodDescriptor<?, ?> method)
                throws StatusException {
            String authority = channel.authority();
            if (authority == null) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Channel has no authority")
                        .asException();
            }
            // Always use HTTPS
            final String scheme = "https";
            final int defaultPort = 443;
            String path = "/" + MethodDescriptor.extractFullServiceName(method.getFullMethodName());
            URI uri;
            try {
                uri = new URI(scheme, authority, path, null, null);
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI for auth")
                        .withCause(e).asException();
            }
            // The default port must not be present. Alternative ports should be present.
            if (uri.getPort() == defaultPort) {
                uri = removePort(uri);
            }
            return uri;
        }

        private URI removePort(URI uri) throws StatusException {
            try {
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), -1 /* port */,
                        uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI after removing port")
                        .withCause(e).asException();
            }
        }

        private Map<String, List<String>> getRequestMetadata(URI uri) throws StatusException {
            try {
                return mCredentials.getRequestMetadata(uri);
            } catch (IOException e) {
                throw Status.UNAUTHENTICATED.withCause(e).asException();
            }
        }

        private static Metadata toHeaders(Map<String, List<String>> metadata) {
            Metadata headers = new Metadata();
            if (metadata != null) {
                for (String key : metadata.keySet()) {
                    Metadata.Key<String> headerKey = Metadata.Key.of(
                            key, Metadata.ASCII_STRING_MARSHALLER);
                    for (String value : metadata.get(key)) {
                        headers.put(headerKey, value);
                    }
                }
            }
            return headers;
        }

    }

}
