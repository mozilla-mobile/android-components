/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.ext

import mozilla.components.browser.session.Session
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.state.SecurityInfoState
import mozilla.components.browser.state.state.TabSessionState

/**
 * Create a matching [TabSessionState] from a [Session].
 */
fun Session.toTabSessionState(): TabSessionState {
    return TabSessionState(id, toContentState())
}

/**
 * Creates a matching [CustomTabSessionState] from a [Session]
 */
fun Session.toCustomTabSessionState(): CustomTabSessionState {
    return CustomTabSessionState(id, toContentState())
}

private fun Session.toContentState(): ContentState {
    return ContentState(
        url,
        private,
        title,
        progress,
        loading,
        searchTerms,
        securityInfo.toSecurityInfoState()
    )
}

private fun Session.SecurityInfo.toSecurityInfoState(): SecurityInfoState {
    return SecurityInfoState(secure, host, issuer)
}
