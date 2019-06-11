# Cloud Data Loss Prevention API Example

This directory contains an Android example that uses the
[Cloud Data Loss Prevention (DLP) API](https://cloud.google.com/dlp/)
with the [Google Cloud Client Library for Java]

## Prerequisites

### Enable the Data Loss Prevention API

If you have not already done so,
[enable the DLP API for your project](https://console.cloud.google.com/flows/enableapi?apiid=dlp.googleapis.com).

### Set Up to Authenticate With Your Project's Credentials

This Android app uses a JSON credential file locally stored in the raw resources folder. 
***You should not do this in your production app.*** 
Instead, you should set up your own backend server that authenticates app users. 
The server should delegate API calls from your client app and enforce usage quota per user.
Alternatively, you should get an access token on the server side,
and supply them to your client app. Tokens expire after a short amount of time. For example:

```java
// read JSON credential into stream and generate a credential using an access token
GoogleCredentials credentials = GoogleCredentials.fromStream(stream).createScoped(SCOPE);
AccessToken token = credentials.refreshAccessToken();
return new GoogleCredentials(accessToken);
```

In this example, we put the Service Account in the client so no backend server is needed 
to run it.

In order to try out this sample, visit the [Cloud Console](https://console.cloud.google.com/), and
navigate to:
`API Manager > Credentials > Create credentials > Service account key > New service account`.
Create a new service account, and download the JSON credentials file. Put the file in the app
resources as `app/src/main/res/raw/credential.json`.

Again, ***you should not do this in your production app.***

See the [Cloud Platform Auth Guide](https://cloud.google.com/docs/authentication#developer_workflow)
for more information.

## Client Libraries on Android

The [Google Cloud Client Library for Java] that is used for this example can be used on Android,
but it is not optimized and is considered experimental. Specifically, it does not use the 
[lite](https://github.com/google/protobuf/blob/master/java/lite.md) version of protocol buffers 
that are optimized for mobile, it has some dependencies that may not be needed on Android, and 
it does not help with the authentication issues mentioned the previous section. We are working 
on this and encourage you to try out the client libraries on Android and give us feedback so 
that we can improve them.

## Run the Example

Open this example and Android studio and run the app. This example uses the device's camera
so if you are using an emulator it is recommended to set a webcam as the camera 
(you can do this when you create a new device with the AVD manager).

[Google Cloud Client Library for Java]: https://github.com/GoogleCloudPlatform/google-cloud-java
