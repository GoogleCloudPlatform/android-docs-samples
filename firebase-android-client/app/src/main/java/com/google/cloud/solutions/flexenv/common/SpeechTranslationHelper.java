/*
 * Copyright 2016 Google LLC.
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

package com.google.cloud.solutions.flexenv.common;

import android.content.Context;
import android.util.Log;

import com.google.cloud.solutions.flexenv.R;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UploadDataProviders;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Singleton class that makes requests to a Google Cloud Function that translates speech messages.
 * For more information about the function implementation, see
 * https://github.com/GoogleCloudPlatform/nodejs-docs-samples/tree/master/functions/speech-to-speech
 */
public class SpeechTranslationHelper {
    private static final String TAG = "SpeechTranslationHelper";
    private static final String SPEECH_TRANSLATE_HTTP_METHOD = "POST";
    private static final String SPEECH_TRANSLATE_CONTENT_TYPE = "application/json";
    private static final String SPEECH_TRANSLATE_ENCODING = "LINEAR16";

    private static SpeechTranslationHelper _instance;

    private SpeechTranslationHelper() { }

    public static SpeechTranslationHelper getInstance() {
        if(_instance == null) {
            _instance = new SpeechTranslationHelper();
        }
        return _instance;
    }

    /**
     * Performs a request to a Google Cloud Function that translates speech messages. Returns a JSON
     * string with information about the response. The response includes information about the audio
     * files that the client can download at a different time.
     * @param context The application context
     * @param base64EncodedAudioMessage The base64-encoded audio message
     * @param sampleRateInHertz The sample rate in hertz
     * @param translationListener The callback to deliver the results to.
     */
    public void translateAudioMessage(Context context, CronetEngine cronetEngine,
                                      String base64EncodedAudioMessage, int sampleRateInHertz,
                                      SpeechTranslationListener translationListener) {
        // [START json_request_body]
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("encoding", SPEECH_TRANSLATE_ENCODING);
            requestBody.put("sampleRateHertz", sampleRateInHertz);
            requestBody.put("languageCode", context.getResources().getConfiguration().getLocales().get(0));
            requestBody.put("audioContent", base64EncodedAudioMessage);
        } catch(JSONException e) {
            Log.e(TAG, e.getLocalizedMessage());
            translationListener.onTranslationFailed(e);
        }
        // [END json_request_body]

        byte[] requestBodyBytes = requestBody.toString().getBytes();
        UrlRequest request = buildSpeechTranslationRequest(context, cronetEngine, requestBodyBytes, translationListener);
        request.start();
    }

    private UrlRequest buildSpeechTranslationRequest(Context context, CronetEngine cronetEngine,
                                                     byte[] requestBody,
                                                     SpeechTranslationListener translationListener) {
        Executor executor = Executors.newSingleThreadExecutor();
        String speechTranslateEndpoint = context.getString(R.string.speechToSpeechEndpoint);
        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(
                speechTranslateEndpoint, new UrlRequest.Callback(){
                    StringBuilder responseBodyBuilder;
                    @Override
                    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
                        Log.i(TAG, "onRedirectReceived method called.");
                        request.followRedirect();
                    }
                    @Override
                    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
                        Log.i(TAG, "onResponseStarted method called.");
                        int httpStatusCode = info.getHttpStatusCode();
                        if (httpStatusCode == 200 || httpStatusCode == 400) {
                            responseBodyBuilder = new StringBuilder();
                            request.read(ByteBuffer.allocateDirect((int)info.getReceivedByteCount()));
                        } else  {
                            request.cancel();
                            String errorMessage = "Unexpected HTTP status code: " + httpStatusCode;
                            translationListener.onTranslationFailed(new SpeechTranslationException(errorMessage));
                        }
                    }
                    @Override
                    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
                        Log.i(TAG, "onReadCompleted method called.");
                        byteBuffer.flip();
                        responseBodyBuilder.append(Charset.forName("UTF-8").decode(byteBuffer).toString());
                        byteBuffer.clear();
                        request.read(byteBuffer);
                    }
                    @Override
                    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
                        Log.i(TAG, "onSucceeded method called.");
                        int httpStatusCode = info.getHttpStatusCode();
                        if(httpStatusCode == 200) {
                            translationListener.onTranslationSucceeded(responseBodyBuilder.toString());
                        } else if(httpStatusCode == 400) {
                            String errorMessage = responseBodyBuilder.toString();
                            translationListener.onTranslationFailed(new SpeechTranslationException(errorMessage));
                        }
                    }
                    @Override
                    public void onFailed(UrlRequest request, UrlResponseInfo responseInfo, CronetException error) {
                        Log.e(TAG, "The request failed.", error);
                        translationListener.onTranslationFailed(error);
                    }
                } , executor)
                .setHttpMethod(SPEECH_TRANSLATE_HTTP_METHOD)
                .addHeader("Content-Type", SPEECH_TRANSLATE_CONTENT_TYPE)
                .setUploadDataProvider(UploadDataProviders.create(requestBody), executor);

        return requestBuilder.build();
    }

    public interface SpeechTranslationListener {
        void onTranslationSucceeded(String responseBody);
        void onTranslationFailed(Exception e);
    }
}
