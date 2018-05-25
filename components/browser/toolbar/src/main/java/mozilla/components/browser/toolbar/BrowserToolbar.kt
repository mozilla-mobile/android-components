/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.browser.toolbar.edit.EditToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.support.ktx.android.view.dp
import mozilla.components.support.ktx.android.view.forEach
import mozilla.components.support.ktx.android.view.isVisible

/**
 * A customizable toolbar for browsers.
 *
 * The toolbar can switch between two modes: display and edit. The display mode displays the current
 * URL and controls for navigation. In edit mode the current URL can be edited. Those two modes are
 * implemented by the DisplayToolbar and EditToolbar classes.
 *
 *           +----------------+
 *           | BrowserToolbar |
 *           +--------+-------+
 *                    +
 *            +-------+-------+
 *            |               |
 *  +---------v------+ +-------v--------+
 *  | DisplayToolbar | |   EditToolbar  |
 *  +----------------+ +----------------+
 *
 */
@Suppress("TooManyFunctions")
class BrowserToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), Toolbar {

    // displayToolbar and editToolbar are only visible internally and mutable so that we can mock
    // them in tests.
    @VisibleForTesting internal var displayToolbar = DisplayToolbar(context, this)
    @VisibleForTesting internal var editToolbar = EditToolbar(context, this)

    /**
     * Set/Get whether a site security icon (usually a lock or globe icon) should be next to the URL.
     */
    var displaySiteSecurityIcon: Boolean
        get() = displayToolbar.iconView.isVisible()
        set(value) {
            displayToolbar.iconView.visibility = if (value) View.VISIBLE else View.GONE
        }

    private var state: State = State.DISPLAY
    private var searchTerms: String = ""
    private var listener: ((String) -> Unit)? = null

    override var url: String = ""
        set(value) {
            // We update the display toolbar immediately. We do not do that for the edit toolbar to not
            // mess with what the user is entering. Instead we will remember the value and update the
            // edit toolbar whenever we switch to it.
            displayToolbar.updateUrl(value)

            field = value
        }


    init {
        addView(displayToolbar)
        addView(editToolbar)

        updateState(State.DISPLAY)
    }

    // We layout the toolbar ourselves to avoid the overhead from using complex ViewGroup implementations
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        forEach { child ->
            child.layout(
                    left + paddingLeft,
                    top + paddingTop,
                    right - paddingRight,
                    bottom - paddingBottom)
        }
    }

    // We measure the views manually to avoid overhead by using complex ViewGroup implementations
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Our toolbar will always use the full width and a fixed height (default) or the provided
        // height if it's an exact value.
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(heightMeasureSpec)
        } else {
            dp(DEFAULT_TOOLBAR_HEIGHT_DP)
        }

        setMeasuredDimension(width, height)

        // Let the children measure themselves using our fixed size (with padding substraced)
        val childWidth = width - paddingLeft - paddingRight
        val childHeight = height - paddingTop - paddingBottom

        val childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)

        forEach { child -> child.measure(childWidthSpec, childHeightSpec) }
    }

    override fun onBackPressed(): Boolean {
        if (state == State.EDIT) {
            displayMode()
            return true
        }
        return false
    }

    override fun setSearchTerms(searchTerms: String) {
        this.searchTerms = searchTerms
    }

    override fun displayProgress(progress: Int) {
        displayToolbar.updateProgress(progress)
    }

    override fun setOnUrlChangeListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    /**
     * Adds an action to be displayed on the right side of the toolbar (outside of the URL bounding
     * box) in display mode.
     *
     * If there is not enough room to show all icons then some icons may be moved to an overflow
     * menu.
     *
     * Related:
     * https://developer.mozilla.org/en-US/Add-ons/WebExtensions/user_interface/Browser_action
     */
    override fun addBrowserAction(action: Toolbar.Action) {
        displayToolbar.addBrowserAction(action)
    }

    /**
     * Adds an action to be displayed on the right side of the URL in display mode.
     *
     * Related:
     * https://developer.mozilla.org/en-US/Add-ons/WebExtensions/user_interface/Page_actions
     */
    override fun addPageAction(action: Toolbar.Action) {
        displayToolbar.addPageAction(action)
    }

    /**
     * Adds an action to be display on the far left side of the toolbar. This area is usually used
     * on larger devices for navigation actions like "back" and "forward".
     */
    override fun addNavigationAction(action: Toolbar.Action) {
        displayToolbar.addNavigationAction(action)
    }

    /**
     * Switches to URL editing mode.
     */
    fun editMode() {
        val urlValue = if (searchTerms.isEmpty()) url else searchTerms
        editToolbar.updateUrl(urlValue)

        updateState(State.EDIT)

        editToolbar.focus()
    }

    /**
     * Switches to URL displaying mode.
     */
    fun displayMode() {
        updateState(State.DISPLAY)
    }

    /**
     * Sets a BrowserMenuBuilder that will be used to create a menu when the menu button is clicked.
     * The menu button will only be visible if a builder has been set.
     */
    fun setMenuBuilder(menuBuilder: BrowserMenuBuilder) {
        displayToolbar.menuBuilder = menuBuilder
    }

    internal fun onUrlEntered(url: String) {
        displayMode()

        listener?.invoke(url)
    }

    private fun updateState(state: State) {
        this.state = state

        val (show, hide) = when (state) {
            State.DISPLAY -> Pair(displayToolbar, editToolbar)
            State.EDIT -> Pair(editToolbar, displayToolbar)
        }

        show.visibility = View.VISIBLE
        hide.visibility = View.GONE
    }

    private enum class State {
        DISPLAY,
        EDIT
    }

    companion object {
        private const val DEFAULT_TOOLBAR_HEIGHT_DP = 56
    }
}
