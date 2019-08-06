package com.google.cloud.examples.dialogflow.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2beta1.DetectIntentRequest;
import com.google.cloud.dialogflow.v2beta1.DetectIntentResponse;
import com.google.cloud.dialogflow.v2beta1.KnowledgeAnswers;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBaseName;
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
import com.google.cloud.examples.dialogflow.ui.ChatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;


public class ApiRequest {

    private String token = null;
    private Date tokenExpiration = null;
    private String response = "";

    public ApiRequest() {
        // Variables needed to retrieve an auth token
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * function to call: detect Intent Sentiment Analysis | Detect Intent With TTS | KnowledgeBase
     *
     * @param context     :   context
     * @param accessToken :   access token received from fcm
     * @param expiryTime  :   expiry time received from fcm
     * @param msg         :   message sent by the user
     * @param tts         :   send message to text to speech if true
     * @param sentiment   :   send message to sentiment analysis if true
     * @param knowledge   :   send message to knowledge base if true
     */
    public void callAPI(Context context, String accessToken, Date expiryTime, String msg, boolean tts, boolean sentiment, boolean knowledge) {
        Toast.makeText(context, "Calling the API", Toast.LENGTH_SHORT).show();
        response = "";
        this.token = accessToken;
        this.tokenExpiration = expiryTime;
        getResults(msg, tts, sentiment, knowledge);
    }

    private void sendMsgToScreen(String response, boolean voiceFeedback) {
        ChatActivity.addMsg(response, 0, voiceFeedback);
    }

    /**
     * function to getting the results from the dialogflow
     *
     * @param msg       :   message sent by the user
     * @param tts       :   send message to text to speech if true
     * @param sentiment :   send message to sentiment analysis if true
     * @param knowledge :   send message to knowledge base if true
     */
    private void getResults(String msg, boolean tts, boolean sentiment, boolean knowledge) {
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            SessionsClient sessionsClient = SessionsClient.create(sessionsSettings);
            SessionName sessionName = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);
            System.out.println("Session Path: " + sessionName.toString());

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
            if (detectIntentRequest != null) {
                DetectIntentResponse detectIntentResponse = sessionsClient.detectIntent(detectIntentRequest);
                // Display the query result
                QueryResult queryResult = detectIntentResponse.getQueryResult();
                StringBuilder knowledgebaseResponse = new StringBuilder();
                String response;

                if (knowledge) {
                    KnowledgeAnswers knowledgeAnswers = queryResult.getKnowledgeAnswers();
                    for (KnowledgeAnswers.Answer answer : knowledgeAnswers.getAnswersList()) {
                        knowledgebaseResponse.append(answer.getAnswer()).append("\n");
                    }
                }

                if (sentiment) {
                    response = queryResult.getFulfillmentText() + " (Magnitude " + queryResult.getSentimentAnalysisResult().getQueryTextSentiment().getMagnitude() + "" + "; score: " + queryResult.getSentimentAnalysisResult().getQueryTextSentiment().getScore() + ")";
                } else {
                    response = queryResult.getFulfillmentText();
                }

                prepareResult(response, knowledgebaseResponse.toString(), tts, sentiment, knowledge);
            } else {
                Log.i("ApiRequest", "DetectIntentRequest is null");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * function to prepare the dialogflow response
     *
     * @param response              : response from tts|sentiment
     * @param knowledgebaseResponse : response from knowledge base
     * @param tts                   : if text to speech is true
     * @param sentiment             : if sentiment analysis is true
     * @param knowledge             : if knowledge base is true
     */
    private void prepareResult(String response, String knowledgebaseResponse, boolean tts, boolean sentiment, boolean knowledge) {
        if (TextUtils.isEmpty(knowledgebaseResponse) && TextUtils.isEmpty(response)) {
            sendMsgToScreen("No Response", tts);
        } else if (!TextUtils.isEmpty(knowledgebaseResponse)) {
            sendMsgToScreen(knowledgebaseResponse, tts);
        } else if (tts || sentiment) {
            sendMsgToScreen(response, tts);
        } else if (!knowledge) {
            sendMsgToScreen(response, tts);
        } else {
            sendMsgToScreen("No Response", tts);
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
    private DetectIntentRequest getDetectIntentRequest(SessionName sessionName, QueryInput queryInput, boolean tts, boolean sentiment, boolean knowledge, FixedCredentialsProvider fixedCredentialsProvider) {
        try {
            OutputAudioEncoding audioEncoding = OutputAudioEncoding.OUTPUT_AUDIO_ENCODING_LINEAR_16;
            int sampleRateHertz = 16000;
            OutputAudioConfig outputAudioConfig = OutputAudioConfig.newBuilder()
                    .setAudioEncoding(audioEncoding)
                    .setSampleRateHertz(sampleRateHertz)
                    .build();

            SentimentAnalysisRequestConfig sentimentAnalysisRequestConfig = SentimentAnalysisRequestConfig.newBuilder().setAnalyzeQueryTextSentiment(true).build();

            DetectIntentRequest detectIntentRequest;
            KnowledgeBaseName knowledgeBaseName = null;

            if (knowledge) {
                KnowledgeBasesSettings knowledgeSessionsSettings = KnowledgeBasesSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
                ArrayList<String> knowledgeBaseNames = KnowledgebaseUtils.listKnowledgeBases(AppController.PROJECT_ID, knowledgeSessionsSettings);
                if (knowledgeBaseNames.size() > 0) {
                    String knowledgebaseId = knowledgeBaseNames.get(0);
                    knowledgebaseId = knowledgebaseId.substring(knowledgebaseId.lastIndexOf("/") + 1);
                    knowledgeBaseName = KnowledgeBaseName.of(AppController.PROJECT_ID, knowledgebaseId);
                }
            }

            if (tts && sentiment && knowledge && knowledgeBaseName != null) {
                QueryParameters queryParameters = QueryParameters.newBuilder()
                        .addKnowledgeBaseNames(knowledgeBaseName.toString())
                        .setSentimentAnalysisRequestConfig(sentimentAnalysisRequestConfig)
                        .build();
                detectIntentRequest = DetectIntentRequest.newBuilder()
                        .setSession(sessionName.toString())
                        .setQueryInput(queryInput)
                        .setQueryParams(queryParameters)
                        .setOutputAudioConfig(outputAudioConfig)
                        .build();
            } else if (tts && sentiment) {
                QueryParameters queryParameters = QueryParameters.newBuilder()
                        .setSentimentAnalysisRequestConfig(sentimentAnalysisRequestConfig)
                        .build();
                detectIntentRequest = DetectIntentRequest.newBuilder()
                        .setSession(sessionName.toString())
                        .setQueryInput(queryInput)
                        .setQueryParams(queryParameters)
                        .setOutputAudioConfig(outputAudioConfig)
                        .build();
            } else if (tts) {
                detectIntentRequest = DetectIntentRequest.newBuilder()
                        .setSession(sessionName.toString())
                        .setQueryInput(queryInput)
                        .setOutputAudioConfig(outputAudioConfig)
                        .build();
            } else if (sentiment) {
                QueryParameters queryParameters = QueryParameters.newBuilder()
                        .setSentimentAnalysisRequestConfig(sentimentAnalysisRequestConfig)
                        .build();
                detectIntentRequest = DetectIntentRequest.newBuilder()
                        .setSession(sessionName.toString())
                        .setQueryInput(queryInput)
                        .setQueryParams(queryParameters)
                        .build();
            } else if (knowledge) {
                QueryParameters queryParameters = QueryParameters.newBuilder()
                        .addKnowledgeBaseNames(knowledgeBaseName.toString())
                        .build();
                detectIntentRequest = DetectIntentRequest.newBuilder()
                        .setSession(sessionName.toString())
                        .setQueryInput(queryInput)
                        .setQueryParams(queryParameters)
                        .build();
            } else {
                detectIntentRequest = DetectIntentRequest.newBuilder()
                        .setSession(sessionName.toString())
                        .setQueryInput(queryInput)
                        .build();
            }

            return detectIntentRequest;
        } catch (Exception ex) {
            return null;
        }
    }
}


