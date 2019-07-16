package com.google.cloud.examples.dialogflow.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.cloud.examples.dialogflow.AppController;
import com.google.cloud.examples.dialogflow.ui.ChatActivity;
import com.google.cloud.examples.dialogflow.ui.MainActivity;
import com.google.api.client.util.Maps;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.BidiStream;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.AudioEncoding;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.InputAudioConfig;
import com.google.cloud.dialogflow.v2.OutputAudioConfig;
import com.google.cloud.dialogflow.v2.OutputAudioEncoding;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryParameters;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SentimentAnalysisRequestConfig;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentRequest;
import com.google.cloud.dialogflow.v2.StreamingDetectIntentResponse;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.cloud.dialogflow.v2beta1.KnowledgeAnswers;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBaseName;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


public class ApiRequest {
    private String TAG = "API_REQUEST";

    // Variables needed to request an auth token
    private FirebaseFunctions firebaseFunctions;

    // Variables needed to retrieve an auth token
    private SimpleDateFormat simpleDateFormat;
    private String token = null;
    private Date tokenExpiration = null;

    public ApiRequest() {
        firebaseFunctions = FirebaseFunctions.getInstance();
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * function to call: detect Intent Sentiment Analysis | Detect Intent With TTS | KnowledgeBase
     * @param context       :   context
     * @param accessToken   :   access token received from fcm
     * @param expiryTime    :   expiry time received from fcm
     * @param type          :   type of api call
     * @param arg           :   message from user
     */
    public void callAPI(Context context, String accessToken, Date expiryTime, String type, String arg) {
        Toast.makeText(context, "Calling the API", Toast.LENGTH_SHORT).show();
        this.token = accessToken;
        this.tokenExpiration = expiryTime;
        if (type.equals("sentiment")) {
            detectIntentSentimentAnalysis(arg);
        } else if (type.equals("tts")) {
            detectIntentWithTexttoSpeech(arg);
        } else if (type.equals("knowledge")) {
            new MyKnowledgeBaseRequest().execute(arg);
        }
    }

    /**
     * function to call the detect input stream
     * @param context       :   context
     * @param accessToken   :   access token received from fcm
     * @param expiryTime    :   expiry time received from fcm
     * @param filePath      :   file path of the audio to be sent to dialog flow
     */
    public void callAudioAPI(Context context, String accessToken, Date expiryTime, String filePath) {
        Toast.makeText(context, "Calling the API", Toast.LENGTH_SHORT).show();
        this.token = accessToken;
        this.tokenExpiration = expiryTime;
        detectIntentStream(filePath);
    }

    /**
     * function to detect intent audio
     * @param audioFilePath :   path to the audio file
     */
    public void detectIntentAudio(String audioFilePath) {
        Map<String, QueryResult> queryResults = Maps.newHashMap();
        // Instantiates a client

        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);

            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();

            // Instantiates a client
            try (SessionsClient sessionsClient = SessionsClient.create(sessionsSettings)) {
                // Set the session name using the sessionId (UUID) and projectID (my-project-id)
                SessionName session = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);
                System.out.println("Session Path: " + session.toString());

                // Note: hard coding audioEncoding and sampleRateHertz for simplicity.
                // Audio encoding of the audio content sent in the query request.
                AudioEncoding audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16;
                int sampleRateHertz = 16000;

                // Instructs the speech recognizer how to process the audio content.
                InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                        .setAudioEncoding(audioEncoding) // audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16
                        .setLanguageCode("en-US") // languageCode = "en-US"
                        .setSampleRateHertz(sampleRateHertz) // sampleRateHertz = 16000
                        .build();

                // Build the query with the InputAudioConfig
                QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();

                // Read the bytes from the audio file
                byte[] inputAudio = Files.readAllBytes(Paths.get(audioFilePath));

                // Build the DetectIntentRequest
                DetectIntentRequest request = DetectIntentRequest.newBuilder()
                        .setSession(session.toString())
                        .setQueryInput(queryInput)
                        .setInputAudio(ByteString.copyFrom(inputAudio))
                        .build();

                // Performs the detect intent request
                DetectIntentResponse response = sessionsClient.detectIntent(request);

                // Display the query result
                QueryResult queryResult = response.getQueryResult();
                System.out.println("====================");
                System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
                System.out.format("Detected Intent: %s (confidence: %f)\n",
                        queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
                System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
                MainActivity.resultsTextView.setText(queryResult.getFulfillmentText());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * function to detect input stream
 * @param audioFilePath :   path to audio file
     */
    private void detectIntentStream(String audioFilePath) {

        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);

            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();

            // Instantiates a client
            try (SessionsClient sessionsClient = SessionsClient.create(sessionsSettings)) {
                // Set the session name using the sessionId (UUID) and projectID (my-project-id)
                SessionName session = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);
                System.out.println("Session Path: " + session.toString());

                // Note: hard coding audioEncoding and sampleRateHertz for simplicity.
                // Audio encoding of the audio content sent in the query request.
                AudioEncoding audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16;
                int sampleRateHertz = 16000;

                // Instructs the speech recognizer how to process the audio content.
                InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                        .setAudioEncoding(audioEncoding) // audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16
                        .setLanguageCode("en-US") // languageCode = "en-US"
                        .setSampleRateHertz(sampleRateHertz) // sampleRateHertz = 16000
                        .build();

                QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();

                // Create the Bidirectional stream
                BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream =
                        sessionsClient.streamingDetectIntentCallable().call();

                // The first request must **only** contain the audio configuration:
                bidiStream.send(StreamingDetectIntentRequest.newBuilder()
                        .setSession(session.toString())
                        .setQueryInput(queryInput)
                        .build());

                try (FileInputStream audioStream = new FileInputStream(audioFilePath)) {
                    // Subsequent requests must **only** contain the audio data.
                    // Following messages: audio chunks. We just read the file in fixed-size chunks. In reality
                    // you would split the user input by time.
                    byte[] buffer = new byte[4096];
                    int bytes;
                    while ((bytes = audioStream.read(buffer)) != -1) {
                        bidiStream.send(
                                StreamingDetectIntentRequest.newBuilder()
                                        .setInputAudio(ByteString.copyFrom(buffer, 0, bytes))
                                        .build());
                    }
                }

                // Tell the service you are done sending data
                bidiStream.closeSend();

                for (StreamingDetectIntentResponse response : bidiStream) {
                    QueryResult queryResult = response.getQueryResult();
                    System.out.println("====================");
                    System.out.format("Intent Display Name: %s\n", queryResult.getIntent().getDisplayName());
                    System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
                    System.out.format("Detected Intent: %s (confidence: %f)\n",
                            queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
                    System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
                    if (!queryResult.getFulfillmentText().equals("")) {
                        MainActivity.resultsTextView.setText("Fulfillment Text: " + queryResult.getFulfillmentText());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    // [END dialogflow_detect_intent_streaming]

    /**
     * function to detect input sentiment analysis
     * @param msg   :   message sent from user
     */
    private void detectIntentSentimentAnalysis(String msg) {
        List<String> texts = new ArrayList<>();
        texts.add(msg);
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);

            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            // Instantiates a client
            try (SessionsClient sessionsClient = SessionsClient.create(sessionsSettings)) {
                // Set the session name using the sessionId (UUID) and projectID (my-project-id)
                SessionName session = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);
                System.out.println("Session Path: " + session.toString());

                // Detect intents for each text input
                for (String text : texts) {
                    // Set the text (hello) and language code (en-US) for the query
                    TextInput.Builder textInput = TextInput.newBuilder().setText(text).setLanguageCode("en-US");

                    // Build the query with the TextInput
                    QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

                    //
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

                    System.out.println("====================");
                    System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
                    System.out.format(
                            "Detected Intent: %s (confidence: %f)\n",
                            queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
                    System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
                    System.out.format(
                            "Sentiment Score: '%s'\n",
                            queryResult.getSentimentAnalysisResult().getQueryTextSentiment().getScore());
                    //MainActivity.resultsTextView.setText(queryResult.getFulfillmentText());
                    ChatActivity.addMsg(queryResult.getFulfillmentText(), 0);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    // [END dialogflow_detect_intent_with_sentiment_analysis]

    /**
     * function to detect input with TTS
     * @param queryMsg   :   message sent from user
     */
    private void detectIntentWithTexttoSpeech(String queryMsg) {
        List<String> texts = new ArrayList<>();
        texts.add(queryMsg);
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);

            SessionsSettings sessionsSettings = SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            // Instantiates a client
            try (SessionsClient sessionsClient = SessionsClient.create(sessionsSettings)) {
                // Set the session name using the sessionId (UUID) and projectID (my-project-id)
                SessionName session = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);
                System.out.println("Session Path: " + session.toString());

                // Detect intents for each text input
                for (String text : texts) {
                    // Set the text (hello) and language code (en-US) for the query
                    TextInput.Builder textInput = TextInput.newBuilder().setText(text).setLanguageCode("en-US");

                    // Build the query with the TextInput
                    QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

                    //
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

                    System.out.println("====================");
                    System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
                    System.out.format(
                            "Detected Intent: %s (confidence: %f)\n",
                            queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
                    System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
                    ChatActivity.addMsg(queryResult.getFulfillmentText(), 0);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * function to detect intent knowledge
     * @param knowledgebaseId   :   Knowledge base id
     * @param msg               :   Message sent from user
     */
    private void detectIntentKnowledge(String knowledgebaseId,
                                       String msg) {
        List<String> texts = new ArrayList<>();
        texts.add(msg);
        try {
            AccessToken accessToken = new AccessToken(token, tokenExpiration);
            Credentials credentials = GoogleCredentials.create(accessToken);
            FixedCredentialsProvider fixedCredentialsProvider = FixedCredentialsProvider.create(credentials);
            com.google.cloud.dialogflow.v2beta1.SessionsSettings sessionsSettings = com.google.cloud.dialogflow.v2beta1.SessionsSettings.newBuilder().setCredentialsProvider(fixedCredentialsProvider).build();
            // Instantiates a client
            try (com.google.cloud.dialogflow.v2beta1.SessionsClient sessionsClient = com.google.cloud.dialogflow.v2beta1.SessionsClient.create(sessionsSettings)) {
                // Set the session name using the sessionId (UUID) and projectID (my-project-id)
                SessionName session = SessionName.of(AppController.PROJECT_ID, AppController.SESSION_ID);
                System.out.println("Session Path: " + session.toString());

                // Detect intents for each text input
                for (String text : texts) {
                    // Set the text and language code (en-US) for the query
                    com.google.cloud.dialogflow.v2beta1.TextInput.Builder textInput = com.google.cloud.dialogflow.v2beta1.TextInput.newBuilder().setText(text).setLanguageCode("en-US");
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

                    System.out.format("Knowledge results:\n");
                    System.out.format("====================\n");
                    System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
                    System.out.format(
                            "Detected Intent: %s (confidence: %f)\n",
                            queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
                    System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
                    KnowledgeAnswers knowledgeAnswers = queryResult.getKnowledgeAnswers();
                    for (KnowledgeAnswers.Answer answer : knowledgeAnswers.getAnswersList()) {
                        System.out.format(" - Answer: '%s'\n", answer.getAnswer());
                        System.out.format(" - Confidence: '%s'\n", answer.getMatchConfidence());
                        ChatActivity.addMsg(answer.getAnswer(), 0);
                    }
                    if (knowledgeAnswers.getAnswersCount() == 0) {
                        ChatActivity.addMsg("No Response", 0);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * function to get the detect the intent
     */
    private void detectIntent() {
        Map<String, QueryResult> queryResults = Maps.newHashMap();
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

                List<String> texts = new ArrayList<>();
                texts.add("book a room");
                // Detect intents for each text input
                for (String text : texts) {
                    // Set the text (hello) and language code (en-US) for the query
                    TextInput textInput = TextInput.newBuilder()
                            .setText(text)
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

                    System.out.println("====================");
                    System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
                    System.out.format("Detected Intent: %s (confidence: %f)\n",
                            queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
                    System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());

                    queryResults.put(text, queryResult);
                    MainActivity.resultsTextView.setText(queryResult.getFulfillmentText());

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
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

                ArrayList<String> ids = KnowledgebaseUtils.listKnowledgeBases(AppController.PROJECT_ID, sessionsSettings);

                if (ids.size() > 0) {

                    return ids.get(0);

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
