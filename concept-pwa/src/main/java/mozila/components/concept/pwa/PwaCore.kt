package mozila.components.concept.pwa

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.net.URL

interface PwaViewContract {
    fun notifyPwaReady(jsonObject: PwaModel)
}

private const val TAG: String = "PwaCore"


class PwaPresenter(viewContract: PwaViewContract) : LifecycleObserver {

    private var pwaViewContract: WeakReference<PwaViewContract> = WeakReference(viewContract)
    private var job: Job? = null

    // cancel the job if it won't be displayed.
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun cancelJob() {
        job?.cancel()
    }

    companion object {

        const val NOT_PWA = "null"

        internal fun link(link: String, url: String): String? {
            val result: String
            val linkIsValidUrl: Boolean = try {
                URL(link)
                true
            } catch (e: MalformedURLException) {
                false
            }
            try {
                val manifestUrl = URL(url)
                var path = manifestUrl.path.substring(0, manifestUrl.path.lastIndexOf('/') + 1)
                if (path.isEmpty()) {
                    path = "/"
                }

                result = when {
                    linkIsValidUrl -> link
                    link == "." -> manifestUrl.protocol + "://" + manifestUrl.host + path
                    link.startsWith(".") -> manifestUrl.protocol + "://" + manifestUrl.host + path + link
                    link.startsWith("?") -> url + link
                    link.startsWith("/") -> manifestUrl.protocol + "://" + manifestUrl.host + link
                    else -> manifestUrl.protocol + "://" + manifestUrl.host + path + link
                }
            } catch (e: MalformedURLException) {
                return null
            }

            return result

        }

    }

    fun bind(host: String, result: String) {

        val path = result.replace("\"", "")

        if (path == NOT_PWA) return

        job = launch(UI) {
            try {

                val pwaModel = getPwaModel(host, path)
                pwaViewContract.get()?.notifyPwaReady(pwaModel)
            } catch (e: MalformedURLException) {
            } catch (e: IllegalStateException) {
            } catch (e: JSONException) {
            } catch (e: IOException) {
                Log.d(TAG, "bind failed: $e")
            }
        }


    }

    private suspend fun getPwaModel(url: String, pwaPath: String): PwaModel = withContext(CommonPool) {
        val manifestUrl = link(pwaPath, url)
                ?: throw IllegalStateException("Can't get PWA Manifest")

        val pwaJson = JSONObject(URL(manifestUrl).readText())

        val pwaModel = PwaModel.fromJson(manifestUrl, pwaJson)
        if (pwaModel.icon != null) {
            val src = pwaModel.icon.src
            val iconUrl = link(src, manifestUrl)
                    ?: return@withContext pwaModel
            val bmp = BitmapFactory.decodeStream(URL(iconUrl).openStream())
            pwaModel.iconBitmap = bmp
        }
        pwaModel
    }

}