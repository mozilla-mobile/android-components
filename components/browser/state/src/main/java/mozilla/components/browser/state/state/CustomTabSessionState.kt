/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.state

import java.util.UUID

/**
 * Value type that represents the state of a Custom Tab.
 *
 * @property id the ID of this custom tab and session.
 * @property content the [ContentState] of this custom tab.
 */
data class CustomTabSessionState(
    override val id: String = UUID.randomUUID().toString(),
    override val content: ContentState
) : SessionState
