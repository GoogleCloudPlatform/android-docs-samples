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

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Abstract class that provides a method to base64 encode a file. The app encodes speech data to
 * send it to a Google Cloud Function that translates the speech to other languages.
 */
object PermissionsHelper {
    private const val TAG = "PermissionsHelper"
    private const val REQUIRED_PERMISSIONS_REQUEST_CODE = 15624

    fun hasRequiredPermissions(context: Context): Boolean {
        val recordAudioPermissionCheck = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO)
        val writeExternalStoragePermissionCheck = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val hasRecordAudioPermission = recordAudioPermissionCheck == PackageManager.PERMISSION_GRANTED
        val hasWriteExternalPermission = writeExternalStoragePermissionCheck == PackageManager.PERMISSION_GRANTED
        Log.i(TAG, "Has record audio permissions: $hasRecordAudioPermission")
        Log.i(TAG, "Has record audio permissions: $hasWriteExternalPermission")
        return hasRecordAudioPermission && hasWriteExternalPermission
    }

    fun requestRequiredPermissions(activity: Activity) {
        activity.requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO),
                REQUIRED_PERMISSIONS_REQUEST_CODE
        )
    }
}
