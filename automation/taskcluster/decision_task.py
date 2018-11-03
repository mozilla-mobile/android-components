# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
Decision task for pull requests and pushes
"""

import datetime
import os
import taskcluster
import re
import subprocess
import sys

import lib.tasks


TASK_ID = os.environ.get('TASK_ID')
REPO_URL = os.environ.get('MOBILE_HEAD_REPOSITORY')
BRANCH = os.environ.get('MOBILE_HEAD_BRANCH')
COMMIT = os.environ.get('MOBILE_HEAD_REV')
PR_TITLE = os.environ.get('GITHUB_PULL_TITLE', '')

# If we see this text inside a pull request title then we will not execute any tasks for this PR.
SKIP_TASKS_TRIGGER = '[ci skip]'


def fetch_module_names():
    process = subprocess.Popen(["./gradlew", "--no-daemon", "printModules"], stdout=subprocess.PIPE)
    (output, err) = process.communicate()
    exit_code = process.wait()

    if exit_code is not 0:
        print "Gradle command returned error:", exit_code

    return re.findall('module: (.*)', output, re.M)


def create_task(name, description, command, scopes = []):
    return create_raw_task(name, description, "./gradlew --no-daemon clean %s" % command, scopes)

def create_raw_task(name, description, full_command, scopes = []):
    created = datetime.datetime.now()
    expires = taskcluster.fromNow('1 year')
    deadline = taskcluster.fromNow('1 day')

    return {
        "workerType": 'github-worker',
        "taskGroupId": TASK_ID,
        "expires": taskcluster.stringDate(expires),
        "retries": 5,
        "created": taskcluster.stringDate(created),
        "tags": {},
        "priority": "lowest",
        "schedulerId": "taskcluster-github",
        "deadline": taskcluster.stringDate(deadline),
        "dependencies": [ TASK_ID ],
        "routes": [],
        "scopes": scopes,
        "requires": "all-completed",
        "payload": {
            "features": {
                'taskclusterProxy': True
            },
            "maxRunTime": 7200,
            "image": "mozillamobile/android-components:1.8",
            "command": [
                "/bin/bash",
                "--login",
                "-cx",
                "export TERM=dumb && git fetch %s %s && git config advice.detachedHead false && git checkout %s && %s" % (REPO_URL, BRANCH, COMMIT, full_command)
            ],
            "artifacts": {},
            "env": {
                "TASK_GROUP_ID": TASK_ID
            }
        },
        "provisionerId": "aws-provisioner-v1",
        "metadata": {
            "name": name,
            "description": description,
            "owner": "skaspari@mozilla.com",
            "source": "https://github.com/mozilla-mobile/android-components"
        }
    }


def create_module_task(module):
    return create_task(
        name='Android Components - Module ' + module,
        description='Building and testing module ' + module,
        command="-Pcoverage " + " ".join(map(lambda x: module + ":" + x, ['assemble', 'test', 'lint']))  +
            " && automation/taskcluster/action/upload_coverage_report.sh",
        scopes = [
            "secrets:get:project/mobile/android-components/public-tokens"
        ])


def create_detekt_task():
    return create_task(
        name='Android Components - detekt',
        description='Running detekt over all modules',
        command='detekt')


def create_ktlint_task():
    return create_task(
        name='Android Components - ktlint',
        description='Running ktlint over all modules',
        command='ktlint')


def create_compare_locales_task():
    return create_raw_task(
        name='Android Components - compare-locales',
        description='Validate strings.xml with compare-locales',
        full_command='pip install "compare-locales>=4.0.1,<5.0" && compare-locales --validate l10n.toml .')


if __name__ == "__main__":
    if SKIP_TASKS_TRIGGER in PR_TITLE:
        print "Pull request title contains", SKIP_TASKS_TRIGGER
        print "Exit"
        exit(0)

    queue = taskcluster.Queue({ 'baseUrl': 'http://taskcluster/queue/v1' })

    modules = fetch_module_names()

    if len(modules) == 0:
        print "Could not get module names from gradle"
        sys.exit(2)

    for module in modules:
        task = create_module_task(module)
        task_id = taskcluster.slugId()
        lib.tasks.schedule_task(queue, task_id, task)

    lib.tasks.schedule_task(queue, taskcluster.slugId(), create_detekt_task())
    lib.tasks.schedule_task(queue, taskcluster.slugId(), create_ktlint_task())
    lib.tasks.schedule_task(queue, taskcluster.slugId(), create_compare_locales_task())
