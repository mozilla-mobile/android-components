/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.sync

/**
 * Possible outcomes of a FXA operation.
 */
sealed class FxaOperationResult {
    /**
     * Operation completed successfully.
     */
    object Success : FxaOperationResult()

    /**
     * Operation was unsuccessful.
     *
     * @param reason [FxaException] specific subclass identifying what caused to operation to fail.
     * May contain an optional [String] with more details.
     */
    class Failure(val reason: FxaException) : FxaOperationResult()
}

/**
 * Reasons for why a certain FXA operation failed.
 */
@Suppress("UNUSED_PARAMETER")
sealed class FxaException {
    /**
     * Operation failed because of a network problem.
     * The network issue may be actionable by the user.
     */
    class FxaNetworkException(reason: String?) : FxaException()

    /**
     * Operation failed because of auth issues.
     */
    class FxaUnauthorizedException(reason: String?) : FxaException()

    /**
     * Operation failed for an unknown reason.
     */
    class FxaUnspecifiedException(reason: String?) : FxaException()
}
