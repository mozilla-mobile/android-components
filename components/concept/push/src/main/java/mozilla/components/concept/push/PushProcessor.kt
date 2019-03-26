/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.concept.push

/**
 * A push notification processor that handles registration and new messages from the [PushService] provided.
 * Starting Push in the Application's onCreate is recommended.
 */
interface PushProcessor {

    /**
     * Start the push processor and starts any service associated.
     */
    fun start()

    /**
     * Stop the push service and stops any service associated.
     */
    fun stop()

    /**
     * A new registration token has been received.
     */
    fun onNewToken(newToken: String)

    /**
     * A new push message has been received.
     */
    fun onMessageReceived(message: PushMessage)

    /**
     * An error has occurred.
     */
    fun onError(error: Error)
}

/**
 * Provider class for the PushProcessor singleton.
 */
object PushProvider {

    /**
     * Initialize the PushProcessor that needs to be called before
     */
    fun install(processor: PushProcessor) {
        instance = processor
    }

    @Volatile
    private var instance: PushProcessor? = null
    val requireInstance: PushProcessor
        get() = instance ?: throw IllegalStateException(
            "You need to call start() on your Push instance from Application.onCreate()."
        )
}

/**
 * A push message holds the information needed to pass the message on to the appropriate receiver.
 */
data class PushMessage(val data: String)

/**
 * The different kind of message types that a [PushMessage] can be.
 */
enum class PushType {
    Services,
    ThirdParty,
    Unknown
}

/**
 *  Various error types.
 */
sealed class Error {
    data class RegistrationError(val desc: String) : Error()
    data class NetworkError(val desc: String) : Error()
}
