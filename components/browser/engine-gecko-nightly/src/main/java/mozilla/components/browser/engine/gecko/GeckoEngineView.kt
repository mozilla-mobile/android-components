/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.permission.PermissionRequest
import org.mozilla.geckoview.GeckoResult

/**
 * Gecko-based EngineView implementation.
 */
class GeckoEngineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), EngineView {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var currentGeckoView = object : NestedGeckoView(context) {
        override fun onDetachedFromWindow() {
            // We are releasing the session before GeckoView gets detached from the window. Otherwise
            // GeckoView will close the session automatically and we do not want that.
            releaseSession()

            super.onDetachedFromWindow()
        }
    }.apply {
        // Explicitly mark this view as important for autofill. The default "auto" doesn't seem to trigger any
        // autofill behavior for us here.
        @Suppress("WrongConstant")
        ViewCompat.setImportantForAutofill(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val observer = object : EngineSession.Observer {
        override fun onCrashStateChange(crashed: Boolean) {
            if (crashed) {
                // When crashing the previous GeckoSession is no longer usable. Internally GeckoEngineSession will
                // create a new instance. This means we will need to tell GeckoView about this new GeckoSession:
                currentSession?.let { currentGeckoView.setSession(it.geckoSession) }
            }
        }
        override fun onAppPermissionRequest(permissionRequest: PermissionRequest) = Unit
        override fun onContentPermissionRequest(permissionRequest: PermissionRequest) = Unit
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var currentSession: GeckoEngineSession? = null

    init {
        // Currently this is just a FrameLayout with a single GeckoView instance. Eventually this
        // implementation should handle at least two GeckoView so that we can switch between
        addView(currentGeckoView)
    }

    /**
     * Render the content of the given session.
     */
    @Synchronized
    override fun render(session: EngineSession) {
        val internalSession = session as GeckoEngineSession

        currentSession?.apply { unregister(observer) }

        currentSession = session.apply {
            register(observer)
        }

        if (currentGeckoView.session != internalSession.geckoSession) {
            currentGeckoView.session?.let {
                // Release a previously assigned session. Otherwise GeckoView will close it
                // automatically.
                currentGeckoView.releaseSession()
            }

            currentGeckoView.setSession(internalSession.geckoSession)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        currentSession?.apply { unregister(observer) }
    }

    override fun canScrollVerticallyUp(): Boolean {
        val result = currentSession?.let { it.scrollY > 0 } != false
        return result
    }

    override fun canScrollVerticallyDown() = true // waiting for this issue https://bugzilla.mozilla.org/show_bug.cgi?id=1507569

    override fun setVerticalClipping(clippingHeight: Int) {
        currentGeckoView.setVerticalClipping(clippingHeight)
    }

    override fun captureThumbnail(onFinish: (Bitmap?) -> Unit) {
        val geckoResult = currentGeckoView.capturePixels()
        geckoResult.then({ bitmap ->
            onFinish(bitmap)
            GeckoResult<Void>()
        }, {
            onFinish(null)
            GeckoResult<Void>()
        })
    }
}
