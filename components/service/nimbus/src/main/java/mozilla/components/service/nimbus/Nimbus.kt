/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.nimbus

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mozilla.components.service.glean.Glean
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import mozilla.components.support.locale.getLocaleTag
import org.mozilla.experiments.nimbus.AppContext
import org.mozilla.experiments.nimbus.AvailableRandomizationUnits
import org.mozilla.experiments.nimbus.EnrolledExperiment
import org.mozilla.experiments.nimbus.NimbusClient
import org.mozilla.experiments.nimbus.NimbusClientInterface
import org.mozilla.experiments.nimbus.RemoteSettingsConfig
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

/**
 * This is the main experiments API, which is exposed through the global [Nimbus] object.
 */
interface NimbusApi {
    /**
     * Initialize the Nimbus SDK library.
     *
     * This should only be initialized once by the application. Initializing the SDK does not fetch
     * experiments from the endpoint, see [updateExperiments] and [getActiveExperiments] in order
     * to retrieve experiments.
     *
     * @param context [Context] to access application and device parameters.  As we cannot enforce
     * through the compiler that the context pass to the initialize function is a Application
     * Context, there could potentially be a memory leak if the initializing application doesn't
     * comply.
     *
     * @param onExperimentUpdated callback that will be executed when the list of experiments has
     * been updated from the experiments endpoint and evaluated by the Nimbus-SDK. This is meant to
     * be used for consuming applications to perform any actions as a result of enrollment in an
     * experiment so that the application is not required to await the network request. The
     * callback will be supplied with the list of active experiments (if any) for which the client
     * is enrolled.
     */
    fun initialize(
        context: Context,
        onExperimentUpdated: ((activeExperiments: List<EnrolledExperiment>) -> Unit)? = null
    )
    /**
     * Get the list of currently enrolled experiments
     *
     * @return A list of [EnrolledExperiment]s
     */
    fun getActiveExperiments(): List<EnrolledExperiment>

    /**
     * Get the currently enrolled branch for the given experiment
     *
     * @param experimentId The string experiment-id or "slug" for which to retrieve the branch
     *
     * @return A String representing the branch-id or "slug"
     */
    fun getExperimentBranch(experimentId: String): String?

    /**
     * Refreshes the experiments from the endpoint. Should be called at least once after
     * calling [initialize]
     */
    fun updateExperiments()

    /**
     * Opt out of a specific experiment
     *
     * @param experimentId The string experiment-id or "slug" for which to opt out of
     */
    fun optOut(experimentId: String)

    /**
     * Control the opt out for all experiments at once. This is likely a user action.
     */
    var globalUserParticipation: Boolean
}

private const val LOG_TAG = "service/Nimbus"
private const val EXPERIMENT_BUCKET_NAME = "main"
private const val EXPERIMENT_COLLECTION_NAME = "nimbus-mobile-experiments"
private const val NIMBUS_DATA_DIR: String = "nimbus_data"

/**
 * An implementation of the [NimbusApi] interface backed by the Nimbus SDK.
 * This class allows client apps to configure Nimbus to point to your own server.
 * Client app developers should set up their own Nimbus infrastructure, to avoid different
 * organizations running conflicting experiments or hitting servers with extra network traffic.
 */
class Nimbus(private val delegate: Observable<Observer> = ObserverRegistry()) :
    NimbusApi, Observable<Nimbus.Observer> by delegate {

    /**
     * Interface to be implemented by classes that want to observe experiment updates
     */
    interface Observer {
        /**
         * Event to indicate that the experiments have been fetched from the endpoint
         */
        fun onExperimentsFetched()
    }

data class NimbusServerSettings(
    val url: Uri
)

/**
 * A implementation of the [NimbusApi] interface backed by the Nimbus SDK.
 */
class Nimbus(
    context: Context,
    server: NimbusServerSettings?
) : NimbusApi {
    // Using a single threaded executor here to enforce synchronization where needed.
    private val scope: CoroutineScope =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val nimbus: NimbusClientInterface
    private var onExperimentUpdated: ((List<EnrolledExperiment>) -> Unit)? = null

    private var isInitialized = false

    override var globalUserParticipation: Boolean
        get() = nimbus.getGlobalUserParticipation()
        set(active) = nimbus.setGlobalUserParticipation(active)

    init {
        // Set the name of the native library so that we use
        // the appservices megazord for compiled code.
        System.setProperty(
            "uniffi.component.nimbus.libraryOverride",
            System.getProperty("mozilla.appservices.megazord.library", "megazord")
        )

        // Build Nimbus AppContext object to pass into initialize
        val experimentContext = buildExperimentContext(context)

        // Build a File object to represent the data directory for Nimbus data
        val dataDir = File(context.applicationInfo.dataDir, NIMBUS_DATA_DIR)

        // Initialize Nimbus
        val remoteSettingsConfig = server?.let {
            RemoteSettingsConfig(
                serverUrl = server.url.toString(),
                bucketName = EXPERIMENT_BUCKET_NAME,
                collectionName = EXPERIMENT_COLLECTION_NAME
            )
        }
        // We'll temporarily use this until the Nimbus SDK supports null servers
        // https://github.com/mozilla/nimbus-sdk/pull/66 is landed, so this is the next release of
        // Nimbus SDK.
        ?: RemoteSettingsConfig(
            serverUrl = "https://settings.stage.mozaws.net",
            bucketName = EXPERIMENT_BUCKET_NAME,
            collectionName = EXPERIMENT_COLLECTION_NAME
        )

        nimbus = NimbusClient(
            experimentContext,
            dataDir.path,
            remoteSettingsConfig,
            // The "dummy" field here is required for obscure reasons when generating code on desktop,
            // so we just automatically set it to a dummy value.
            AvailableRandomizationUnits(clientId = null, dummy = 0)
        )
    }

    /**
     * Initialize the Nimbus SDK library.
     *
     * This should only be initialized once by the application.
     *
     * @param onExperimentUpdated callback that will be executed when the list of experiments has
     * been updated from the experiments endpoint and evaluated by the Nimbus-SDK. This is meant to
     * be used for consuming applications to perform any actions as a result of enrollment in an
     * experiment so that the application is not required to await the network request. The
     * callback will be supplied with the list of active experiments (if any) for which the client
     * is enrolled.
     */
    fun initialize(
        onExperimentUpdated: ((activeExperiments: List<EnrolledExperiment>) -> Unit)? = null
    ) {
        this.onExperimentUpdated = onExperimentUpdated

        // Do initialization off of the main thread
        scope.launch {
            nimbus.updateExperiments()

            // Get experiments
            val activeExperiments = nimbus.getActiveExperiments()

            // Build a File object to represent the data directory for Nimbus data
            dataDir = File(context.applicationInfo.dataDir, NIMBUS_DATA_DIR)

            // Initialize Nimbus
            nimbus = NimbusClient(
                experimentContext,
                dataDir.path,
                RemoteSettingsConfig(
                    serverUrl = context.resources.getString(R.string.nimbus_default_endpoint),
                    bucketName = EXPERIMENT_BUCKET_NAME,
                    collectionName = EXPERIMENT_COLLECTION_NAME
                ),
                // The "dummy" field here is required for obscure reasons when generating code on desktop,
                // so we just automatically set it to a dummy value.
                AvailableRandomizationUnits(clientId = null, dummy = 0)
            )
        }
    }

    override fun getActiveExperiments(): List<EnrolledExperiment> =
        if (isInitialized) { nimbus.getActiveExperiments() } else { emptyList() }

    override fun getExperimentBranch(experimentId: String): String? =
        if (isInitialized) { nimbus.getExperimentBranch(experimentId) } else { null }

    override fun updateExperiments() {
        if (!isInitialized) return
        scope.launch {
            nimbus.updateExperiments()
            // Get the experiments to record in telemetry
            nimbus.getActiveExperiments().let {
                if (it.any()) {
                    recordExperimentTelemetry(it)
                    // The current plan is to have the nimbus-sdk updateExperiments() function
                    // return a diff of the experiments that have been received, at which point we
                    // can emit the appropriate telemetry events.
                }
                onExperimentUpdated?.invoke(it)
            }
            notifyObservers { onExperimentsFetched() }
        }
    }

    override fun optOut(experimentId: String) {
        if (!isInitialized) return
        nimbus.optOut(experimentId)
    }

    // This function shouldn't be exposed to the public API, but is meant for testing purposes to
    // force an experiment/branch enrollment.
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun optInWithBranch(experiment: String, branch: String) {
        if (!isInitialized) return
        nimbus.optInWithBranch(experiment, branch)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun recordExperimentTelemetry(experiments: List<EnrolledExperiment>) {
        // Call Glean.setExperimentActive() for each active experiment.
        experiments.forEach {
            // For now, we will just record the experiment id and the branch id. Once we can call
            // Glean from Rust, this will move to the nimbus-sdk Rust core.
            Glean.setExperimentActive(it.slug, it.branchSlug)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun buildExperimentContext(context: Context): AppContext {
        val packageInfo: PackageInfo? = try {
            context.packageManager.getPackageInfo(
                context.packageName, 0
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.log(Log.Priority.ERROR,
                LOG_TAG,
                message = "Could not retrieve package info for appBuild and appVersion"
            )
            null
        }

        return AppContext(
                appId = context.packageName,
                androidSdkVersion = Build.VERSION.SDK_INT.toString(),
                appBuild = packageInfo?.let { PackageInfoCompat.getLongVersionCode(it).toString() },
                appVersion = packageInfo?.versionName,
                architecture = Build.SUPPORTED_ABIS[0],
                debugTag = null,
                deviceManufacturer = Build.MANUFACTURER,
                deviceModel = Build.MODEL,
                locale = Locale.getDefault().getLocaleTag(),
                os = "Android",
                osVersion = Build.VERSION.RELEASE)
    }
}
