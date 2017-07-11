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

set -ex

# Set pipefail so that `egrep` does not eat the exit code.
set -o pipefail
shopt -s globstar

# Finds the closest parent dir that encompasses all changed files, and has a
# build.gradle
travis_changed_files_parent() {
  # If we're not in a PR, forget it
  [ -z "${CI_PULL_REQUEST}" ] && return 0

  (
    set +e

    changed="$(git diff --name-only "${CIRCLE_SHA1}" master)"
    if [ $? -ne 0 ]; then
      # Fall back to git head
      changed="$(git diff --name-only "$(git rev-parse HEAD)" "${CIRCLE_BRANCH}")"
      [ $? -ne 0 ] && return 0  # Give up. Just run everything.
    fi

    # Find the common prefix
    prefix="$(echo "$changed" | \
      # N: Do this for a pair of lines
      # s: capture the beginning of a line, that's followed by a new line
      #    starting with that capture group. IOW - two lines that start with the
      #    same zero-or-more characters. Replace it with just the capture group
      #    (ie the common prefix).
      # D: Delete the first line of the pair, leaving the second line for the
      #    next pass.
      sed -e 'N;s/^\(.*\).*\n\1.*$/\1\n\1/;D')"

    while [ ! -z "$prefix" ] && [ ! -r "$prefix/build.gradle" ] && [ ! -r "$prefix/jenkins.sh" ] && [ "${prefix%/*}" != "$prefix" ]; do
      prefix="${prefix%/*}"
    done

    [ -r "$prefix/build.gradle" ] || [ -r "$prefix/jenkins.sh" ] || return 0

    echo "$prefix"
  )
}

common_changed_dir="$(travis_changed_files_parent)"

[ -z "${common_changed_dir}" ] || pushd "${common_travis_dir}"



[ -z "${common_changed_dir}" ] || popd


# Check that all shell scripts in this repo (including this one) pass the
# Shell Check linter.
shellcheck ./**/*.sh


