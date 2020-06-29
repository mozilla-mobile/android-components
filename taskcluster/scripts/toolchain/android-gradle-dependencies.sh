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
# XXX We don't build samples-browsers at all. They don't have any extra dependencies so we can
# filter them out. Other components are built in second pass
COMPONENTS_TO_EXCLUDE_IN_FIRST_PASS='(samples-browser|tooling-detekt|tooling-lint)'
FIRST_PASS_COMPONENTS=$(grep -E "$COMPONENT_REGEX" "$PROJECT_DIR/.buildconfig.yml" | sed -E "s/$COMPONENT_REGEX/:\1/g" | grep -E -v "$COMPONENTS_TO_EXCLUDE_IN_FIRST_PASS")

ASSEMBLE_COMMANDS=$(echo "$FIRST_PASS_COMPONENTS" | sed "s/$/:assemble/g")
ASSEMBLE_TEST_COMMANDS=$(echo "$FIRST_PASS_COMPONENTS" | sed "s/$/:assembleAndroidTest/g")
TEST_COMMANDS=$(echo "$FIRST_PASS_COMPONENTS" | sed "s/$/:test/g")
LINT_COMMANDS=$(echo "$FIRST_PASS_COMPONENTS" | sed "s/$/:lintRelease/g")

NEXUS_PREFIX='http://localhost:8081/nexus/content/repositories'
GRADLE_ARGS="--parallel -PgoogleRepo=$NEXUS_PREFIX/google/ -PjcenterRepo=$NEXUS_PREFIX/jcenter/ -PcentralRepo=$NEXUS_PREFIX/central/"

# First pass. We build everything to be sure to fetch all dependencies
./gradlew $GRADLE_ARGS $ASSEMBLE_COMMANDS $ASSEMBLE_TEST_COMMANDS ktlint detekt $LINT_COMMANDS
# Some tests may be flaky, although they still download dependencies. So we let the following
# command fail, if needed.
set +e; ./gradlew $GRADLE_ARGS -Pcoverage $TEST_COMMANDS; set -e

# Second pass. For an unknown reason, gradle optimize away some of the tasks below, if they're
# part of the first pass. That's why the second pass is introduced.
./gradlew $GRADLE_ARGS -Pcoverage \
  :tooling-detekt:assemble :tooling-detekt:assembleAndroidTest :tooling-detekt:test :tooling-detekt:lintRelease \
  :tooling-lint:assemble :tooling-lint:assembleAndroidTest :tooling-lint:test :tooling-lint:lint
  # :tooling-lint:lintRelease doesn't exist

. taskcluster/scripts/toolchain/android-gradle-dependencies/after.sh

popd
