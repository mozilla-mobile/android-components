/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p.view

import android.view.View
import mozilla.components.browser.state.state.content.FindResultState

/**
 * An interface for views that can display "find in page" results and related UI controls.
 */
interface P2PView {
    /**
     * Listener to be invoked after the user performs certain actions (e.g. "find next result").
     */
    var listener: Listener?

    /**
     * Displays the given [FindResultState] state in the view.
     */
    fun updateStatus(status: String)

    /**
     * Requests focus for the input element the user can type their query into.
     */
    fun focus()

    /**
     * Clears the UI state.
     */
    fun clear()

    fun enable()

    /**
     * Casts this [P2PView] interface to an actual Android [View] object.
     */
    fun asView(): View = (this as View)

    interface Listener {
        fun onAdvertise()
        fun onDiscover()
        fun onClose()
    }
}
