#!/usr/bin/env bash

# Copyright 2017 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Fail on non-zero return and print command to stdout
set -xe

# Update YOUR-PROJECT-ID with project id version
sed -i'.bak' \
    -e "s/YOUR-PROJECT-ID/${GOOGLE_VERSION_ID}-dot-${GOOGLE_PROJECT_ID}/g" \
    app/src/main/java/com/example/migration/endpoints/app/MainActivity.java

# Deploy backend
./gradlew backend:appengineUpdate \
    -Pappengine.deploy.application=${GOOGLE_PROJECT_ID} \
    -Pappengine.deploy.version="${GOOGLE_VERSION_ID}" \
    -Pappengine.deploy.serviceAccount="${GOOGLE_APPLICATION_CREDENTIALS}"

# Generate apk from "app" module
./gradlew app:assembleAndroidTest
./gradlew app:assembleDebug

# Run Tests on Firebase Test Lab
gcloud firebase test android run \
   --type instrumentation \
   --app  app/build/outputs/apk/app-debug.apk \
   --test app/build/outputs/apk/app-debug-androidTest.apk \
   --device model=sailfish,version=25,locale=en,orientation=portrait  \
   --timeout 5m

