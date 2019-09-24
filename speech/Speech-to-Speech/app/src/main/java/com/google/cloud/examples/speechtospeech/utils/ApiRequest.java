/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.examples.speechtospeech.utils;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.google.cloud.examples.speechtospeech.AppController;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechSettings;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionResult;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse;
import com.google.cloud.texttospeech.v1beta1.AudioConfig;
import com.google.cloud.texttospeech.v1beta1.AudioEncoding;
import com.google.cloud.texttospeech.v1beta1.ListVoicesResponse;
import com.google.cloud.texttospeech.v1beta1.SynthesisInput;
import com.google.cloud.texttospeech.v1beta1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1beta1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1beta1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1beta1.Voice;
import com.google.cloud.texttospeech.v1beta1.VoiceSelectionParams;
import com.google.cloud.translate.Language;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.v3beta1.LocationName;
import com.google.cloud.translate.v3beta1.TranslateTextRequest;
import com.google.cloud.translate.v3beta1.TranslateTextResponse;
import com.google.cloud.translate.v3beta1.TranslationServiceClient;
import com.google.cloud.translate.v3beta1.TranslationServiceSettings;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ApiRequest {

    private String token = null;
    private Date tokenExpiration = null;

    public ApiRequest() {
        // Variables needed to retrieve an auth token
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static List<Voice> listVoices(String token, Date tokenExpiration, String language) {

        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
            TextToSpeechSettings textToSpeechSettings = TextToSpeechSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(textToSpeechSettings)) {

                ListVoicesResponse listVoicesResponse = textToSpeechClient.listVoices(language);
                List<Voice> voices = listVoicesResponse.getVoicesList();

                for (int i = 0; i < voices.size(); i++) {
                    Log.i("voices", voices.get(i).getName());
                }

                return voices;


            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public List<Language> getSupportedLanguages(String token, Date tokenExpiration, String sourceLanguageCode) {

        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
            Translate translate = TranslateOptions.newBuilder().setProjectId(AppController.PROJECT_ID).setCredentials(fixedCredentialsProvider.getCredentials()).build().getService();
            List<Language> languages;

            if (TextUtils.isEmpty(sourceLanguageCode)) {
                languages = translate.listSupportedLanguages();
            } else {
                languages = translate.listSupportedLanguages(Translate.LanguageListOption.targetLanguage(sourceLanguageCode));
            }

            for (Language language : languages) {
                System.out.printf("Name: %s, Code: %s\n", language.getName(), language.getCode());
            }

            return languages;

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * function to call: detect Intent Sentiment Analysis | Detect Intent With TTS | KnowledgeBase
     *
     * @param accessToken :   access token received from fcm
     * @param expiryTime  :   expiry time received from fcm
     * @return :   response from the server
     */
    public String callAPI(String accessToken, Date expiryTime, String text, String fileName, String sourceLanguageCode, String targetLanguageCode, int ssmlGenderValue) {
        this.token = accessToken;
        this.tokenExpiration = expiryTime;

        String response = "";
        if (!TextUtils.isEmpty(fileName)) {
            String convertedText = speechToText(accessToken, expiryTime, fileName, sourceLanguageCode);
            addMsg(convertedText);
            response = translateText(accessToken, expiryTime, convertedText, sourceLanguageCode, targetLanguageCode);
        } else if (!TextUtils.isEmpty(text)) {
            response = translateText(accessToken, expiryTime, text, sourceLanguageCode, targetLanguageCode);
        }

        textToSpeech(response, ssmlGenderValue, targetLanguageCode);
        return response;
    }

    private void addMsg(String message) {
        Intent intent = new Intent(AppController.TOKEN_RECEIVED);
        intent.putExtra("type", "addMsg");
        intent.putExtra("message", message);
        intent.putExtra("messageType", 1);
        AppController.context.sendBroadcast(intent);
    }

    /**
     * function to getting the results from the dialogflow
     *
     * @return :   response from the server
     */
    private String translateText(String token, Date tokenExpiration, String text, String sourceLanguageCode, String targetLanguageCode) {
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
            TranslationServiceSettings translationServiceSettings = TranslationServiceSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            try (TranslationServiceClient translationServiceClient = TranslationServiceClient.create(translationServiceSettings)) {

                LocationName locationName =
                        LocationName.newBuilder().setProject(AppController.PROJECT_ID).setLocation("global").build();

                TranslateTextRequest translateTextRequest =
                        TranslateTextRequest.newBuilder()
                                .setParent(locationName.toString())
                                .setMimeType("text/plain")
                                .setSourceLanguageCode(sourceLanguageCode)
                                .setTargetLanguageCode(targetLanguageCode)
                                .addContents(text)
                                .build();

                // Call the API
                TranslateTextResponse response = translationServiceClient.translateText(translateTextRequest);
                System.out.format(
                        "Translated Text: %s", response.getTranslationsList().get(0).getTranslatedText());
                return response.getTranslationsList().get(0).getTranslatedText();

            } catch (Exception e) {
                throw new RuntimeException("Couldn't create client.", e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ex.getMessage();
        }
    }

    private void textToSpeech(String text, int ssmlGenderValue, String languageCode) {

        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
            TextToSpeechSettings textToSpeechSettings = TextToSpeechSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(textToSpeechSettings)) {
                // Set the text input to be synthesized
                SynthesisInput input = SynthesisInput.newBuilder()
                        .setText(text)
                        .build();

                // Build the voice request, select the language code ("en-US") and the ssml voice gender
                // ("neutral")
                VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                        .setLanguageCode(languageCode)
                        .setSsmlGenderValue(ssmlGenderValue)
                        .build();

                // Select the type of audio file you want returned
                AudioConfig audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.MP3)
                        .build();

                // Perform the text-to-speech request on the text input with the selected voice parameters and
                // audio file type
                SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice,
                        audioConfig);

                // Get the audio contents from the response
                ByteString audioContents = response.getAudioContent();
                AppController.playAudio(audioContents.toByteArray());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String speechToText(String token, Date tokenExpiration, String fileName, String languageCode) {
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
            SpeechSettings speechSettings =
                    SpeechSettings.newBuilder()
                            .setCredentialsProvider(fixedCredentialsProvider)
                            .build();
            try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {

                // Reads the audio file into memory
                Path path = Paths.get(fileName);
                byte[] data = Files.readAllBytes(path);
                ByteString audioBytes = ByteString.copyFrom(data);

                // Builds the sync recognize request
                RecognitionConfig recConfig = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.MP3)
                        .setSampleRateHertz(16000)
                        .setLanguageCode(languageCode)
                        .setMaxAlternatives(30)
                        .setModel("default")
                        .build();

                StreamingRecognitionConfig config =
                        StreamingRecognitionConfig.newBuilder().setConfig(recConfig).build();

                class ResponseApiStreamingObserver<T> implements ApiStreamObserver<T> {
                    private final SettableFuture<List<T>> future = SettableFuture.create();
                    private final List<T> messages = new java.util.ArrayList<T>();

                    @Override
                    public void onNext(T message) {
                        messages.add(message);
                    }

                    @Override
                    public void onError(Throwable t) {
                        future.setException(t);
                    }

                    @Override
                    public void onCompleted() {
                        future.set(messages);
                    }

                    // Returns the SettableFuture object to get received messages / exceptions.
                    public SettableFuture<List<T>> future() {
                        return future;
                    }
                }

                ResponseApiStreamingObserver<StreamingRecognizeResponse> responseObserver =
                        new ResponseApiStreamingObserver<>();

                BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable =
                        speechClient.streamingRecognizeCallable();

                ApiStreamObserver<StreamingRecognizeRequest> requestObserver =
                        callable.bidiStreamingCall(responseObserver);

                // The first request must **only** contain the audio configuration:
                requestObserver.onNext(
                        StreamingRecognizeRequest.newBuilder().setStreamingConfig(config).build());

                // Subsequent requests must **only** contain the audio data.
                requestObserver.onNext(
                        StreamingRecognizeRequest.newBuilder()
                                .setAudioContent(ByteString.copyFrom(data))
                                .build());

                // Mark transmission as completed after sending the data.
                requestObserver.onCompleted();

                List<StreamingRecognizeResponse> responses = responseObserver.future().get();
                String responseText = "";
                for (StreamingRecognizeResponse response : responses) {
                    // For streaming recognize, the results list has one is_final result (if available) followed
                    // by a number of in-progress results (if iterim_results is true) for subsequent utterances.
                    // Just print the first result here.
                    StreamingRecognitionResult result = response.getResultsList().get(0);
                    // There can be several alternative transcripts for a given chunk of speech. Just use the
                    // first (most likely) one here.
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    responseText += alternative.getTranscript();
                }

                return responseText;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "";
    }
}


