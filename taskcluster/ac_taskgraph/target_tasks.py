# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.target_tasks import _target_task, filter_for_tasks_for


@_target_task("nightly")
def target_tasks_nightly(full_task_graph, parameters, graph_config):
    def filter(task, parameters):
        return task.attributes.get("build-type", "") == "nightly"

    return [l for l, t in full_task_graph.tasks.iteritems() if filter(t, parameters)]


@_target_task("release")
def target_tasks_release(full_task_graph, parameters, graph_config):
    def filter(task, parameters):
        return task.attributes.get("build-type", "") == "release"

    return [l for l, t in full_task_graph.tasks.iteritems() if filter(t, parameters)]
