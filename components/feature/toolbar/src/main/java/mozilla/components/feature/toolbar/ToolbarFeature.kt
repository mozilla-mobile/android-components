/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.toolbar

import android.content.Intent
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.ViewContainer

/**
 * A function representing the search use case, accepting
 * the search terms as string.
 */
typealias SearchUseCase = (String) -> Unit

/**
 * Feature implementation for connecting a toolbar implementation with the session module.
 */
class ToolbarFeature(
    private val toolbar: Toolbar,
    private val fragment: Fragment,
    store: BrowserStore,
    loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
    searchUseCase: SearchUseCase? = null,
    customTabId: String? = null,
    urlRenderConfiguration: UrlRenderConfiguration? = null
) : LifecycleAwareFeature, UserInteractionHandler {
    private val presenter = ToolbarPresenter(toolbar, store, customTabId, urlRenderConfiguration)
    private val interactor = ToolbarInteractor(toolbar, loadUrlUseCase, searchUseCase)

    /**
     * Start feature: App is in the foreground.
     */
    override fun start() {
        toolbar.container = ViewContainer.Fragment(fragment)
        interactor.start()
        presenter.start()
    }

    /**
     * Handler for back pressed events in activities that use this feature.
     *
     * @return true if the event was handled, otherwise false.
     */
    override fun onBackPressed(): Boolean = toolbar.onBackPressed()

    /**
     * Stop feature: App is in the background.
     */
    override fun stop() {
        presenter.stop()
        toolbar.onStop()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        toolbar.onActivityResult(requestCode, resultCode, intent)
    }

    /**
     * Configuration that controls how URLs are rendered.
     *
     * @property publicSuffixList A shared/global [PublicSuffixList] object required to extract certain domain parts.
     * @property registrableDomainColor Text color that should be used for the registrable domain of the URL (see
     * [PublicSuffixList.getPublicSuffixPlusOne] for an explanation of "registrable domain".
     * @property urlColor Optional text color used for the URL.
     * @property renderStyle Sealed class that controls the style of the url to be displayed
     */
    data class UrlRenderConfiguration(
        internal val publicSuffixList: PublicSuffixList,
        @ColorInt internal val registrableDomainColor: Int,
        @ColorInt internal val urlColor: Int? = null,
        internal val renderStyle: RenderStyle = RenderStyle.ColoredUrl
    )

    /**
     * Controls how the url should be styled
     *
     * RegistrableDomain: displays only the url, uncolored
     * ColoredUrl: displays the registrableDomain with color and url with another color
     * UncoloredUrl: displays the full url, uncolored
     */
    sealed class RenderStyle {
        object RegistrableDomain : RenderStyle()
        object ColoredUrl : RenderStyle()
        object UncoloredUrl : RenderStyle()
    }
}
