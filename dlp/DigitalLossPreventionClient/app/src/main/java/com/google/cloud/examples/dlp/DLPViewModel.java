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

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A view model for the MainActivity.
 */
public class DLPViewModel extends BaseObservable {

    private static final String TAG = "DLP";

    public interface CameraProvider {
        void takePhoto();
        void showPhotoFullscreen(@NonNull File photo);
    }

    private String inputText;
    private String inspectTextResult;
    private boolean loading = false;
    private boolean cameraEnabled = false;
    private boolean resultAvailable = false;

    private final CameraProvider cameraProvider;
    private final DLPClient client;

    /**
     * Create a new view model.
     *
     * @param cameraProvider the camera provider
     * @param client the service client
     */
    public DLPViewModel(@NonNull CameraProvider cameraProvider,
                        @NonNull DLPClient client) {
        this.cameraProvider = cameraProvider;
        this.client = client;
    }

    /** onClick handler for the inspect text button */
    public void onInspectText() {
        if (!Strings.isNullOrEmpty(inputText)) {
            Log.d(TAG, "Inspecting text...");

            reset(true);

            // send text for analysis
            new InspectTextTask(client, inputText, (r) -> {
                setLoading(false);
                setInspectTextResult(r);
            }).execute();
        }
    }

    /** onClick handler for the take photo button */
    public void onInspectPhoto() {
        reset(true);

        // open camera
        cameraProvider.takePhoto();
    }

    /** Inspect the given photo and update the UI */
    public void inspectPhoto(File photo) {
        if (photo != null) {
            Log.d(TAG, "Inspecting photo...");

            reset(true);

            // send the photo for analysis
            new InspectPhotoTask(client, photo, (file) -> {
                setLoading(false);
                cameraProvider.showPhotoFullscreen(file);
            }).execute();
        }
    }

    /** Reset / forget all results */
    private void reset(boolean loading) {
        setInspectTextResult(null);
        setLoading(loading);
    }

    @Bindable
    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
        notifyPropertyChanged(BR.inputText);
    }

    @Bindable
    public String getInspectTextResult() {
        return inspectTextResult;
    }

    public void setInspectTextResult(String inspectTextResult) {
        this.inspectTextResult = inspectTextResult;
        notifyPropertyChanged(BR.inspectTextResult);
        notifyPropertyChanged(BR.resultAvailable);
    }

    @Bindable
    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
        notifyPropertyChanged(BR.loading);
        notifyPropertyChanged(BR.resultAvailable);
    }

    @Bindable
    public boolean isResultAvailable() {
        return !this.isLoading() && this.getInspectTextResult() != null;
    }

    @Bindable
    public boolean isCameraEnabled() {
        return cameraEnabled;
    }

    public void setCameraEnabled(boolean cameraEnabled) {
        this.cameraEnabled = cameraEnabled;
        notifyPropertyChanged(BR.cameraEnabled);
    }

    /** Result handler for ClientTask */
    public interface DLPResult<T> {
        void onResult(T result);
    }

    /** Helper class for calling the DLPClient methods in the background */
    private abstract static class ClientTask<T> extends AsyncTask<Void, Void, T> {
        private final DLPResult<T> handler;
        final DLPClient client;

        ClientTask(DLPClient client, DLPResult<T> handler) {
            this.client = client;
            this.handler = handler;
        }

        @Override
        protected void onPostExecute(T t) {
            handler.onResult(t);
        }
    }

    /** Background task for inspecting text */
    private static class InspectTextTask extends ClientTask<String> {
        private final String text;

        InspectTextTask(DLPClient client, String text, DLPResult<String> handler) {
            super(client, handler);
            this.text = text;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return client.inspectString(text);
        }
    }

    /** Background task for inspecting images */
    private static class InspectPhotoTask extends ClientTask<File> {
        private final File photo;

        InspectPhotoTask(DLPClient client, File photo, DLPResult<File> handler) {
            super(client, handler);
            this.photo = photo;
        }

        @Override
        protected File doInBackground(Void... voids) {
            // decode the image
            // (optionally, scale it down if needed)
            BitmapFactory.Options options = new BitmapFactory.Options();
            //options.inSampleSize = 2;
            Bitmap bitmap = BitmapFactory.decodeFile(photo.getAbsolutePath(), options);

            // convert to PNG and send image to the API
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, data);
            ByteString redacted = client.redactPhoto(ByteString.copyFrom(data.toByteArray()));

            // create a new image from the response and overwrite the original file
            try (ByteArrayInputStream is = new ByteArrayInputStream(redacted.toByteArray());
                 FileOutputStream outputStream = new FileOutputStream(photo)) {
                    BitmapFactory.decodeStream(is)
                            .compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to decode image", e);
            }
            return photo;
        }
    }
}
