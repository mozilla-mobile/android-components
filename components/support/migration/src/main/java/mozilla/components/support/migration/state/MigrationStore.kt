/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.migration.state

import mozilla.components.lib.state.Store

/**
 * [Store] keeping track of the current [MigrationState].
 */
class MigrationStore : Store<MigrationState, MigrationAction>(
    MigrationState(),
    MigrationReducer::reduce
)
