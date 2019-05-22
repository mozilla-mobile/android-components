/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.awesomebar

import android.graphics.Bitmap
import android.view.View
import java.util.UUID

/**
 * Interface to be implemented by awesome bar implementations.
 *
 * An awesome bar has multiple duties:
 *  - Display [Suggestion] instances and invoking its callbacks once selected
 *  - React to outside events: [onInputStarted], [onInputChanged], [onInputCancelled].
 *  - Query [SuggestionProvider] instances for new suggestions when the text changes.
 */
interface AwesomeBar {

    /**
     * Adds the following [SuggestionProvider] instances to be queried for [Suggestion]s whenever the text changes.
     */
    fun addProviders(vararg providers: SuggestionProvider)

    /**
     * Removes the following [SuggestionProvider]
     */
    fun removeProviders(vararg providers: SuggestionProvider)

    /**
     * Removes all [SuggestionProvider]s
     */
    fun removeAllProviders()

    /**
     * Fired when the user starts interacting with the awesome bar by entering text in the toolbar.
     */
    fun onInputStarted() = Unit

    /**
     * Fired whenever the user changes their input, after they have started interacting with the awesome bar.
     *
     * @param text The current user input in the toolbar.
     */
    fun onInputChanged(text: String)

    /**
     * Fired when the user has cancelled their interaction with the awesome bar.
     */
    fun onInputCancelled() = Unit

    /**
     * Casts this awesome bar to an Android View object.
     */
    fun asView(): View = this as View

    /**
     * Adds a lambda to be invoked when the user has finished interacting with the awesome bar (e.g. selected a
     * suggestion).
     */
    fun setOnStopListener(listener: () -> Unit)

    /**
     * A [Suggestion] to be displayed by an [AwesomeBar] implementation.
     *
     * @property provider The provider this suggestion came from.
     * @property id A unique ID (provider scope) identifying this [Suggestion]. A stable ID but different data indicates
     * to the [AwesomeBar] that this is the same [Suggestion] with new data. This will affect how the [AwesomeBar]
     * animates showing the new suggestion.
     * @property title A user-readable title for the [Suggestion].
     * @property description A user-readable description for the [Suggestion].
     * @property icon A lambda that can be invoked by the [AwesomeBar] implementation to receive an icon [Bitmap] for
     * this [Suggestion]. The [AwesomeBar] will pass in its desired width and height for the Bitmap.
     * @property chips A list of [Chip] instances to be displayed.
     * @property flags A set of [Flag] values for this [Suggestion].
     * @property onSuggestionClicked A callback to be executed when the [Suggestion] was clicked by the user.
     * @property onChipClicked A callback to be executed when a [Chip] was clicked by the user.
     * @property score A score used to rank suggestions of this provider against each other. A suggestion with a higher
     * score will be shown on top of suggestions with a lower score.
     */
    data class Suggestion(
        val provider: SuggestionProvider,
        val id: String = UUID.randomUUID().toString(),
        val title: String? = null,
        val description: String? = null,
        val icon: (suspend (width: Int, height: Int) -> Bitmap?)? = null,
        val chips: List<Chip> = emptyList(),
        val flags: Set<Flag> = emptySet(),
        val onSuggestionClicked: (() -> Unit)? = null,
        val onChipClicked: ((Chip) -> Unit)? = null,
        val score: Int = 0
    ) {
        /**
         * Chips are compact actions that are shown as part of a suggestion. For example a [Suggestion] from a search
         * engine may offer multiple search suggestion chips for different search terms.
         */
        data class Chip(
            val title: String
        )

        /**
         * Flags can be added by a [SuggestionProvider] to help the [AwesomeBar] implementation decide how to display
         * a specific [Suggestion]. For example an [AwesomeBar] could display a bookmark star icon next to [Suggestion]s
         * that contain the [BOOKMARK] flag.
         */
        enum class Flag {
            BOOKMARK,
            OPEN_TAB,
            CLIPBOARD
        }

        /**
         * Returns true if the content of the two suggestions is the same.
         *
         * This is used by [AwesomeBar] implementations to decide whether an updated suggestion (same id) needs its
         * view to be updated in order to display new data.
         */
        fun areContentsTheSame(other: Suggestion): Boolean {
            return title == other.title &&
                description == other.description &&
                chips == other.chips &&
                flags == other.flags
        }
    }

    /**
     * A [SuggestionProvider] is queried by an [AwesomeBar] whenever the text in the address bar is changed by the user.
     * It returns a list of [Suggestion]s to be displayed by the [AwesomeBar].
     */
    interface SuggestionProvider {
        /**
         * A unique ID used for identifying this provider.
         *
         * The recommended approach for a [SuggestionProvider] implementation is to generate a UUID.
         */
        val id: String

        /**
         * Fired when the user starts interacting with the awesome bar by entering text in the toolbar.
         */
        fun onInputStarted() = Unit

        /**
         * Fired whenever the user changes their input, after they have started interacting with the awesome bar.
         *
         * This is a suspending function. An [AwesomeBar] implementation is expected to invoke this method from a
         * [Coroutine](https://kotlinlang.org/docs/reference/coroutines-overview.html). This allows the [AwesomeBar]
         * implementation to group and cancel calls to multiple providers.
         *
         * Coroutine cancellation is cooperative. A coroutine code has to cooperate to be cancellable:
         * https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/cancellation-and-timeouts.md
         *
         * @param text The current user input in the toolbar.
         * @return A list of suggestions to be displayed by the [AwesomeBar].
         */
        suspend fun onInputChanged(text: String): List<Suggestion>

        /**
         * Fired when the user has cancelled their interaction with the awesome bar.
         */
        fun onInputCancelled() = Unit

        /**
         * If true an [AwesomeBar] implementation can clear the previous suggestions of this provider as soon as the
         * user continues to type. If this is false an [AwesomeBar] implementation is allowed to keep the previous
         * suggestions around until the provider returns a new list of suggestions for the updated text.
         */
        val shouldClearSuggestions
            get() = true
    }
}
