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

import android.accounts.AccountManager
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors

/**
 * Singleton class that makes authenticated requests to a Google Cloud Storage bucket to get the
 * files that contain translated speech messages.
 */
object GcsDownloadHelper {
    private const val TAG = "GcsDownloadHelper"
    private const val GCS_DOWNLOAD_HTTP_METHOD = "GET"

    /**
     * Performs an authenticated request to a Google Cloud Storage bucket to download the object in
     * the specified path. Returns a pointer to the downloaded file in the local storage.
     * @param context The application context.
     * @param gcsFile The file in the Google Cloud Storage bucket.
     * @param downloadListener The callback to deliver the results to.
     */
    fun downloadGcsFile(context: Context, cronetEngine: CronetEngine, gcsFile: String,
                        downloadListener: GcsDownloadListener) {
        getGcsAccessToken(context, object : GcsTokenListener {
            override fun onAccessTokenRequestSucceeded(token: String) {
                val request = buildGcsRequest(context, cronetEngine, gcsFile,
                        token, downloadListener)
                request.start()
            }

            override fun onAccessTokenRequestFailed(e: Exception) {
                downloadListener.onDownloadFailed(e)
            }
        })
    }

    private fun getGcsAccessToken(context: Context, tokenListener: GcsTokenListener) {
        val runnable = {
            try {
                val currentAccount = AccountManager.get(context).accounts[0]
                val scope = "oauth2:" + context.getString(R.string.speechToSpeechOAuth2Scope)
                val token = GoogleAuthUtil.getToken(
                        context, currentAccount, scope, Bundle())
                tokenListener.onAccessTokenRequestSucceeded(token)
            } catch (e: GoogleAuthException) {
                Log.e(TAG, e.localizedMessage)
                tokenListener.onAccessTokenRequestFailed(e)
            } catch (e: IOException) {
                Log.e(TAG, e.localizedMessage)
                tokenListener.onAccessTokenRequestFailed(e)
            }
        }
        AsyncTask.execute(runnable)
    }

    private fun buildGcsRequest(context: Context, cronetEngine: CronetEngine, gcsFile: String,
                                accessToken: String, downloadListener: GcsDownloadListener): UrlRequest {
        val executor = Executors.newSingleThreadExecutor()

        val gcsPathEndpoint = context.getString(R.string.gcsBaseEndpoint) + gcsFile

        val requestBuilder = cronetEngine.newUrlRequestBuilder(
                gcsPathEndpoint, object : UrlRequest.Callback() {
            var localPath: String = context.filesDir.toString() + "/" + gcsFile.substringAfterLast('/')
            var downloadedFile: File = File(localPath)
            var outputChannel: FileChannel = FileOutputStream(localPath).channel
            override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                Log.i(TAG, "onRedirectReceived method called.")
                request.followRedirect()
            }

            override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                Log.i(TAG, "onResponseStarted method called.")
                val httpStatusCode = info.httpStatusCode
                when (httpStatusCode) {
                    200 -> {
                        try {
                            // Check if parent folders exists
                            val languageFolder = downloadedFile.parentFile
                            if (!languageFolder.exists()) {
                                if (!languageFolder.mkdirs()) {
                                    val message = "Failed to create directory " + languageFolder.absolutePath
                                    downloadListener.onDownloadFailed(IOException(message))
                                }
                            }
                            request.read(ByteBuffer.allocateDirect(info.receivedByteCount.toInt()))
                        } catch (e: IOException) {
                            downloadListener.onDownloadFailed(e)
                        }

                    }
                    403 -> {
                        val errorMessage = "HTTP status code: " + httpStatusCode +
                                ". Does the user have read access to the GCS bucket?"
                        downloadListener.onDownloadFailed(SpeechTranslationException(errorMessage))
                    }
                    else -> {
                        val errorMessage = "Unexpected HTTP status code: $httpStatusCode"
                        downloadListener.onDownloadFailed(SpeechTranslationException(errorMessage))
                    }
                }
            }

            override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
                Log.i(TAG, "onReadCompleted method called.")
                request.read(ByteBuffer.allocateDirect(info.receivedByteCount.toInt()))
                byteBuffer.flip()
                try {
                    outputChannel.write(byteBuffer)
                } catch (e: IOException) {
                    downloadListener.onDownloadFailed(e)
                }

            }

            override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                Log.i(TAG, "onSucceeded method called.")
                val httpStatusCode = info.httpStatusCode
                if (httpStatusCode == 200) {
                    try {
                        outputChannel.close()
                        val downloadedFile = File(localPath)
                        downloadListener.onDownloadSucceeded(downloadedFile)
                    } catch (e: IOException) {
                        downloadListener.onDownloadFailed(e)
                    }

                }
            }

            override fun onFailed(request: UrlRequest, responseInfo: UrlResponseInfo, error: CronetException) {
                Log.e(TAG, "The request failed.", error)
                downloadListener.onDownloadFailed(error)
            }
        }, executor)
                .setHttpMethod(GCS_DOWNLOAD_HTTP_METHOD)
                .addHeader("Authorization", "Bearer $accessToken")

        return requestBuilder.build()
    }

    interface GcsDownloadListener {
        fun onDownloadSucceeded(file: File)
        fun onDownloadFailed(e: Exception)
    }

    private interface GcsTokenListener {
        fun onAccessTokenRequestSucceeded(token: String)
        fun onAccessTokenRequestFailed(e: Exception)
    }
}
