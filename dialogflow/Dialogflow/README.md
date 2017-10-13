# Google Cloud Dialogflow Enterprise examples

This directory contains Android example that uses the
[Google Cloud Dialogflow Enterprise](https://cloud.google.com/conversation/).

## Prerequisites

### Enable Cloud Dialogflow Enterprise

If you have not already done so, [enable Cloud Dialogflow Enterprise for your project](
https://cloud.google.com/conversation/docs/quickstart).

### Set Up to Authenticate With Your Project's Credentials

This Android app uses JSON credential file locally stored in the resources. ***You should not do
this in your production app.*** Instead, you should set up your own backend server that
authenticates app users. The server should delegate API calls from your client app. This way, you
can enforce usage quota per user. Alternatively, you should get the access token on the server side,
and supply client app with it. The access token will expire in a short while.

In this sample, we just put the Service Account in the client for ease of use. The app still gets
an access token using the service account credential, and use the token to call the API, so you can
see how to do so.

In order to try out this sample, visit the [Cloud Console](https://console.cloud.google.com/), and
navigate to:
`API Manager > Credentials > Create credentials > Service account key > New service account`.
Create a new service account, and download the JSON credentials file. Put the file in the app
resources as `app/src/main/res/raw/credential.json`.

Again, ***you should not do this in your production app.***

See the [Cloud Platform Auth Guide](https://cloud.google.com/docs/authentication#developer_workflow)
for more information.

### Project name and agent name for Dialogflow

Open the file `gradle.properties` and change the Dialogflow project name and agent name there. 

```
dialogflowProjectName=(your project name here)
dialogflowAgentName(your agent name here)
dialogflowLanguageCode=(language code, such as en-US)
```
