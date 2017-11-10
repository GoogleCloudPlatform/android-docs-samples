# Android Studio Cloud Endpoints v2.0 Example

This sample is an example of a migrated [Android Studio Cloud Endpoints backend](https://cloud.google.com/tools/android-studio/app_engine/add_module)
using [Google Cloud Endpoints Frameworks 2.0](https://cloud.google.com/endpoints/docs/frameworks/java/about-cloud-endpoints-frameworks).

## Setup

This sample is an Android Studio 2.x project.

* [Install the Cloud SDK](https://cloud.google.com/sdk/docs/).
* [Initialize the Cloud SDK](https://cloud.google.com/sdk/docs/initializing)
* In Android Studio, use *Open an existing Android Studio project* to open the project.
* Install Android API Level 26, Android SDK Tools, and  Android SDK Build-Tools 26.x.x
* In [MainActivity.java](app/src/main/java/com/example/migration/endpoints/app/MainActivity.java)
update `YOUR-PROJECT-ID` with your Google Cloud Project. The related URL is the endpoint
of your Cloud Endpoints Frameworks project.

## Tests

This sample is tested using an instrumentation test on [Firebase Test Lab](https://firebase.google.com/docs/test-lab/)
to verify the end-to-end communication between an Android app and
a Cloud Endpoints Frameworks project. The test is initiated by
the `acceptance_test.sh` script.
