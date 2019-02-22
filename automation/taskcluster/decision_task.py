# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
Decision task
"""

from __future__ import print_function

import argparse
import os
import taskcluster
import sys

from checksumdir import dirhash
from datetime import datetime

from lib.build_config import components_version, module_definitions
from lib.tasks import (
    craft_new_task_id,
    DOCKER_IMAGE_ROUTE_HASH_PATTERN,
    schedule_task_graph,
    TaskBuilder,
    TASKCLUSTER_DATE_ISO_FORMAT,
)
from lib.util import (
    populate_chain_of_trust_task_graph,
    populate_chain_of_trust_required_but_unused_files,
)


REPO_URL = os.environ.get('MOBILE_HEAD_REPOSITORY')
COMMIT = os.environ.get('MOBILE_HEAD_REV')
PR_TITLE = os.environ.get('GITHUB_PULL_TITLE', '')
TRUST_LEVEL = os.environ.get('TRUST_LEVEL', 1)

PROJECT_FOLDER = os.path.abspath(os.path.join(os.path.abspath(__file__), '..', '..', '..'))
DOCKER_FOLDER = os.path.join(PROJECT_FOLDER, 'automation', 'docker')

# If we see this text inside a pull request title then we will not execute any tasks for this PR.
SKIP_TASKS_TRIGGER = '[ci skip]'


BUILDER = TaskBuilder(
    beetmover_worker_type=os.environ.get('BEETMOVER_WORKER_TYPE'),
    branch=os.environ.get('MOBILE_HEAD_BRANCH'),
    commit=COMMIT,
    owner="skaspari@mozilla.com",
    push_date_time=os.environ.get('MOBILE_PUSH_DATE_TIME'),
    repo_url=os.environ.get('MOBILE_HEAD_REPOSITORY'),
    scheduler_id=os.environ.get('SCHEDULER_ID', 'taskcluster-github'),
    source='{}/raw/{}/.taskcluster.yml'.format(REPO_URL, COMMIT),
    task_id=os.environ.get('TASK_ID'),
    tasks_priority=os.environ.get('TASKS_PRIORITY'),
    trust_level=TRUST_LEVEL,
)


def create_module_tasks(module, build_docker_image_task_id):
    def gradle_module_task_name(module, gradle_task_name):
        return "%s:%s" % (module, gradle_task_name)

    def craft_definition(module, variant, run_tests=True, lint_task=None):
        module_task = gradle_module_task_name(module, "assemble%s" % variant)
        subtitle = "assemble"

        if run_tests:
            module_task = "%s %s" % (module_task, gradle_module_task_name(module, "test%sDebugUnitTest" % variant))
            subtitle = "%sAndTest" % subtitle

        scheduled_lint = False
        if lint_task and variant in lint_task:
            module_task = "%s %s" % (module_task, gradle_module_task_name(module, lint_task))
            subtitle = "%sAndLintDebug" % subtitle
            scheduled_lint = True

        return BUILDER.craft_build_task(
            module_name=module,
            gradle_tasks=module_task,
            subtitle=subtitle + variant,
            run_coverage=True,
            build_docker_image_task_id=build_docker_image_task_id,
        ), scheduled_lint

    # Takes list of variants and produces taskcluter task definitions.
    # Schedules a specified lint task at most once.
    def variants_to_definitions(variants, run_tests=True, lint_task=None):
        scheduled_lint = False
        definitions = []
        for variant in variants:
            task_definition, definition_has_lint = craft_definition(
                module, variant, run_tests=run_tests, lint_task=lint_task)
            if definition_has_lint:
                scheduled_lint = True
            lint_task = lint_task if lint_task and not definition_has_lint else None
            definitions.append(task_definition)
        return definitions, scheduled_lint

    # Module settings that describe which task definitions will be created.
    # Absence of a module override means that default definition pattern will be used for the module.
    module_overrides = {
        ":samples-browser": {
            "assembleOnly/Test": (
                ["ServoArm", "ServoX86", "SystemUniversal"],
                [
                    "GeckoBetaAarch64", "GeckoBetaArm", "GeckoBetaX86",
                    "GeckoNightlyAarch64", "GeckoNightlyArm", "GeckoNightlyX86",
                    "GeckoReleaseAarch64", "GeckoReleaseArm", "GeckoReleaseX86"
                ]
            ),
            "lintTask": "lintGeckoBetaArmDebug"
        },
        ":support-test": {
            "lintTask": "lint"
        },
        ":tooling-lint": {
            "lintTask": "lint"
        }
    }

    override = module_overrides.get(module, {})
    task_definitions = []

    # If assemble/test variant lists are specified, create individual tasks as appropriate,
    # while also checking if we can sneak in a specified lint task along with a matching assemble
    # or test variant. If lint task isn't specified, it's created separately.
    if "assembleOnly/Test" in override:
        assemble_only, assemble_and_test = override["assembleOnly/Test"]

        # As an optimization, lint task will appear at most once in our final list of definitions.
        lint_task = override.get("lintTask", "lintRelease")

        # Assemble-only definitions with an optional lint task (if it matches variants).
        assemble_only_definitions, scheduled_lint_with_assemble = variants_to_definitions(
            assemble_only, run_tests=False, lint_task=lint_task)
        # Assemble-and-test definitions with an optional lint task if it wasn't present
        # above and matches variants.
        assemble_and_test_definitions, scheduled_lint_with_test = variants_to_definitions(
            assemble_and_test, run_tests=True, lint_task=lint_task if lint_task and not scheduled_lint_with_assemble else None)
        # Combine two lists of task definitions.
        task_definitions += assemble_only_definitions + assemble_and_test_definitions

        # If neither list has a lint task within it, append a separate lint task.
        # Overhead of a separate task just for linting is quite significant, but this should be a rare case.
        if not scheduled_lint_with_assemble and not scheduled_lint_with_test:
            task_definitions.append(
                BUILDER.craft_build_task(
                    module_name=module,
                    gradle_tasks=gradle_module_task_name(module, lint_task),
                    subtitle="onlyLintRelease",
                    run_coverage=True,
                    build_docker_image_task_id=build_docker_image_task_id,
                )
            )

    # If only the lint task override is specified, 'assemble' and 'test' are left as-is.
    elif "lintTask" in override:
        gradle_tasks = " ".join([
            gradle_module_task_name(module, gradle_task)
            for gradle_task in ['assemble', 'test', override["lintTask"]]
        ])
        task_definitions.append(
            BUILDER.craft_build_task(
                module_name=module,
                gradle_tasks=gradle_tasks,
                subtitle="assembleAndTestAndCustomLintAll",
                run_coverage=True,
                build_docker_image_task_id=build_docker_image_task_id,
            )
        )

    # Default module task configuration is a single gradle task which runs assemble, test and lint for every
    # variant configured for the given module.
    else:
        gradle_tasks = " ".join([
            gradle_module_task_name(module, gradle_task)
            for gradle_task in ['assemble', 'test', 'lintRelease']
        ])
        task_definitions.append(
            BUILDER.craft_build_task(
                module_name=module,
                gradle_tasks=gradle_tasks,
                subtitle="assembleAndTestAndLintReleaseAll",
                run_coverage=True,
                build_docker_image_task_id=build_docker_image_task_id,
            )
        )

    return task_definitions


def pr_or_push(artifacts_info):
    if SKIP_TASKS_TRIGGER in PR_TITLE:
        print("Pull request title contains", SKIP_TASKS_TRIGGER)
        print("Exit")
        exit(0)

    modules = [_get_gradle_module_name(artifact_info) for artifact_info in artifacts_info]

    docker_images_tasks, build_docker_image_task_id = create_docker_tasks_if_needed()
    build_tasks = {}
    other_tasks = {}

    for module in modules:
        tasks = create_module_tasks(module, build_docker_image_task_id)
        for task in tasks:
            build_tasks[craft_new_task_id()] = task

    for craft_function in (BUILDER.craft_detekt_task, BUILDER.craft_ktlint_task, BUILDER.craft_compare_locales_task):
        other_tasks[craft_new_task_id()] = craft_function(build_docker_image_task_id)

    return (docker_images_tasks, build_tasks, other_tasks)


def _get_gradle_module_name(artifact_info):
    return ':{}'.format(artifact_info['name'])


def _get_release_gradle_tasks(module_name, is_snapshot):
    gradle_tasks = ' '.join(
        '{}:{}{}'.format(
            module_name,
            gradle_task,
            '' if is_snapshot else 'Release'
        )
        for gradle_task in ('assemble', 'test', 'lint')
    )

    return gradle_tasks + ' uploadArchives zipMavenArtifacts'


def release(artifacts_info, version, is_snapshot, is_staging):
    version = components_version() if version is None else version

    docker_images_tasks, build_docker_image_task_id = create_docker_tasks_if_needed(is_release=True)
    build_tasks = {}
    wait_on_builds_tasks = {}
    beetmover_tasks = {}
    other_tasks = {}
    wait_on_builds_task_id = craft_new_task_id()

    for artifact_info in artifacts_info:
        build_task_id = craft_new_task_id()
        module_name = _get_gradle_module_name(artifact_info)
        build_tasks[build_task_id] = BUILDER.craft_build_task(
            module_name=module_name,
            gradle_tasks=_get_release_gradle_tasks(module_name, is_snapshot),
            subtitle='({}{})'.format(version, '-SNAPSHOT' if is_snapshot else ''),
            run_coverage=False,
            is_snapshot=is_snapshot,
            artifact_info=artifact_info,
            build_docker_image_task_id=build_docker_image_task_id,
        )

        beetmover_tasks[craft_new_task_id()] = BUILDER.craft_beetmover_task(
            build_task_id, wait_on_builds_task_id, version, artifact_info['artifact'],
            artifact_info['name'], is_snapshot, is_staging
        )

    wait_on_builds_tasks[wait_on_builds_task_id] = BUILDER.craft_wait_on_builds_task(
        build_tasks.keys(), build_docker_image_task_id
    )

    if is_snapshot:     # XXX These jobs perma-fail on release
        for craft_function in (BUILDER.craft_detekt_task, BUILDER.craft_ktlint_task, BUILDER.craft_compare_locales_task):
            other_tasks[craft_new_task_id()] = craft_function(build_docker_image_task_id)

    return (docker_images_tasks, build_tasks, wait_on_builds_tasks, beetmover_tasks, other_tasks)


def create_docker_tasks_if_needed(is_release=False):
    tasks = {}
    docker_folders = [
        os.path.join(DOCKER_FOLDER, f)
        for f in os.listdir(DOCKER_FOLDER)
        if os.path.isdir(os.path.join(DOCKER_FOLDER, f))
    ]

    for folder in docker_folders:
        folder_name = os.path.basename(folder)
        folder_hash = dirhash(folder, 'sha256')
        try:
            task_id = get_existing_task_in_index(TRUST_LEVEL, folder_name, folder_hash)
            # This allows to automatically and regularly rebuild docker images.
            # We don't want this on releases because, in the context of chemspills, time is
            # important
            if not is_release and is_task_too_old(task_id):
                print(
                    'Docker image "{}" has already a task "{}" for hash "{}". However, it is'
                    'too old. Scheduling a new one...'.format(
                        folder_name, task_id, folder_hash
                    )
                )
                raise ValueError()
        except ValueError:
            task_id = craft_new_task_id()
            tasks[task_id] = BUILDER.craft_docker_image_task(
                image_name=folder_name, folder_hash=folder_hash
            )

        if folder_name == 'build':
            build_docker_image_task_id = task_id


    return tasks, build_docker_image_task_id


# TODO retry this function
def get_existing_task_in_index(trust_level, folder_name, folder_hash):
    index = taskcluster.Index()

    route = DOCKER_IMAGE_ROUTE_HASH_PATTERN.format(
        trust_level=trust_level, image_name=folder_name, hash=folder_hash
    )

    route = route.lstrip('index.')

    try:
        return index.findTask(route)['taskId']
    except taskcluster.exceptions.TaskclusterRestFailure as e:
        if e.status_code == 404:
            raise ValueError
        else:
            raise


# TODO retry this function
def is_task_too_old(task_id):
    queue = taskcluster.Queue()
    task_definition = queue.task(task_id)
    task_created = datetime.strptime(task_definition['created'], TASKCLUSTER_DATE_ISO_FORMAT)
    time_since_task_was_created = datetime.utcnow() - task_created
    # We want to rebuild the docker image roughly once a month, at least:
    return time_since_task_was_created.days > 30


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Creates and submit a graph of tasks on Taskcluster.')

    subparsers = parser.add_subparsers(dest='command')

    subparsers.add_parser('pr-or-push')
    release_parser = subparsers.add_parser('release')

    release_parser.add_argument(
        '--version', action='store', help='git tag to build from',
        default=None
    )
    release_parser.add_argument(
        '--snapshot', action='store_true', dest='is_snapshot',
        help='Create snapshot artifacts and upload them onto the snapshot repository'
    )
    release_parser.add_argument(
        '--staging', action='store_true', dest='is_staging',
        help='Perform a staging build. Artifacts are uploaded on either '
             'https://maven-default.stage.mozaws.net/ or https://maven-snapshots.stage.mozaws.net/'
    )

    result = parser.parse_args()

    command = result.command

    artifacts_info = module_definitions()
    if command == 'release':
        artifacts_info = [info for info in artifacts_info if info['shouldPublish']]

    if len(artifacts_info) == 0:
        print("Could not get module names from gradle")
        sys.exit(2)

    if command == 'pr-or-push':
        ordered_groups_of_tasks = pr_or_push(artifacts_info)
    elif command == 'release':
        ordered_groups_of_tasks = release(
            artifacts_info, result.version, result.is_snapshot, result.is_staging
        )
    else:
        raise Exception('Unsupported command "{}"'.format(command))

    full_task_graph = schedule_task_graph(ordered_groups_of_tasks)

    populate_chain_of_trust_task_graph(full_task_graph)
    populate_chain_of_trust_required_but_unused_files()
