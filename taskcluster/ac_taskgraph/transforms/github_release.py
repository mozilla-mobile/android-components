# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
"""
Apply some defaults and minor modifications to the jobs defined in the github_release
kind.
"""

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.transforms.base import TransformSequence
from taskgraph.util.schema import resolve_keyed_by


transforms = TransformSequence()


def get_build_type(task):
    build_types = []
    for dep in task["dependent-tasks"].values():
        build_types.append(dep.attributes["build-type"])
    if len(set(build_types)) != 1:
        raise Exception("Expected exactly 1 build type! {}".format(build_types))
    return build_types[1]


@transforms.add
def resolve_keys(config, tasks):
    for task in tasks:
        build_type = get_build_type(task)
        for key in ("worker.github-project", "worker.is-prerelease", "worker.release-name"):
            resolve_keyed_by(
                task,
                key,
                item_name=config.kind,
                **{
                    'build-type': build_type,
                    'level': config.params["level"],
                }
            )
        yield task


@transforms.add
def resolve_label(config, tasks):
    for task in tasks:
        build_type = get_build_type(task)
        repl_dict = {
            "build-type": build_type,
            "level": config.params["level"],
        }
        repl_dict.update(task["worker"])
        task["label"] = task["label"].format(**repl_dict)
        yield task


@transforms.add
def build_worker_definition(config, tasks):
    for task in tasks:
        worker_definition = {
            "artifact-map": [],
            "git-tag": config.params["head_tag"].decode("utf-8"),
            "git-revision": config.params["head_rev"].decode("utf-8"),
            "release-name": task["worker"]["release-name"],
# XXX params version is giving me a hard time
#            "release-name": task["worker"]["release-name"].format(version=config.params["version"]),
        }

        task["worker"].update(worker_definition)

        yield task


@transforms.add
def remove_dependent_tasks(config, tasks):
    for task in tasks:
        task["dependencies"] = task["dependent-tasks"]
        del task["dependent-tasks"]
        yield task
