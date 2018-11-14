#!/bin/bash
# Copyright 2018 Google LLC.
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

# Fail on non-zero return and print command to stdout
set -xe

echo "Setting up speech translation microservice..."

git clone https://github.com/GoogleCloudPlatform/nodejs-docs-samples.git ./app/build/github/nodejs-docs-samples || true
pushd ./app/build/github/nodejs-docs-samples/functions/speech-to-speech
gcloud functions deploy speechTranslate --runtime nodejs6 --trigger-http \
    --update-env-vars ^:^OUTPUT_BUCKET=playchat-c5cc70f6-61ed-4640-91be-996721838560:SUPPORTED_LANGUAGE_CODES=en,es,fr
popd
