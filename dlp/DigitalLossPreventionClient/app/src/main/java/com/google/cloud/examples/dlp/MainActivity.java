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

package com.google.cloud.examples.dlp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.cloud.examples.dlp.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Main Activity for the DLP example.
 *
 * This app shows how to inspect image and text content to identify sensitive information
 * such as names, phone numbers, and credit card numbers.
 */
public class MainActivity extends AppCompatActivity implements DLPViewModel.CameraProvider {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int CAMERA_REQUEST_CODE = 10;
    private static final String FILE_PROVIDER = "com.google.cloud.dlp.fileprovider";

    private DLPViewModel mViewModel;
    private DLPClient mClient;
    private File mPhotoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        // get project name from the JSON key file
        // Note: In a real app this would often be a constant string. We do this here
        //       since we're embedding the credentials file so you don't have to change any
        //       code when you use your own key file to run this example.
        String projectId;
        try (InputStreamReader reader = new InputStreamReader(
                getApplicationContext().getResources().openRawResource(R.raw.credential))) {
            projectId = new Gson().fromJson(reader, KeyFileContent.class).project_id;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read service account key file", e);
        }

        // create client
        try {
            // NOTE: The line below uses an embedded credential (res/raw/credential.json).
            //       You should not package a credential with real application.
            //       Instead, you should get a credential securely from a server.
            mClient = new DLPClient(GoogleCredentials.fromStream(
                    getApplicationContext().getResources().openRawResource(R.raw.credential)), projectId);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create DLP client", e);
        }

        // bind to view model
        mViewModel = new DLPViewModel(this, mClient);
        binding.setViewModel(mViewModel);

        // check permissions and ensure camera is available
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mViewModel.setCameraEnabled(false);
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            mViewModel.setCameraEnabled(true);
        }

        // create a temp file to save the photo
        try {
            mPhotoFile = File.createTempFile("JPEG_dlp_example_", ".jpg", getCacheDir());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create temporary file for photo", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // shutdown client
        mClient.shutdown();
    }

    @Override
    public void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uri = FileProvider.getUriForFile(this, FILE_PROVIDER, mPhotoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, MainActivity.REQUEST_IMAGE_CAPTURE);
        } else {
            throw new IllegalStateException("Unable to capture photo");
        }
    }

    @Override
    public void showPhotoFullscreen(@NonNull File photo) {
        Intent intent = new Intent(this, ShowPhotoActivity.class);
        Uri uri = FileProvider.getUriForFile(this, FILE_PROVIDER, photo);
        intent.putExtra(ShowPhotoActivity.EXTRA_PHOTO, uri);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @NonNull Intent imageIntent) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // the full size photo data is written to the file, so we just pass it along
            mViewModel.inspectPhoto(mPhotoFile);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mViewModel.setCameraEnabled(true);
        }
    }

    // helper class for reading keyfile contents
    private static class KeyFileContent {
        public String project_id;
    }
}
