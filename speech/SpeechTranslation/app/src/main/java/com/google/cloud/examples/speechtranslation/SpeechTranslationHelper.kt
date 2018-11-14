/*
 * Copyright 2018 Google LLC.
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

package com.google.cloud.examples.speechtranslation

import android.content.Context
import android.util.Log
import org.chromium.net.*
import org.json.JSONException
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.Executors

/**
 * Singleton class that makes requests to a Google Cloud Function that translates speech messages.
 * For more information about the function implementation, see
 * https://github.com/GoogleCloudPlatform/nodejs-docs-samples/tree/master/functions/speech-to-speech
 */
object SpeechTranslationHelper {
    private const val TAG = "SpeechTranslationHelper"
    private const val SPEECH_TRANSLATE_HTTP_METHOD = "POST"
    private const val SPEECH_TRANSLATE_CONTENT_TYPE = "application/json"
    private const val SPEECH_TRANSLATE_ENCODING = "LINEAR16"

    /**
     * Performs a request to a Google Cloud Function that translates speech messages. Returns a JSON
     * string with information about the response. The response includes information about the audio
     * files that the client can download at a different time.
     * @param context The application context
     * @param base64EncodedAudioMessage The base64-encoded audio message
     * @param sampleRateInHertz The sample rate in hertz
     * @param translationListener The callback to deliver the results to.
     */
    fun translateAudioMessage(context: Context, cronetEngine: CronetEngine,
                              base64EncodedAudioMessage: String, sampleRateInHertz: Int,
                              translationListener: SpeechTranslationListener) {
        val requestBody = JSONObject()
        try {
            requestBody.put("encoding", SPEECH_TRANSLATE_ENCODING)
            requestBody.put("sampleRateHertz", sampleRateInHertz)
            requestBody.put("languageCode", context.resources.configuration.locales.get(0))
            requestBody.put("audioContent", base64EncodedAudioMessage)
        } catch (e: JSONException) {
            Log.e(TAG, e.localizedMessage)
            translationListener.onTranslationFailed(e)
        }

        val requestBodyBytes = requestBody.toString().toByteArray()
        val request = buildSpeechTranslationRequest(context, cronetEngine, requestBodyBytes, translationListener)
        request.start()
    }

    private fun buildSpeechTranslationRequest(context: Context, cronetEngine: CronetEngine,
                                              requestBody: ByteArray,
                                              translationListener: SpeechTranslationListener): UrlRequest {
        val executor = Executors.newSingleThreadExecutor()
        val speechTranslateEndpoint = context.getString(R.string.speechToSpeechEndpoint)
        val requestBuilder = cronetEngine.newUrlRequestBuilder(
                speechTranslateEndpoint, object : UrlRequest.Callback() {
            var responseBodyBuilder: StringBuilder = StringBuilder()
            override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                Log.i(TAG, "onRedirectReceived method called.")
                request.followRedirect()
            }

            override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                Log.i(TAG, "onResponseStarted method called.")
                val httpStatusCode = info.httpStatusCode
                if (httpStatusCode == 200 || httpStatusCode == 400) {
                    request.read(ByteBuffer.allocateDirect(info.receivedByteCount.toInt()))
                } else {
                    request.cancel()
                    val errorMessage = "Unexpected HTTP status code: $httpStatusCode"
                    translationListener.onTranslationFailed(SpeechTranslationException(errorMessage))
                }
            }

            override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
                Log.i(TAG, "onReadCompleted method called.")
                byteBuffer.flip()
                responseBodyBuilder.append(Charset.forName("UTF-8").decode(byteBuffer).toString())
                byteBuffer.clear()
                request.read(byteBuffer)
            }

            override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                Log.i(TAG, "onSucceeded method called.")
                val httpStatusCode = info.httpStatusCode
                if (httpStatusCode == 200) {
                    translationListener.onTranslationSucceeded(responseBodyBuilder.toString())
                } else if (httpStatusCode == 400) {
                    val errorMessage = responseBodyBuilder.toString()
                    translationListener.onTranslationFailed(SpeechTranslationException(errorMessage))
                }
            }

            override fun onFailed(request: UrlRequest, responseInfo: UrlResponseInfo, error: CronetException) {
                Log.e(TAG, "The request failed.", error)
                translationListener.onTranslationFailed(error)
            }
        }, executor)
                .setHttpMethod(SPEECH_TRANSLATE_HTTP_METHOD)
                .addHeader("Content-Type", SPEECH_TRANSLATE_CONTENT_TYPE)
                .setUploadDataProvider(UploadDataProviders.create(requestBody), executor)

        return requestBuilder.build()
    }

    interface SpeechTranslationListener {
        fun onTranslationSucceeded(responseBody: String)
        fun onTranslationFailed(e: Exception)
    }
}