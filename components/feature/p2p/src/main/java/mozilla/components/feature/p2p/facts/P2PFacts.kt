/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p.facts

import mozilla.components.support.base.Component
import mozilla.components.support.base.facts.Action
import mozilla.components.support.base.facts.Fact
import mozilla.components.support.base.facts.collect

/**
 * Facts emitted for telemetry related to [P2PFeature]
 */
class P2PFacts {
    /**
     * Items that specify which portion of the [P2PFeature] was interacted with
     */
    object Items {
        const val PREVIOUS = "previous"
        const val NEXT = "next"
        const val CLOSE = "close"
        const val INPUT = "input"
    }
}

private fun emitP2PFact(
    action: Action,
    item: String,
    value: String? = null,
    metadata: Map<String, Any>? = null
) {
    Fact(
        Component.FEATURE_P2P,
        action,
        item,
        value,
        metadata
    ).collect()
}

internal fun emitCloseFact() = emitP2PFact(Action.CLICK, P2PFacts.Items.CLOSE)
internal fun emitNextFact() = emitP2PFact(Action.CLICK, P2PFacts.Items.NEXT)
internal fun emitPreviousFact() = emitP2PFact(Action.CLICK, P2PFacts.Items.PREVIOUS)
internal fun emitCommitFact(value: String) =
        emitP2PFact(Action.COMMIT, P2PFacts.Items.INPUT, value)
