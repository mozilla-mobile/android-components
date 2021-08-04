#!/bin/bash

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

set -ex

function get_abs_path {
    local file_path="$1"
    echo "$( cd "$(dirname "$file_path")" >/dev/null 2>&1 ; pwd -P )"
}

CURRENT_DIR="$(get_abs_path $0)"
PROJECT_DIR="$(get_abs_path $CURRENT_DIR/../../../..)"

pushd $PROJECT_DIR

. taskcluster/scripts/toolchain/android-gradle-dependencies/before.sh

COMPONENT_REGEX='^  ([-a-z]+):$'
FIRST_PASS_COMPONENTS=$(grep -E "$COMPONENT_REGEX" "$PROJECT_DIR/.buildconfig.yml" | sed -E "s/$COMPONENT_REGEX/:\1/g")
DEPENDENCY_COMMANDS=$(echo "$FIRST_PASS_COMPONENTS" | sed "s/$/:downloadDependencies/g")

NEXUS_PREFIX='http://localhost:8081/nexus/content/repositories'
REPOS="-PgoogleRepo=$NEXUS_PREFIX/google/ -PcentralRepo=$NEXUS_PREFIX/central/"
GRADLE_ARGS="--parallel $REPOS -d"
# First pass. We build everything to be sure to fetch all dependencies

./gradlew $GRADLE_ARGS $DEPENDENCY_COMMANDS

. taskcluster/scripts/toolchain/android-gradle-dependencies/after.sh

popd
