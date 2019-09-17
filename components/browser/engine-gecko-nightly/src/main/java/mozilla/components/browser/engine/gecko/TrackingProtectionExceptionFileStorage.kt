/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */


package mozilla.components.browser.engine.gecko

import android.content.Context
import android.util.AtomicFile
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.content.blocking.TrackingProtectionExceptionStorage
import org.json.JSONException
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val STORE_FILE_NAME_FORMAT =
    "mozilla_components_tracking_protection_storage_gecko.json"

class TrackingProtectionExceptionFileStorage(
    private val context: Context,
    private val runtime: GeckoRuntime
) : TrackingProtectionExceptionStorage {
    private val fileLock = Any()

    @WorkerThread //This will be on coroutine or similar
    fun restore() {
        synchronized(fileLock) {
            getFile(context).readAndDeserialize { json ->
               if (json.isNotEmpty()) {
                   val exceptionList =  runtime.contentBlockingController.ExceptionList(json)
                   runtime.contentBlockingController.restoreExceptionList(exceptionList)
                }
            }
        }
    }

    override fun remove(session: EngineSession) {
        val geckoSession = (session as GeckoEngineSession).geckoSession
        runtime.contentBlockingController.removeException(geckoSession)
        persist()
    }

    override fun containsAsync(session: EngineSession, onFinish: (Boolean) -> Unit) {
        val geckoSession = (session as GeckoEngineSession).geckoSession
        runtime.contentBlockingController.checkException(geckoSession).then({
            onFinish(requireNotNull(it))
            GeckoResult<Void>()
        }, {
            onFinish(false)
            GeckoResult<Void>()
        })
    }

    override fun getAll(onFinish: (List<String>) -> Unit) {
        runtime.contentBlockingController.saveExceptionList().accept {
            val exceptionList = requireNotNull(it)
            val uris = exceptionList.uris.map { uri ->
                uri.toString()
            }
            onFinish(uris)
        }
    }

    override fun add(session: EngineSession) {
        val geckoSession = (session as GeckoEngineSession).geckoSession
        runtime.contentBlockingController.addException(geckoSession)
        persist()
    }

    override fun removeAll() {
        runtime.contentBlockingController.clearExceptionList()
        removeFileFromDisk(context)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getFile(context: Context): AtomicFile {
        return AtomicFile(
            File(
                context.filesDir,
                STORE_FILE_NAME_FORMAT
            )
        )
    }

    //This will be on coroutine or similar
    private fun persist() {
        runtime.contentBlockingController.saveExceptionList().accept {
            it?.let { list ->
                getFile(context).writeString {
                    list.toJson().toString()
                }
            }
        }
    }

    private fun removeFileFromDisk(context: Context) {
        synchronized(fileLock) {
            getFile(context)
                .delete()
        }
    }

    // This will under support-ktx
    private fun <T> AtomicFile.readAndDeserialize(block: (String) -> T): T? {
        return try {
            openRead().use {
                val json = it.bufferedReader().use { reader -> reader.readText() }
                block(json)
            }
        } catch (_: IOException) {
            null
        } catch (_: JSONException) {
            null
        }
    }

    // This will under support-ktx
    private fun AtomicFile.writeString(block: () -> String): Boolean {
        var outputStream: FileOutputStream? = null

        return try {
            outputStream = startWrite()
            outputStream.write(block().toByteArray())
            finishWrite(outputStream)
            true
        } catch (_: IOException) {
            failWrite(outputStream)
            false
        } catch (_: JSONException) {
            failWrite(outputStream)
            false
        }
    }
}