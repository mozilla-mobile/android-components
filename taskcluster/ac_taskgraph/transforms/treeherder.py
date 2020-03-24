# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.transforms.base import TransformSequence
from taskgraph.util.treeherder import inherit_treeherder_from_dep, join_symbol


transforms = TransformSequence()


@transforms.add
def build_treeherder_definition(config, tasks):
    for task in tasks:
        dep = task.pop("primary-dependency")

        task.setdefault("treeherder", {}).update(inherit_treeherder_from_dep(task, dep))
        job_group = dep.task["extra"]["treeherder"].get("groupSymbol", "?")
        job_symbol = task["treeherder"].pop("job-symbol")
        full_symbol = join_symbol(job_group, job_symbol)
        task["treeherder"]["symbol"] = full_symbol

        yield task
