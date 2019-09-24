# Cloud Speech to Speech Translation Sample

This app demonstrates how to create a live translation service using the Cloud Speech-To-Text,
Translation, and Text-To-Speech APIs. It uses these apis to:

- Make streaming gRPC connections to the Cloud Speech API` to recognize speech in recorded audio.
- Send transcripts to the Translate API to get the translation.
- Send translated text to the Text-to-Speech API so that it can be played back to the user.

To call the Dialogflow API from Android, you need provide authorization tokens with each request. To
get this token, this sample uses a Firebase Function to genereate these tokens on the behalf of a
service account. The token is returned to the app via Firebase Cloud Messaging.

## Prerequisites
- An Android device or emulator
- Android Studio 3 or later

## Setup
- Create a project (or use an existing one) in the [Google Cloud Console](https://console.cloud.google.com)
- [Enable billing](https://console.cloud.google.com/billing?project=_) and the
    - [Speech API](https://console.cloud.google.com/apis/library/speech.googleapis.com).
    - [Translate API](https://console.cloud.google.com/apis/library/translate.googleapis.com).
    - [Text-to-Speech API](https://console.cloud.google.com/apis/library/texttospeech.googleapis.com).
    - [IAM Service Account Credentials API](https://console.cloud.google.com/apis/library/iamcredentials.googleapis.com).
- [Create a Service account](https://cloud.google.com/iam/docs/creating-managing-service-accounts) with the following IAM role: `Cloud Translation API User`. Example name: `speech-to-speech-client`. ([For more info on: how to add roles to a Service Account](https://cloud.google.com/iam/docs/granting-roles-to-service-accounts#granting_access_to_a_service_account_for_a_resource))

### Setup the app
- Clone this repository `git clone https://github.com/GoogleCloudPlatform/android-docs-samples.git`
- `cd speech/Speech-to-Speech`
- Replace `GCP_PROJECT_ID` in strings.xml with your Project ID

###  Setup Firebase on the application:
- Complete the steps for [Add Firebase to your app](https://firebase.google.com/docs/android/setup)
and expand the "Create a Firebase project" section for instructions on how to add project to your
Firebase console. Note: No need to complete any other sections, they are already done. 
- In the [Firebase console](https://console.firebase.google.com/), open the "Authentication" section under Develop.
- On the **Sign-in Methods** page, enable the **Anonymous** sign-in method.
- Give the package name of the app as `com.google.cloud.examples.speechtospeech`

###  Setup and Deploy the Firebase Function 
The Firebase Function provides auth tokens to your app, You'll be using a provided sample function to be run with this app.

- Follow the steps in this [guide](https://firebase.google.com/docs/functions/get-started) for: 
  - "2. Set up Node.js and the Firebase CLI"
  - "3. Initialize Firebase SDK for Cloud Functions". 
- Replace `index.js` file with the [provided index.js](https://github.com/GoogleCloudPlatform/nodejs-docs-samples/blob/master/functions/tokenservice/functions/index.js).
- Open `index.js`, go to the `generateAccessToken` function, and replace`SERVICE-ACCOUNT-NAME@YOUR_PROJECT_ID.iam.gserviceaccount.com` with your Service account name (`speech-to-speech-client`) and project id. 
- Deploy getOAuthToken method by running command:
```
firebase deploy --only functions
```
- On the GCP console, add the following IAM role: `Service Account Token Creator` to your
"App Engine Default Service Account" ([For more info on: how to add roles to a Service Account](https://cloud.google.com/iam/docs/granting-roles-to-service-accounts#granting_access_to_a_service_account_for_a_resource))
- Open the [Firebase console](https://console.firebase.google.com/)
  - Select Databases on the side menu
  - Select Firestore
  - Click `Start collection`
  - Title your collection `ShortLivedAuthTOkens` and click Next
  - Enter `OauthToken` for the Document title and Save
- For more info please refer (https://firebase.google.com/docs/functions/get-started).

## Run the app
- You are now ready to build and run the project. In Android Studio you can do this by clicking the 'Play' button in the toolbar. This will launch the app on the emulator or on the device you've selected. 
- As soon the app launches, it will ask for the google sign-in.
- After successful signing in, choose the option by selecting a checkbox and click on chat button
- Type the message to send and click on the send button on the bottom right.
- Alternatively tap on the mic button to speak and send the message to the Dialogflow.
