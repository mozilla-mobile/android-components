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

NEXUS_PREFIX='http://localhost:8081/nexus/content/repositories'
REPOS="-PgoogleRepo=$NEXUS_PREFIX/google/ -PcentralRepo=$NEXUS_PREFIX/central/"
GRADLE_ARGS="--parallel $REPOS"

# This command will download all dependencies that are statically declared. Any
# dependency added at runtime by gradle plugins cannot be downloaded by this
# command.
./gradlew downloadDependencies

./gradlew $GRADLE_ARGS -Pcoverage \
  :tooling-detekt:lintRelease :tooling-lint:lint :samples-browser:lint

. taskcluster/scripts/toolchain/android-gradle-dependencies/after.sh

popd
