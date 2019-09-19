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

package com.google.cloud.examples.dialogflow.utils;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2beta1.AudioEncoding;
import com.google.cloud.dialogflow.v2beta1.InputAudioConfig;
import com.google.cloud.dialogflow.v2beta1.DetectIntentRequest;
import com.google.cloud.dialogflow.v2beta1.DetectIntentResponse;
import com.google.cloud.dialogflow.v2beta1.KnowledgeAnswers;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBasesSettings;
import com.google.cloud.dialogflow.v2beta1.OutputAudioConfig;
import com.google.cloud.dialogflow.v2beta1.OutputAudioEncoding;
import com.google.cloud.dialogflow.v2beta1.QueryInput;
import com.google.cloud.dialogflow.v2beta1.QueryParameters;
import com.google.cloud.dialogflow.v2beta1.QueryResult;
import com.google.cloud.dialogflow.v2beta1.SentimentAnalysisRequestConfig;
import com.google.cloud.dialogflow.v2beta1.SessionName;
import com.google.cloud.dialogflow.v2beta1.SessionsClient;
import com.google.cloud.dialogflow.v2beta1.SessionsSettings;
import com.google.cloud.dialogflow.v2beta1.TextInput;
import com.google.cloud.examples.dialogflow.AppController;
import com.google.protobuf.ByteString;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;


public class ApiRequest {

    private String token = null;
    private Date tokenExpiration = null;

    public ApiRequest() {
        // Variables needed to retrieve an auth token
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                java.util.Locale.getDefault());
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * function to call: detect Intent Sentiment Analysis | Detect Intent With TTS | KnowledgeBase
     *
     * @param accessToken :   access token received from fcm
     * @param expiryTime  :   expiry time received from fcm
     * @param msg         :   message sent by the user
     * @param tts         :   send message to text to speech if true
     * @param sentiment   :   send message to sentiment analysis if true
     * @param knowledge   :   send message to knowledge base if true
     * @return :   response from the server
     */
    public String callAPI(String accessToken, Date expiryTime, String msg, byte[] audioBytes, boolean tts,
                          boolean sentiment, boolean knowledge) {
        this.token = accessToken;
        this.tokenExpiration = expiryTime;

        return detectIntent(msg, audioBytes, tts, sentiment, knowledge);
    }

    /**
     * function to getting the results from the dialogflow
     *
     * @param msg        :   message sent by the user
     * @param audioBytes :   audio sent by the user
     * @param tts        :   send message to text to speech if true
     * @param sentiment  :   send message to sentiment analysis if true
     * @param knowledge  :   send message to knowledge base if true
     * @return :   response from the server
     */
    private String detectIntent(String msg, byte[] audioBytes, boolean tts, boolean sentiment,
                                boolean knowledge) {
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider =
                    FixedCredentialsProvider.create(credentials);
            SessionsSettings sessionsSettings = SessionsSettings.newBuilder()
                    .setCredentialsProvider(fixedCredentialsProvider).build();
            SessionsClient sessionsClient = SessionsClient.create(sessionsSettings);
            SessionName sessionName = SessionName.of(AppController.PROJECT_ID,
                    AppController.SESSION_ID);

            QueryInput queryInput;
            if (msg != null) {
                // Set the text (hello) and language code (en-US) for the query
                TextInput textInput = TextInput.newBuilder()
                        .setText(msg)
                        .setLanguageCode("en-US")
                        .build();

                // Build the query with the TextInput
                queryInput = QueryInput.newBuilder().setText(textInput).build();
            } else {
                // Instructs the speech recognizer how to process the audio content.
                InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_AMR)
                        .setLanguageCode("en-US")
                        .setSampleRateHertz(8000)
                        .build();

                // Build the query with the TextInput
                queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();
            }

            DetectIntentRequest detectIntentRequest = getDetectIntentRequest(sessionName,
                    queryInput, tts, sentiment, knowledge, fixedCredentialsProvider, audioBytes);

            DetectIntentResponse detectIntentResponse =
                    sessionsClient.detectIntent(detectIntentRequest);

            sessionsClient.close();

            if (tts) {
                AppController.playAudio(detectIntentResponse.getOutputAudio().toByteArray());
            }

            if (msg != null) {
                return handleResults(detectIntentResponse);
            } else {
                return String.format(
                        "%s|%s",
                        handleResults(detectIntentResponse),
                        detectIntentResponse.getQueryResult().getQueryText());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ex.getMessage();
        }
    }

    /**
     * function to get the DetectIntentRequest object
     *
     * @param sessionName              : sessionName object
     * @param queryInput               : queryInput object
     * @param tts                      : if text to speech is true
     * @param sentiment                : if sentiment analysis is true
     * @param knowledge                : if knowledge base is true
     * @param fixedCredentialsProvider : fixedCredentialsProvider for knowledgebase
     * @return : DetectIntentRequest object
     */
    private DetectIntentRequest getDetectIntentRequest(SessionName sessionName,
            QueryInput queryInput, boolean tts, boolean sentiment, boolean knowledge,
            FixedCredentialsProvider fixedCredentialsProvider, byte[] audioBytes) throws Exception {

        DetectIntentRequest.Builder detectIntentRequestBuilder = DetectIntentRequest.newBuilder()
                .setSession(sessionName.toString())
                .setQueryInput(queryInput);

        if (audioBytes != null) {
            detectIntentRequestBuilder.setInputAudio(ByteString.copyFrom(audioBytes));
        }

        QueryParameters.Builder queryParametersBuilder = QueryParameters.newBuilder();

        if (tts) {
            OutputAudioEncoding audioEncoding = OutputAudioEncoding.OUTPUT_AUDIO_ENCODING_MP3;
            int sampleRateHertz = 16000;
            OutputAudioConfig outputAudioConfig = OutputAudioConfig.newBuilder()
                    .setAudioEncoding(audioEncoding)
                    .setSampleRateHertz(sampleRateHertz)
                    .build();

            detectIntentRequestBuilder.setOutputAudioConfig(outputAudioConfig);
        }

        if (sentiment) {
            SentimentAnalysisRequestConfig sentimentAnalysisRequestConfig =
                    SentimentAnalysisRequestConfig.newBuilder()
                            .setAnalyzeQueryTextSentiment(true).build();

            queryParametersBuilder
                    .setSentimentAnalysisRequestConfig(sentimentAnalysisRequestConfig);
        }


        if (knowledge) {
            KnowledgeBasesSettings knowledgeSessionsSettings = KnowledgeBasesSettings.newBuilder()
                    .setCredentialsProvider(fixedCredentialsProvider).build();
            ArrayList<String> knowledgeBaseNames =
                    KnowledgeBaseUtils.listKnowledgeBases(
                            AppController.PROJECT_ID, knowledgeSessionsSettings);

            if (knowledgeBaseNames.size() > 0) {
                // As an example, we'll only grab the first Knowledge Base
                queryParametersBuilder.addKnowledgeBaseNames(knowledgeBaseNames.get(0));
            }
        }

        QueryParameters queryParameters = queryParametersBuilder.build();
        detectIntentRequestBuilder.setQueryParams(queryParameters);

        return detectIntentRequestBuilder.build();
    }

    /**
     * function to handle the results
     *
     * @param detectIntentResponse :   detectIntentResponse object
     * @return :   String response
     */
    private String handleResults(DetectIntentResponse detectIntentResponse) {
        QueryResult queryResult = detectIntentResponse.getQueryResult();
        StringBuilder response = new StringBuilder();

        KnowledgeAnswers knowledgeAnswers = queryResult.getKnowledgeAnswers();
        for (KnowledgeAnswers.Answer answer : knowledgeAnswers.getAnswersList()) {
            response.append(answer.getAnswer()).append("\n");
        }

        response.append(queryResult.getFulfillmentText());

        if (queryResult.hasSentimentAnalysisResult()) {
            String magnitude = String.format("(Magnitude: %s; ",
                    queryResult.getSentimentAnalysisResult().getQueryTextSentiment().getMagnitude());
            response.append(magnitude);

            String score = String.format("score: %s)",
                    queryResult.getSentimentAnalysisResult().getQueryTextSentiment().getScore());
            response.append(score);
        }

        return response.toString();
    }
}


