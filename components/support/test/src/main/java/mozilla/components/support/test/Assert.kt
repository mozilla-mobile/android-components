/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:JvmName("AssertUtils")

package mozilla.components.support.test

/**
 * Throw assertion error.
 *
 * @param message optional message for error
 */
@JvmOverloads
fun fail(message: String? = null): Nothing {
    throw if (message != null) {
        AssertionError(message)
    } else {
        AssertionError()
    }
}
