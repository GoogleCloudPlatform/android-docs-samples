package com.google.cloud.examples.dialogflow.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.api.client.util.Maps;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.OutputAudioConfig;
import com.google.cloud.dialogflow.v2.OutputAudioEncoding;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryParameters;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SentimentAnalysisRequestConfig;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.cloud.dialogflow.v2beta1.KnowledgeAnswers;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBaseName;
import com.google.cloud.examples.dialogflow.AppController;
import com.google.cloud.examples.dialogflow.ui.ChatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;


public class ApiRequest {

    // Variables needed to retrieve an auth token
    private SimpleDateFormat simpleDateFormat;
    private String token = null;
    private Date tokenExpiration = null;

    public ApiRequest() {
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * function to call: detect Intent Sentiment Analysis | Detect Intent With TTS | KnowledgeBase
     *
     * @param context     :   context
     * @param accessToken :   access token received from fcm
     * @param expiryTime  :   expiry time received from fcm
     * @param msg         :   messagen sent by the user
     * @param tts         :   send mesaage to text to speech if true
     * @param sentiment   :   send mesaage to sentiment analysis if true
     * @param knowledge   :   send mesaage to knowledge base if true
     */
    public void callAPI(Context context, String accessToken, Date expiryTime, String msg, boolean tts, boolean sentiment, boolean knowledge) {
        Toast.makeText(context, "Calling the API", Toast.LENGTH_SHORT).show();
        this.token = accessToken;
        this.tokenExpiration = expiryTime;
        if (sentiment) {
            detectIntentSentimentAnalysis(msg);
        }
        if (tts) {
            detectIntentWithTextToSpeech(msg);
        }
        if (knowledge) {
            new MyKnowledgeBaseRequest().execute(msg);
        }

        if(!tts && !sentiment && !knowledge) {
            detectIntent(msg);
        }
    }

    /**
     * function to get the detect the intent
     */
    private void detectIntent(String msg) {
        // Instantiates a client
        AccessToken accessToken = new AccessToken(token, tokenExpiration);
        Credentials credentials = GoogleCredentials.create(accessToken);
        FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
        try {
            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();

            try (SessionsClient sessionsClient = SessionsClient.create(sessionsSettings)) {
                // Set the session name using the sessionId (UUID) and projectID (my-project-id)
                SessionName session = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);
                System.out.println("Session Path: " + session.toString());

                // Set the text (hello) and language code (en-US) for the query
                TextInput textInput = TextInput.newBuilder()
                        .setText(msg)
                        .setLanguageCode("en-US")
                        .build();

                // Build the query with the TextInput
                QueryInput queryInput = QueryInput.newBuilder()
                        .setText(textInput)
                        .build();

                // Performs the detect intent request
                DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

                // Display the query result
                QueryResult queryResult = response.getQueryResult();
                ChatActivity.addMsg("Detect Intent: " + queryResult.getFulfillmentText() + "(confidence: " + queryResult.getIntentDetectionConfidence() + ")", 0);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * function to detect input sentiment analysis
     *
     * @param msg :   message sent from user
     */
    private void detectIntentSentimentAnalysis(String msg) {
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);

            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            // Instantiates a client
            try (SessionsClient sessionsClient = SessionsClient.create(sessionsSettings)) {
                // Set the session name using the sessionId (UUID) and projectID (my-project-id)
                SessionName session = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);

                // Set the text (hello) and language code (en-US) for the query
                TextInput.Builder textInput = TextInput.newBuilder().setText(msg).setLanguageCode("en-US");

                // Build the query with the TextInput
                QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

                SentimentAnalysisRequestConfig sentimentAnalysisRequestConfig =
                        SentimentAnalysisRequestConfig.newBuilder().setAnalyzeQueryTextSentiment(true).build();

                QueryParameters queryParameters =
                        QueryParameters.newBuilder()
                                .setSentimentAnalysisRequestConfig(sentimentAnalysisRequestConfig)
                                .build();
                DetectIntentRequest detectIntentRequest =
                        DetectIntentRequest.newBuilder()
                                .setSession(session.toString())
                                .setQueryInput(queryInput)
                                .setQueryParams(queryParameters)
                                .build();

                // Performs the detect intent request
                DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);

                // Display the query result
                QueryResult queryResult = response.getQueryResult();
                ChatActivity.addMsg("Sentiment Analysis: " + queryResult.getFulfillmentText() + "(confidence: " + queryResult.getIntentDetectionConfidence() + ")", 0);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * function to detect input with TTS
     *
     * @param queryMsg :   message sent from user
     */
    private void detectIntentWithTextToSpeech(String queryMsg) {
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);

            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            // Instantiates a client
            try (SessionsClient sessionsClient = SessionsClient.create(sessionsSettings)) {
                // Set the session name using the sessionId (UUID) and projectID (my-project-id)
                SessionName session = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);

                // Set the text (hello) and language code (en-US) for the query
                TextInput.Builder textInput = TextInput.newBuilder().setText(queryMsg).setLanguageCode("en-US");

                // Build the query with the TextInput
                QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

                OutputAudioEncoding audioEncoding = OutputAudioEncoding.OUTPUT_AUDIO_ENCODING_LINEAR_16;
                int sampleRateHertz = 16000;
                OutputAudioConfig outputAudioConfig =
                        OutputAudioConfig.newBuilder()
                                .setAudioEncoding(audioEncoding)
                                .setSampleRateHertz(sampleRateHertz)
                                .build();

                DetectIntentRequest dr =
                        DetectIntentRequest.newBuilder()
                                .setQueryInput(queryInput)
                                .setOutputAudioConfig(outputAudioConfig)
                                .setSession(session.toString())
                                .build();

                // Performs the detect intent request
                // DetectIntentResponse response = sessionsClient.detectIntent(session,
                // queryInput,outputAudioConfig);
                DetectIntentResponse response = sessionsClient.detectIntent(dr);

                // Display the query result
                QueryResult queryResult = response.getQueryResult();
                ChatActivity.addMsg("Text To Speech: " + queryResult.getFulfillmentText() + " (Confidence: " + queryResult.getIntentDetectionConfidence() + ")", 0);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * function to detect intent knowledge
     *
     * @param knowledgebaseId :   Knowledge base id
     * @param msg             :   Message sent from user
     */
    private void detectIntentKnowledge(String knowledgebaseId,
                                       String msg) {
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
            com.google.cloud.dialogflow.v2beta1.SessionsSettings sessionsSettings = com.google.cloud.dialogflow.v2beta1.SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            // Instantiates a client
            try (com.google.cloud.dialogflow.v2beta1.SessionsClient sessionsClient = com.google.cloud.dialogflow.v2beta1.SessionsClient.create(sessionsSettings)) {
                // Set the session name using the sessionId (UUID) and projectID (my-project-id)
                SessionName session = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);

                // Set the text and language code (en-US) for the query
                com.google.cloud.dialogflow.v2beta1.TextInput.Builder textInput = com.google.cloud.dialogflow.v2beta1.TextInput.newBuilder().setText(msg).setLanguageCode("en-US");
                // Build the query with the TextInput
                com.google.cloud.dialogflow.v2beta1.QueryInput queryInput = com.google.cloud.dialogflow.v2beta1.QueryInput.newBuilder().setText(textInput).build();

                KnowledgeBaseName knowledgeBaseName = KnowledgeBaseName.of(AppController.PROJECT_ID, knowledgebaseId);
                com.google.cloud.dialogflow.v2beta1.QueryParameters queryParameters =
                        com.google.cloud.dialogflow.v2beta1.QueryParameters.newBuilder()
                                .addKnowledgeBaseNames(knowledgeBaseName.toString())
                                .build();

                com.google.cloud.dialogflow.v2beta1.DetectIntentRequest detectIntentRequest =
                        com.google.cloud.dialogflow.v2beta1.DetectIntentRequest.newBuilder()
                                .setSession(session.toString())
                                .setQueryInput(queryInput)
                                .setQueryParams(queryParameters)
                                .build();
                // Performs the detect intent request
                com.google.cloud.dialogflow.v2beta1.DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);

                // Display the query result
                com.google.cloud.dialogflow.v2beta1.QueryResult queryResult = response.getQueryResult();

                KnowledgeAnswers knowledgeAnswers = queryResult.getKnowledgeAnswers();
                for (KnowledgeAnswers.Answer answer : knowledgeAnswers.getAnswersList()) {
                    ChatActivity.addMsg("Knowledge Base: " + answer.getAnswer() + " (Confidence: " + answer.getMatchConfidence() + ")", 0);
                }
                if (knowledgeAnswers.getAnswersCount() == 0) {
                    ChatActivity.addMsg("Knowledge Base: No Response", 0);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * AsyncTask class to get the knowledgebase id
     */
    private class MyKnowledgeBaseRequest extends AsyncTask<String, Void, String> {

        private String msg = "";

        @Override
        protected String doInBackground(String... args) {
            msg = args[0];

            try {
                AccessToken accessToken = new AccessToken(token, tokenExpiration);
                Credentials credentials = GoogleCredentials.create(accessToken);
                FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
                com.google.cloud.dialogflow.v2beta1.KnowledgeBasesSettings sessionsSettings = com.google.cloud.dialogflow.v2beta1.KnowledgeBasesSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();

                ArrayList<String> knowledgeBaseNames = KnowledgebaseUtils.listKnowledgeBases(AppController.PROJECT_ID, sessionsSettings);
                if (knowledgeBaseNames.size() > 0) {
                    return knowledgeBaseNames.get(0);
                }

            } catch (Exception ex) {
                return "";
            }

            return "";
        }

        @Override
        protected void onPostExecute(String str) {
            super.onPostExecute(str);

            if (!str.equals("")) {
                detectIntentKnowledge(str.substring(str.lastIndexOf("/") + 1), msg);
            }
        }
    }
}
