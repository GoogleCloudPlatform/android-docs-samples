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

# Fail on any error.
set -e

echo "Setting up Android environment..."

if [ -z "$ANDROID_HOME" ]; then
    mkdir -p ./app/build/android-sdk
    pushd ./app/build/android-sdk
    wget -N https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip
    unzip -o sdk-tools-linux-4333796.zip
    export ANDROID_HOME="./app/build/android-sdk"
    popd
fi

echo "Install Android SDK, tools, and build tools API 27, system image, and emulator..."
echo "y" | ${ANDROID_HOME}/tools/bin/sdkmanager \
    "platforms;android-27" "tools" "platform-tools" "build-tools;27.0.3" \
    "system-images;android-27;default;x86_64" "emulator"
export PATH=${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools:${PATH}
echo "Move to the tools/bin directory..."
pushd ${ANDROID_HOME}/tools/bin
echo "no" | ./avdmanager create avd -n playchat -k "system-images;android-27;default;x86_64" --force
