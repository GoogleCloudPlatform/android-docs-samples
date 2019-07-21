# Dialogflow Sample

This app demonstrates how to make gRPC connections to the [Dialogflow API](https://cloud.google.com/dialogflow-enterprise/)

The app demonstrates how to detect intents:
- Via Text
- Via Streaming Audio
- With Sentiment Analysis
- With Text-to-Speech
- With Knowledge Connectors

To call the Dialogflow API from Android, you need to get authorization tokens from Firebase Cloud Messaging for them to be accepted by the Dialogflow API. To get this token, this sample uses a Firebase Function (in Node.js) to generate these tokens on the behalf of a service account to be used by the app when making a request to the Dialogflow API.

## Prerequisites
- An Android device or emulator
- Android Studio 3 or later

## Setup
- Create a project (or use an existing one) in the [Google Cloud Console][cloud-console]
- Enable the [Dialogflow API](https://console.cloud.google.com/apis/library/dialogflow.googleapis.com).
- Enable the [IAM Service Account Credentials API](https://pantheon.corp.google.com/apis/library/iamcredentials.googleapis.com).
- [Enable billing][billing].
- Be sure that you have gone through the steps by expanding the [Create an agent](https://cloud.google.com/dialogflow-enterprise/docs/quickstart-console#create-an-agent) to create and configure your stopwatch agent.
- [Import the Dialogflow Agent](https://dialogflow.com/docs/agents/export-import-restore#import) using the `StopwatchAgent.zip` which is located in the `stopwatch` directory. 
- [Create a Service account](https://cloud.google.com/iam/docs/creating-managing-service-accounts) with the following IAM role: `Dialogflow API Client`. Example name: `dialogflow-client`. ([For more info on: how to add roles to a Service Account](https://cloud.google.com/iam/docs/granting-roles-to-service-accounts#granting_access_to_a_service_account_for_a_resource))
- Enable beta features for:
- [Sentiment Analysis](https://cloud.google.com/dialogflow-enterprise/docs/sentiment#enable_beta_features)
- [Text-to-Speech](https://cloud.google.com/dialogflow-enterprise/docs/detect-intent-tts#enable_beta_features)
- [Knowledge Connectors](https://cloud.google.com/dialogflow-enterprise/docs/knowledge-connectors#enable_beta_features)

### Setup the app
-Clone this repository `git clone https://github.com/GoogleCloudPlatform/android-docs-samples.git`
- Replace PROJECT_ID in AppController.java with your Project ID

###  Setup Firebase on the application:
- Complete the steps for [Add Firebase to your app](https://firebase.google.com/docs/android/setup) and expand the "Create a Firebase project" section for instructions on how to add project to your Firebase console. Note: No need to complete any other sections, they are already done. 
- In the [Firebase console](https://console.firebase.google.com/), open the "Authentication" section under Develop.
- On the **Sign-in Methods** page, enable the **Anonymous** sign-in method.

###  Setup and Deploy the Firebase Function 
The Firebase Function provides auth tokens to your app, You'll be using a provided sample function to be run with this app.

- Follow the steps in this [guide](https://firebase.google.com/docs/functions/get-started) for: 
- "1. Set up Node.js and the Firebase CLI"
- "2. Initialize Firebase SDK for Cloud Functions". 
- Replace `index.js` file with the [provided index.js](https://github.com/GoogleCloudPlatform/nodejs-docs-samples/blob/master/functions/dialogflow/functions/index.js).
- Open `index.js`, go to function "generateAccessToken", and replace “SERVICE-ACCOUNT-NAME@YOUR_PROJECT_ID.iam.gserviceaccount.com” with your Service account name (`dialogflow-client`) and project id. 
- Deploy getOAuthToken method by running command:
```
firebase deploy -—only functions
```
- For your "App Engine Default Service Account" add the following IAM role: `Service Account Token Creator` . ([For more info on: how to add roles to a Service Account](https://cloud.google.com/iam/docs/granting-roles-to-service-accounts#granting_access_to_a_service_account_for_a_resource))

- For more info please refer (https://firebase.google.com/docs/functions/get-started).

## Run the app
- You are now ready to build and run the project. In Android Studio you can do this by clicking the 'Play' button in the toolbar. This will launch the app on the emulator or on the device you've selected. 
- As soon the app launches, it will ask for the google sign-in.
- After successful signing in, choose the option by selecting a checkbox and click on chat button
- Type the message to send and click on the send button on the bottom right.
- Alternatively tap on the mic button to speak and send the message to the Dialogflow.


[cloud-console]: https://console.cloud.google.com
[git]: https://git-scm.com/
[android-studio]: https://developer.android.com/studio
[billing]: https://console.cloud.google.com/billing?project=_
[Firebase]: https://firebase.google.com/
