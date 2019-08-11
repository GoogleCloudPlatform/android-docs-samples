package com.google.cloud.examples.dialogflow.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;


public class ApiRequest {

    private String token = null;
    private Date tokenExpiration = null;

    public ApiRequest() {
        // Variables needed to retrieve an auth token
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * function to call: detect Intent Sentiment Analysis | Detect Intent With TTS | KnowledgeBase
     *
     * @param context     :   context from the caller
     * @param accessToken :   access token received from fcm
     * @param expiryTime  :   expiry time received from fcm
     * @param msg         :   message sent by the user
     * @param tts         :   send message to text to speech if true
     * @param sentiment   :   send message to sentiment analysis if true
     * @param knowledge   :   send message to knowledge base if true
     * @return            :   response from the server
     */
    public String callAPI(Context context, String accessToken, Date expiryTime, String msg, boolean tts, boolean sentiment, boolean knowledge) {
        this.token = accessToken;
        this.tokenExpiration = expiryTime;
        return detectIntent(context, msg, tts, sentiment, knowledge);
    }

    /**
     * function to getting the results from the dialogflow
     *
     * @param context   :   context from the caller
     * @param msg       :   message sent by the user
     * @param tts       :   send message to text to speech if true
     * @param sentiment :   send message to sentiment analysis if true
     * @param knowledge :   send message to knowledge base if true
     * @return          :   response from the server
     */
    private String detectIntent(Context context, String msg, boolean tts, boolean sentiment, boolean knowledge) {
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            SessionsClient sessionsClient = SessionsClient.create(sessionsSettings);
            SessionName sessionName = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);

            // Set the text (hello) and language code (en-US) for the query
            TextInput textInput = TextInput.newBuilder()
                    .setText(msg)
                    .setLanguageCode("en-US")
                    .build();

            // Build the query with the TextInput
            QueryInput queryInput = QueryInput.newBuilder()
                    .setText(textInput)
                    .build();

            DetectIntentRequest detectIntentRequest = getDetectIntentRequest(sessionName, queryInput, tts, sentiment, knowledge, fixedCredentialsProvider);
            DetectIntentResponse detectIntentResponse = sessionsClient.detectIntent(detectIntentRequest);

            sessionsClient.close();

            if(tts) {
                AppController.playAudio(detectIntentResponse.getOutputAudio().toByteArray());
            }

            return handleResults(detectIntentResponse);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * function to handle the results
     * @param detectIntentResponse  :   detectIntentResponse object
     * @return  :   String response
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
    private DetectIntentRequest getDetectIntentRequest(SessionName sessionName, QueryInput queryInput, boolean tts, boolean sentiment, boolean knowledge, FixedCredentialsProvider fixedCredentialsProvider) throws Exception {
        DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder()
                .setSession(sessionName.toString())
                .setQueryInput(queryInput)
                .build();
        QueryParameters queryParameters = QueryParameters.newBuilder().build();


        if (tts) {
            OutputAudioEncoding audioEncoding = OutputAudioEncoding.OUTPUT_AUDIO_ENCODING_LINEAR_16;
            int sampleRateHertz = 16000;
            OutputAudioConfig outputAudioConfig = OutputAudioConfig.newBuilder()
                    .setAudioEncoding(audioEncoding)
                    .setSampleRateHertz(sampleRateHertz)
                    .build();

            detectIntentRequest.toBuilder()
                    .setOutputAudioConfig(outputAudioConfig)
                    .build();
        }

        if (sentiment) {
            SentimentAnalysisRequestConfig sentimentAnalysisRequestConfig = SentimentAnalysisRequestConfig.newBuilder().setAnalyzeQueryTextSentiment(true).build();

            queryParameters.toBuilder()
                    .setSentimentAnalysisRequestConfig(sentimentAnalysisRequestConfig)
                    .build();
            detectIntentRequest.toBuilder()
                    .setQueryParams(queryParameters)
                    .build();
        }


        if (knowledge) {
            KnowledgeBasesSettings knowledgeSessionsSettings = KnowledgeBasesSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            ArrayList<String> knowledgeBaseNames = KnowledgeBaseUtils.listKnowledgeBases(AppController.PROJECT_ID, knowledgeSessionsSettings);
            if (knowledgeBaseNames.size() > 0) {
                // As an example, we'll only grab the first Knowledge Base
                queryParameters.toBuilder()
                        .addKnowledgeBaseNames(knowledgeBaseNames.get(0))
                        .build();
                detectIntentRequest.toBuilder()
                        .setQueryParams(queryParameters)
                        .build();
            }
        }

        return detectIntentRequest;
    }
}


