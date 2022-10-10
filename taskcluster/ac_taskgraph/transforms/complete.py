# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.


from copy import deepcopy
import re

from taskgraph.transforms.base import TransformSequence


transforms = TransformSequence()


@transforms.add
def fill_dependencies(config, tasks):
    for task in tasks:
        dependencies = (f'<{dep}>' for dep in task['dependencies'].keys())
        task['run']['command']['task-reference'] = task['run']['command']['task-reference'].format(
            dependencies=' '.join(dependencies)
        )

        yield task
