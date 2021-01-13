/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.sitepermissions

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.ContentAction.UpdatePermissionHighlightsStateAction
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.content.PermissionHighlightsState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.permission.Permission
import mozilla.components.concept.engine.permission.Permission.ContentAudioCapture
import mozilla.components.concept.engine.permission.Permission.ContentAudioMicrophone
import mozilla.components.concept.engine.permission.Permission.ContentAutoPlayAudible
import mozilla.components.concept.engine.permission.Permission.ContentAutoPlayInaudible
import mozilla.components.concept.engine.permission.Permission.ContentGeoLocation
import mozilla.components.concept.engine.permission.Permission.ContentNotification
import mozilla.components.concept.engine.permission.Permission.ContentVideoCamera
import mozilla.components.concept.engine.permission.Permission.ContentVideoCapture
import mozilla.components.concept.engine.permission.Permission.ContentPersistentStorage
import mozilla.components.concept.engine.permission.Permission.ContentMediaKeySystemAccess
import mozilla.components.concept.engine.permission.Permission.AppLocationCoarse
import mozilla.components.concept.engine.permission.Permission.AppLocationFine
import mozilla.components.concept.engine.permission.Permission.AppAudio
import mozilla.components.concept.engine.permission.Permission.AppCamera
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.feature.sitepermissions.SitePermissions.Status.ALLOWED
import mozilla.components.feature.sitepermissions.SitePermissions.Status.BLOCKED
import mozilla.components.feature.sitepermissions.SitePermissionsFeature.DialogConfig
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.ktx.android.content.isPermissionGranted
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import mozilla.components.support.ktx.kotlinx.coroutines.flow.filterChanged
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import java.security.InvalidParameterException

internal const val FRAGMENT_TAG = "mozac_feature_sitepermissions_prompt_dialog"

/**
 * This feature will collect [PermissionRequest] from [ContentState] and display
 * suitable [SitePermissionsDialogFragment].
 * Once the dialog is closed the [PermissionRequest] will be consumed.
 *
 * @property context a reference to the context.
 * @property sessionId optional sessionId to be observed if null the selected session will be observed.
 * @property storage the object in charge of persisting all the [SitePermissions] objects.
 * @property sitePermissionsRules indicates how permissions should behave per permission category.
 * @property fragmentManager a reference to a [FragmentManager], used to show permissions prompts.
 * @property promptsStyling optional styling for prompts.
 * @property dialogConfig optional customization for dialog initial state. See [DialogConfig].
 * @property onNeedToRequestPermissions a callback invoked when permissions
 * need to be requested. Once the request is completed, [onPermissionsResult] needs to be invoked.
 * @property onShouldShowRequestPermissionRationale a callback that allows the feature to query
 * the ActivityCompat.shouldShowRequestPermissionRationale or the Fragment.shouldShowRequestPermissionRationale values.
 **/

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
class SitePermissionsFeature(
    private val context: Context,
    private var sessionId: String? = null,
    private val storage: SitePermissionsStorage = SitePermissionsStorage(context),
    var sitePermissionsRules: SitePermissionsRules? = null,
    private val fragmentManager: FragmentManager,
    var promptsStyling: PromptsStyling? = null,
    private val dialogConfig: DialogConfig? = null,
    override val onNeedToRequestPermissions: OnNeedToRequestPermissions,
    val onShouldShowRequestPermissionRationale: (permission: String) -> Boolean,
    private val store: BrowserStore
) : LifecycleAwareFeature, PermissionsFeature {

    internal val ioCoroutineScope by lazy { coroutineScopeInitializer() }

    internal var coroutineScopeInitializer = {
        CoroutineScope(Dispatchers.IO)
    }
    private var sitePermissionScope: CoroutineScope? = null
    private var appPermissionScope: CoroutineScope? = null
    private var loadingScope: CoroutineScope? = null

    @ExperimentalCoroutinesApi
    override fun start() {
        fragmentManager.findFragmentByTag(FRAGMENT_TAG)?.let { fragment ->
            // There's still a [SitePermissionsDialogFragment] visible from the last time. Re-attach
            // this feature so that the fragment can invoke the callback on this feature once the user
            // makes a selection. This can happen when the app was in the background and on resume
            // the activity and fragments get recreated.
            reattachFragment(fragment as SitePermissionsDialogFragment)
        }

        setupPermissionRequestsCollector()
        setupAppPermissionRequestsCollector()
        setupLoadingCollector()
    }

    @VisibleForTesting
    internal fun setupLoadingCollector() {
        loadingScope = store.flowScoped { flow ->
            flow.mapNotNull { state ->
                state.findTabOrCustomTabOrSelectedTab(sessionId)
            }.ifChanged { it.content.loading }.collect { tab ->
                if (tab.content.loading) {
                    // Clears stale permission indicators in the toolbar,
                    // after the session starts loading.
                    store.dispatch(UpdatePermissionHighlightsStateAction(tab.id, PermissionHighlightsState()))
                }
            }
        }
    }

    @VisibleForTesting
    internal fun setupAppPermissionRequestsCollector() {
        appPermissionScope =
            store.flowScoped { flow ->
                flow.mapNotNull { state ->
                    state.findTabOrCustomTabOrSelectedTab(sessionId)?.content?.appPermissionRequestsList
                }
                    .filterChanged { it }
                    .collect { appPermissionRequest ->
                        val permissions = appPermissionRequest.permissions.map { it.id ?: "" }
                        onNeedToRequestPermissions(permissions.toTypedArray())
                    }
            }
    }

    @VisibleForTesting
    internal fun setupPermissionRequestsCollector() {
        sitePermissionScope =
            store.flowScoped { flow ->
                flow.mapNotNull { state ->
                    state.findTabOrCustomTabOrSelectedTab(sessionId)?.content?.permissionRequestsList
                }
                    .filterChanged { it }
                    .collect { permissionRequest ->

                        val host =
                            getCurrentContentState()?.url
                                ?: ""

                        if (permissionRequest.permissions.all { it.isSupported() }) {
                            onContentPermissionRequested(
                                permissionRequest,
                                host.tryGetHostFromUrl()
                            )
                        } else {
                            consumePermissionRequest(permissionRequest)
                            permissionRequest.reject()
                        }
                    }
            }
    }

    @VisibleForTesting
    internal fun consumePermissionRequest(
        permissionRequest: PermissionRequest,
        optionalSessionId: String? = null
    ) {
        val thisSessionId = optionalSessionId ?: getCurrentTabState()?.id
        thisSessionId?.let { sessionId ->
            store.dispatch(ContentAction.ConsumePermissionsRequest(sessionId, permissionRequest))
        }
    }

    @VisibleForTesting
    internal fun consumeAppPermissionRequest(
        appPermissionRequest: PermissionRequest,
        optionalSessionId: String? = null
    ) {
        val thisSessionId = optionalSessionId ?: getCurrentTabState()?.id
        thisSessionId?.let { sessionId ->
            store.dispatch(
                ContentAction.ConsumeAppPermissionsRequest(
                    sessionId,
                    appPermissionRequest
                )
            )
        }
    }

    override fun stop() {
        sitePermissionScope?.cancel()
        appPermissionScope?.cancel()
        loadingScope?.cancel()
    }

    /**
     * Notifies the feature that the permissions requested were completed.
     *
     * @param grantResults the grant results for the corresponding permissions
     * @see [onNeedToRequestPermissions].
     */
    @Suppress("NestedBlockDepth")
    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        val currentContentSate = getCurrentContentState()
        val appPermissionRequest = findRequestedAppPermission(permissions)

        if (appPermissionRequest != null && currentContentSate != null) {
            val allPermissionWereGranted = grantResults.all { grantResult ->
                grantResult == PackageManager.PERMISSION_GRANTED
            }

            if (grantResults.isNotEmpty() && allPermissionWereGranted) {
                appPermissionRequest.grant()
            } else {
                appPermissionRequest.reject()
                permissions.forEach { systemPermission ->
                    if (!onShouldShowRequestPermissionRationale(systemPermission)) {
                        // The system permission is denied permanently
                        storeSitePermissions(
                            currentContentSate,
                            appPermissionRequest,
                            status = BLOCKED
                        )
                    }
                }
            }
            consumeAppPermissionRequest(appPermissionRequest)
        }
    }

    @VisibleForTesting
    internal fun getCurrentContentState() = getCurrentTabState()?.content

    @VisibleForTesting
    internal fun getCurrentTabState() = store.state.findTabOrCustomTabOrSelectedTab(sessionId)

    @VisibleForTesting
    internal fun findRequestedAppPermission(permissions: Array<String>): PermissionRequest? {
        return getCurrentContentState()?.appPermissionRequestsList?.find {
            permissions.contains(it.permissions.first().id)
        }
    }

    @VisibleForTesting
    internal fun findRequestedPermission(permissionId: String): PermissionRequest? {
        return getCurrentContentState()?.permissionRequestsList?.find {
            it.id == permissionId
        }
    }

    @VisibleForTesting
    internal fun onContentPermissionGranted(
        permissionRequest: PermissionRequest,
        shouldStore: Boolean
    ) {
        permissionRequest.grant()
        if (shouldStore) {
            getCurrentContentState()?.let { contentState ->
                storeSitePermissions(contentState, permissionRequest, ALLOWED)
            }
        }
    }

    internal fun onPositiveButtonPress(
        permissionId: String,
        sessionId: String,
        shouldStore: Boolean
    ) {
        findRequestedPermission(permissionId)?.let { permissionRequest ->
            consumePermissionRequest(permissionRequest, sessionId)
            onContentPermissionGranted(permissionRequest, shouldStore)
        }
    }

    internal fun onNegativeButtonPress(
        permissionId: String,
        sessionId: String,
        shouldStore: Boolean
    ) {
        findRequestedPermission(permissionId)?.let { permissionRequest ->
            consumePermissionRequest(permissionRequest, sessionId)
            onContentPermissionDeny(permissionRequest, shouldStore)
        }
    }

    internal fun onDismiss(
        permissionId: String,
        sessionId: String
    ) {
        findRequestedPermission(permissionId)?.let { permissionRequest ->
            consumePermissionRequest(permissionRequest, sessionId)
            onContentPermissionDeny(permissionRequest, false)
        }
    }

    internal fun storeSitePermissions(
        contentState: ContentState,
        request: PermissionRequest,
        status: SitePermissions.Status,
        coroutineScope: CoroutineScope = ioCoroutineScope
    ) {
        if (contentState.private) {
            return
        }
        coroutineScope.launch {
            synchronized(storage) {
                val host = contentState.url.tryGetHostFromUrl()
                var sitePermissions =
                    storage.findSitePermissionsBy(host)

                if (sitePermissions == null) {
                    sitePermissions =
                        request.toSitePermissions(
                            host,
                            status = status,
                            permissions = request.permissions
                        )
                    storage.save(sitePermissions)
                } else {
                    sitePermissions = request.toSitePermissions(host, status, sitePermissions)
                    storage.update(sitePermissions)
                }
            }
        }
    }

    internal fun onContentPermissionDeny(
        permissionRequest: PermissionRequest,
        shouldStore: Boolean
    ) {
        permissionRequest.reject()
        if (shouldStore) {
            getCurrentContentState()?.let { contentState ->
                storeSitePermissions(contentState, permissionRequest, BLOCKED)
            }
        }
    }

    internal suspend fun onContentPermissionRequested(
        permissionRequest: PermissionRequest,
        host: String,
        coroutineScope: CoroutineScope = ioCoroutineScope
    ): SitePermissionsDialogFragment? {
        // We want to warranty that all media permissions have the required system
        // permissions are granted first, otherwise, we reject the request
        if (permissionRequest.isMedia && !permissionRequest.areAllMediaPermissionsGranted) {
            permissionRequest.reject()
            consumePermissionRequest(permissionRequest)
            return null
        }

        val permissionFromStorage = withContext(coroutineScope.coroutineContext) {
            storage.findSitePermissionsBy(host)
        }

        val prompt = if (shouldApplyRules(permissionFromStorage)) {
            handleRuledFlow(permissionRequest, host)
        } else {
            handleNoRuledFlow(permissionFromStorage, permissionRequest, host)
        }
        prompt?.show(fragmentManager, FRAGMENT_TAG)
        return prompt
    }

    @VisibleForTesting
    internal fun handleNoRuledFlow(
        permissionFromStorage: SitePermissions?,
        permissionRequest: PermissionRequest,
        host: String
    ): SitePermissionsDialogFragment? {
        return if (shouldShowPrompt(permissionRequest, permissionFromStorage)) {
            createPrompt(permissionRequest, host)
        } else {
            if (permissionFromStorage.isGranted(permissionRequest)) {
                permissionRequest.grant()
            } else {
                permissionRequest.reject()
                updateAutoplayToolbarIndicator(permissionRequest)
            }
            consumePermissionRequest(permissionRequest)
            null
        }
    }

    @VisibleForTesting
    internal fun shouldShowPrompt(
        permissionRequest: PermissionRequest,
        permissionFromStorage: SitePermissions?
    ): Boolean {
        return if (permissionRequest.isForAutoplay()) {
            false
        } else {
            (permissionFromStorage == null ||
                    !permissionRequest.doNotAskAgain(permissionFromStorage))
        }
    }

    @VisibleForTesting
    internal fun handleRuledFlow(
        permissionRequest: PermissionRequest,
        host: String
    ): SitePermissionsDialogFragment? {
        return when (sitePermissionsRules?.getActionFrom(permissionRequest)) {
            SitePermissionsRules.Action.ALLOWED -> {
                permissionRequest.grant()
                consumePermissionRequest(permissionRequest)
                null
            }
            SitePermissionsRules.Action.BLOCKED -> {
                permissionRequest.reject()
                consumePermissionRequest(permissionRequest)
                updateAutoplayToolbarIndicator(permissionRequest)
                null
            }
            SitePermissionsRules.Action.ASK_TO_ALLOW -> {
                createPrompt(permissionRequest, host)
            }
            null -> {
                consumePermissionRequest(permissionRequest)
                null
            }
        }
    }

    @VisibleForTesting
    internal fun updateAutoplayToolbarIndicator(permissionRequest: PermissionRequest) {
        if (permissionRequest.isForAutoplay()) {
            getCurrentTabState()?.let {
                store.dispatch(UpdatePermissionHighlightsStateAction(
                    it.id,
                    PermissionHighlightsState(true)
                ))
            }
        }
    }

    @VisibleForTesting
    internal fun shouldApplyRules(permissionFromStorage: SitePermissions?) =
        sitePermissionsRules != null && permissionFromStorage == null

    private fun PermissionRequest.doNotAskAgain(permissionFromStore: SitePermissions): Boolean {
        return permissions.any { permission ->
            when (permission) {
                is ContentGeoLocation -> {
                    permissionFromStore.location.doNotAskAgain()
                }
                is ContentNotification -> {
                    permissionFromStore.notification.doNotAskAgain()
                }
                is ContentAudioCapture, is ContentAudioMicrophone -> {
                    permissionFromStore.microphone.doNotAskAgain()
                }
                is ContentVideoCamera, is ContentVideoCapture -> {
                    permissionFromStore.camera.doNotAskAgain()
                }
                is ContentPersistentStorage -> {
                    permissionFromStore.localStorage.doNotAskAgain()
                }
                is ContentMediaKeySystemAccess -> {
                    permissionFromStore.mediaKeySystemAccess.doNotAskAgain()
                }
                else -> false
            }
        }
    }

    private fun PermissionRequest.toSitePermissions(
        host: String,
        status: SitePermissions.Status,
        initialSitePermission: SitePermissions = getInitialSitePermissions(host),
        permissions: List<Permission> = this.permissions
    ): SitePermissions {
        var sitePermissions = initialSitePermission
        for (permission in permissions) {
            sitePermissions = updateSitePermissionsStatus(status, permission, sitePermissions)
        }
        return sitePermissions
    }

    @VisibleForTesting
    internal fun getInitialSitePermissions(
        host: String
    ): SitePermissions {
        val rules = sitePermissionsRules
        return rules?.toSitePermissions(
            host,
            savedAt = System.currentTimeMillis()
        )
            ?: SitePermissions(host, savedAt = System.currentTimeMillis())
    }

    private fun PermissionRequest.isForAutoplay() =
        this.permissions.any { it is ContentAutoPlayInaudible || it is ContentAutoPlayAudible }

    @VisibleForTesting
    internal fun updateSitePermissionsStatus(
        status: SitePermissions.Status,
        permission: Permission,
        sitePermissions: SitePermissions
    ): SitePermissions {
        return when (permission) {
            is ContentGeoLocation, is AppLocationCoarse, is AppLocationFine -> {
                sitePermissions.copy(location = status)
            }
            is ContentNotification -> {
                sitePermissions.copy(notification = status)
            }
            is ContentAudioCapture, is ContentAudioMicrophone, is AppAudio -> {
                sitePermissions.copy(microphone = status)
            }
            is ContentVideoCamera, is ContentVideoCapture, is AppCamera -> {
                sitePermissions.copy(camera = status)
            }
            is ContentAutoPlayAudible -> {
                sitePermissions.copy(autoplayAudible = status.toAutoplayStatus())
            }
            is ContentAutoPlayInaudible -> {
                sitePermissions.copy(autoplayInaudible = status.toAutoplayStatus())
            }
            is ContentPersistentStorage -> {
                sitePermissions.copy(localStorage = status)
            }
            is ContentMediaKeySystemAccess -> {
                sitePermissions.copy(mediaKeySystemAccess = status)
            }
            else ->
                throw InvalidParameterException("$permission is not a valid permission.")
        }
    }

    @VisibleForTesting
    internal fun createPrompt(
        permissionRequest: PermissionRequest,
        host: String
    ): SitePermissionsDialogFragment {
        return if (!permissionRequest.containsVideoAndAudioSources()) {
            val permission = permissionRequest.permissions.first()
            handlingSingleContentPermissions(permissionRequest, permission, host)
        } else {
            createSinglePermissionPrompt(
                context,
                host,
                permissionRequest,
                R.string.mozac_feature_sitepermissions_camera_and_microphone,
                R.drawable.mozac_ic_microphone,
                showDoNotAskAgainCheckBox = true,
                shouldSelectRememberChoice = dialogConfig?.shouldPreselectDoNotAskAgain
                    ?: DialogConfig.DEFAULT_PRESELECT_DO_NOT_ASK_AGAIN
            )
        }
    }

    @Suppress("LongMethod")
    @VisibleForTesting
    internal fun handlingSingleContentPermissions(
        permissionRequest: PermissionRequest,
        permission: Permission,
        host: String
    ): SitePermissionsDialogFragment {
        return when (permission) {
            is ContentGeoLocation -> {
                createSinglePermissionPrompt(
                    context,
                    host,
                    permissionRequest,
                    R.string.mozac_feature_sitepermissions_location_title,
                    R.drawable.mozac_ic_location,
                    showDoNotAskAgainCheckBox = true,
                    shouldSelectRememberChoice = dialogConfig?.shouldPreselectDoNotAskAgain
                        ?: DialogConfig.DEFAULT_PRESELECT_DO_NOT_ASK_AGAIN
                )
            }
            is ContentNotification -> {
                createSinglePermissionPrompt(
                    context,
                    host,
                    permissionRequest,
                    R.string.mozac_feature_sitepermissions_notification_title,
                    R.drawable.mozac_ic_notification,
                    showDoNotAskAgainCheckBox = false,
                    shouldSelectRememberChoice = false,
                    isNotificationRequest = true
                )
            }
            is ContentAudioCapture, is ContentAudioMicrophone -> {
                createSinglePermissionPrompt(
                    context,
                    host,
                    permissionRequest,
                    R.string.mozac_feature_sitepermissions_microfone_title,
                    R.drawable.mozac_ic_microphone,
                    showDoNotAskAgainCheckBox = true,
                    shouldSelectRememberChoice = dialogConfig?.shouldPreselectDoNotAskAgain
                        ?: DialogConfig.DEFAULT_PRESELECT_DO_NOT_ASK_AGAIN
                )
            }
            is ContentVideoCamera, is ContentVideoCapture -> {
                createSinglePermissionPrompt(
                    context,
                    host,
                    permissionRequest,
                    R.string.mozac_feature_sitepermissions_camera_title,
                    R.drawable.mozac_ic_video,
                    showDoNotAskAgainCheckBox = true,
                    shouldSelectRememberChoice = dialogConfig?.shouldPreselectDoNotAskAgain
                        ?: DialogConfig.DEFAULT_PRESELECT_DO_NOT_ASK_AGAIN
                )
            }
            is ContentPersistentStorage -> {
                createSinglePermissionPrompt(
                    context,
                    host,
                    permissionRequest,
                    R.string.mozac_feature_sitepermissions_persistent_storage_title,
                    R.drawable.mozac_ic_storage,
                    showDoNotAskAgainCheckBox = false,
                    shouldSelectRememberChoice = true
                )
            }
            is ContentMediaKeySystemAccess -> {
                createSinglePermissionPrompt(
                    context,
                    host,
                    permissionRequest,
                    R.string.mozac_feature_sitepermissions_media_key_system_access_title,
                    R.drawable.mozac_ic_link,
                    showDoNotAskAgainCheckBox = false,
                    shouldSelectRememberChoice = true
                )
            }
            else ->
                throw InvalidParameterException("$permission is not a valid permission.")
        }
    }

    @Suppress("LongParameterList")
    @VisibleForTesting
    internal fun createSinglePermissionPrompt(
        context: Context,
        host: String,
        permissionRequest: PermissionRequest,
        @StringRes titleId: Int,
        @DrawableRes iconId: Int,
        showDoNotAskAgainCheckBox: Boolean,
        shouldSelectRememberChoice: Boolean,
        isNotificationRequest: Boolean = false
    ): SitePermissionsDialogFragment {
        val title = context.getString(titleId, host)

        val currentSessionId: String = store.state.findTabOrCustomTabOrSelectedTab(sessionId)?.id
                ?: throw IllegalStateException("Unable to find session for $sessionId or selected session")

        return SitePermissionsDialogFragment.newInstance(
            currentSessionId,
            title,
            iconId,
            permissionRequest.id,
            this,
            showDoNotAskAgainCheckBox,
            isNotificationRequest = isNotificationRequest,
            shouldSelectDoNotAskAgainCheckBox = shouldSelectRememberChoice
        )
    }

    private val PermissionRequest.isMedia: Boolean
        get() {
            return when (permissions.first()) {
                is ContentVideoCamera, is ContentVideoCapture,
                is ContentAudioCapture, is ContentAudioMicrophone -> true
                else -> false
            }
        }

    private val PermissionRequest.areAllMediaPermissionsGranted: Boolean
        get() {
            val systemPermissions = mutableListOf<String>()
            permissions.forEach { permission ->
                when (permission) {
                    is ContentVideoCamera, is ContentVideoCapture -> {
                        systemPermissions.add(CAMERA)
                    }
                    is ContentAudioCapture, is ContentAudioMicrophone -> {
                        systemPermissions.add(RECORD_AUDIO)
                    }
                }
            }
            return systemPermissions.all { context.isPermissionGranted((it)) }
        }

    data class PromptsStyling(
        val gravity: Int,
        val shouldWidthMatchParent: Boolean = false,
        @ColorRes
        val positiveButtonBackgroundColor: Int? = null,
        @ColorRes
        val positiveButtonTextColor: Int? = null
    )

    /**
     * Customization options for feature request dialog
     */
    data class DialogConfig(
        /** Use **true** to pre-select "Do not ask again" checkbox. */
        val shouldPreselectDoNotAskAgain: Boolean = false
    ) {

        companion object {
            /** Default values for [DialogConfig.shouldPreselectDoNotAskAgain] */
            internal const val DEFAULT_PRESELECT_DO_NOT_ASK_AGAIN = false
        }
    }

    /**
     * Re-attaches a fragment that is still visible but not linked to this feature anymore.
     */
    private fun reattachFragment(fragment: SitePermissionsDialogFragment) {
        val currentTab = store.state.findTabOrCustomTabOrSelectedTab(fragment.sessionId)?.content
        if (currentTab == null || (noPermissionRequests(currentTab))) {
            fragmentManager.beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss()
        } else {
            // Re-assign the feature instance so that the fragment can invoke us once the
            // user makes a selection or cancels the dialog.
            fragment.feature = this
        }
    }

    @VisibleForTesting
    internal fun noPermissionRequests(contentState: ContentState) =
        contentState.appPermissionRequestsList.isEmpty() &&
                contentState.permissionRequestsList.isEmpty()
}

internal fun SitePermissions?.isGranted(permissionRequest: PermissionRequest): Boolean {
    return if (this != null) {
        permissionRequest.permissions.all { permission ->
            isPermissionGranted(permission, this)
        }
    } else {
        false
    }
}

@VisibleForTesting
internal fun isPermissionGranted(
    permission: Permission,
    permissionFromStorage: SitePermissions
): Boolean {
    return when (permission) {
        is ContentGeoLocation -> {
            permissionFromStorage.location.isAllowed()
        }
        is ContentNotification -> {
            permissionFromStorage.notification.isAllowed()
        }
        is ContentAudioCapture, is ContentAudioMicrophone -> {
            permissionFromStorage.microphone.isAllowed()
        }
        is ContentVideoCamera, is ContentVideoCapture -> {
            permissionFromStorage.camera.isAllowed()
        }
        is ContentPersistentStorage -> {
            permissionFromStorage.localStorage.isAllowed()
        }
        is ContentMediaKeySystemAccess -> {
            permissionFromStorage.mediaKeySystemAccess.isAllowed()
        }
        is ContentAutoPlayAudible -> {
            permissionFromStorage.autoplayAudible.isAllowed()
        }
        is ContentAutoPlayInaudible -> {
            permissionFromStorage.autoplayInaudible.isAllowed()
        }
        else ->
            throw InvalidParameterException("$permission is not a valid permission.")
    }
}

private fun Permission.isSupported(): Boolean {
    return when (this) {
        is ContentGeoLocation,
        is ContentNotification,
        is ContentPersistentStorage,
        is ContentAudioCapture, is ContentAudioMicrophone,
        is ContentVideoCamera, is ContentVideoCapture,
        is ContentAutoPlayAudible, is ContentAutoPlayInaudible,
        is ContentMediaKeySystemAccess -> true
        else -> false
    }
}
