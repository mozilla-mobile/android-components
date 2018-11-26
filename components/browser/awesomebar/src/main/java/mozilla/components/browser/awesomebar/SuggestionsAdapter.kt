/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.awesomebar

import android.support.annotation.GuardedBy
import android.support.annotation.VisibleForTesting
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import mozilla.components.concept.awesomebar.AwesomeBar
import java.lang.IllegalStateException

/**
 * [RecyclerView.Adapter] for displaying [AwesomeBar.Suggestion] in [BrowserAwesomeBar].
 */
internal class SuggestionsAdapter(
    private val awesomeBar: BrowserAwesomeBar
) : RecyclerView.Adapter<SuggestionViewHolder>() {
    /**
     * List of suggestions to be displayed by this adapter.
     */
    @GuardedBy("suggestions")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var suggestions = listOf<WeightedSuggestion>()

    /**
     * Mapping a provider to the suggestions that it provided. This allows us to remove/update the suggestions of
     * a specific provider after they have been added to the list.
     */
    private val suggestionMap: MutableMap<AwesomeBar.SuggestionProvider, List<WeightedSuggestion>> = mutableMapOf()

    /**
     * Adds the given list of suggestions (from the given provider) to this adapter.
     */
    fun addSuggestions(
        provider: AwesomeBar.SuggestionProvider,
        providerSuggestions: List<AwesomeBar.Suggestion>,
        weight: Double
    ) = synchronized(suggestions) {
        // Start with the current list of suggestions
        val updatedSuggestions = suggestions.toMutableList()

        val (providerMin, providerMax) = provider.scoreRange
        if (providerMin > providerMax) {
            // TODO: Rounding issues?
            throw IllegalStateException("Illegal provider score range: [$providerMin, $providerMax]")
        }

        if (!provider.shouldClearSuggestions) {
            // This provider doesn't want its suggestions to be cleared when the typed text changes. This means now
            // that there are new suggestions we need to remove the previous suggestions of this provider.
            suggestionMap[provider]?.let { updatedSuggestions.removeAll(it) }
        }

        val weightedSuggestions = providerSuggestions.map { suggestion ->
            val score = suggestion.score
            if (score > providerMax || score < providerMin) {
                // TODO: Rounding issues
                throw IllegalStateException("Score $score out of range [$providerMin, $providerMax]")
            }

            val normalizedScore = (score - providerMin) / (providerMax - providerMin)

            WeightedSuggestion(suggestion, normalizedScore * weight)
        }

        // Remember which suggestions this provider added
        suggestionMap[provider] = weightedSuggestions

        updatedSuggestions.addAll(weightedSuggestions)

        updateTo(updatedSuggestions.sortedByDescending { it.weightedNormalizedScore })
    }

    /**
     * Removes all suggestions except the ones from providers that have set shouldClearSuggestions to false.
     */
    fun clearSuggestions() = synchronized(suggestions) {
        val updatedSuggestions = suggestions.toMutableList()

        suggestionMap.keys.forEach { provider ->
            if (provider.shouldClearSuggestions) {
                suggestionMap[provider]?.let { updatedSuggestions.removeAll(it) }
            }
        }

        updateTo(updatedSuggestions)
    }

    /**
     * Takes an updated list of suggestions, calculates the diff and then dispatches the appropriate notifications to
     * update the RecyclerView.
     */
    private fun updateTo(updatedSuggestions: List<WeightedSuggestion>) {
        val result = DiffUtil.calculateDiff(SuggestionDiffCallback(suggestions, updatedSuggestions))

        this.suggestions = updatedSuggestions
        result.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SuggestionViewHolder = synchronized(suggestions) {
        return when (viewType) {
            SuggestionViewHolder.DefaultSuggestionViewHolder.LAYOUT_ID -> {
                val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
                SuggestionViewHolder.DefaultSuggestionViewHolder(awesomeBar, view)
            }

            SuggestionViewHolder.ChipsSuggestionViewHolder.LAYOUT_ID -> {
                val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
                SuggestionViewHolder.ChipsSuggestionViewHolder(awesomeBar, view)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int = synchronized(suggestions) {
        val suggestion = suggestions.get(position).suggestion

        return if (suggestion.chips.isNotEmpty()) {
            SuggestionViewHolder.ChipsSuggestionViewHolder.LAYOUT_ID
        } else {
            SuggestionViewHolder.DefaultSuggestionViewHolder.LAYOUT_ID
        }
    }

    override fun getItemCount(): Int = synchronized(suggestions) {
        return suggestions.size
    }

    override fun onBindViewHolder(
        holder: SuggestionViewHolder,
        position: Int
    ) = synchronized(suggestions) {
        val suggestion = suggestions[position].suggestion
        holder.bind(suggestion)
    }
}

internal data class WeightedSuggestion(
    val suggestion: AwesomeBar.Suggestion,
    val weightedNormalizedScore: Double
)

internal class SuggestionDiffCallback(
    private val suggestions: List<WeightedSuggestion>,
    private val updatedSuggestions: List<WeightedSuggestion>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        suggestions[oldItemPosition].suggestion.id == updatedSuggestions[newItemPosition].suggestion.id

    override fun getOldListSize(): Int = suggestions.size

    override fun getNewListSize(): Int = updatedSuggestions.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        suggestions[oldItemPosition].suggestion.areContentsTheSame(updatedSuggestions[newItemPosition].suggestion)
}
