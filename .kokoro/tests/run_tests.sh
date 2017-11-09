#!/bin/bash

# Copyright 2017 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#Fail on Exit
set -euxo pipefail

export GOOGLE_APPLICATION_CREDENTIALS=${KOKORO_GFILE_DIR}/secrets-password.txt
export GOOGLE_PROJECT_ID=android-docs-samples
export CLOUDSDK_ACTIVE_CONFIG_NAME=android-docs-samples

apt-get -qq update \
 && apt-get -qq -y upgrade \
 && apt-get -qq -y install \
    unzip \
    wget \
    openjdk-8-jdk 

update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

# Install gcloud
if [ ! -d ${HOME}/google-cloud-sdk ]; then
    pushd ${HOME}
    wget https://dl.google.com/dl/cloudsdk/release/google-cloud-sdk.tar.gz --directory-prefix=${HOME}
    tar xzf google-cloud-sdk.tar.gz
    ./google-cloud-sdk/install.sh --usage-reporting false --path-update false --command-completion false
    popd
fi

# Install gradle
if [ ! -d ${HOME}/gradle ]; then
	mkdir -p ${HOME}/gradle
    pushd "${HOME}/gradle"
    wget https://services.gradle.org/distributions/gradle-3.3-bin.zip
    unzip gradle-3.3-bin.zip
    mv gradle-3.3 gradle
    popd
fi

if [ ! -d ${HOME}/android ]; then
	mkdir -p ${HOME}/android
    pushd "${HOME}/android"
    wget https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip
    unzip sdk-tools-linux-3859397.zip
    popd
fi

export ANDROID_HOME="${HOME}/android"
# Install Android SDK, tools, and build tools API 26
echo "y" | ${ANDROID_HOME}/tools/bin/sdkmanager "platforms;android-26" "tools" "build-tools;26.0.1"

export PATH=${HOME}/google-cloud-sdk/bin:${HOME}/appengine-java-sdk/bin:${HOME}/maven/apache-maven/bin:${HOME}/gradle/gradle/bin:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools:${PATH}

gcloud -q components update app-engine-java

# Clear the Maven cache.
rm -rf ~/.m2/repository

java -version
gradle --version

# activate the service account
gcloud config configurations create ${CLOUDSDK_ACTIVE_CONFIG_NAME} || /bin/true
gcloud -q auth activate-service-account --key-file ${KOKORO_GFILE_DIR}/secrets-password.txt
gcloud -q config set project ${GOOGLE_PROJECT_ID}

gcloud info

## BEGIN TESTS ##
pushd github/android-docs-samples
bash run-tests.sh
popd
## END TESTS ##



