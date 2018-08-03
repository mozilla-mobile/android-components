package mozila.components.concept.pwa

import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject

class PwaModel(
        val short_name: String,
        val name: String,
        val display: String,
        val icon: Icon?,
        val start_url: String,
        val theme_color: String,
        val background_color: String) {

    val pwaIconName: String by lazy {
        if (short_name.isNotEmpty())
            return@lazy short_name
        if (name.isNotEmpty())
            return@lazy name
        if (start_url.isNotEmpty())
            return@lazy start_url
        ""

    }
    var iconBitmap: Bitmap? = null


    data class Icon(
            val src: String,
            val sizes: String,
            val type: String)

    object IconParser {
        fun findMax(jsonArray: JSONArray): Icon? {
            val list: ArrayList<Icon> = ArrayList()
            for (i in 0..jsonArray.length() - 1) {
                val jsonObject = jsonArray.getJSONObject(i)
                val icon = Icon(jsonObject.optString("src"),
                        jsonObject.optString("sizes"),
                        jsonObject.optString("type"))
                list.add(icon)
            }
            var size = 0
            var max: Icon? = null

            for (icon in list) {
                val s = icon.sizes.split("x")[0]
                try {
                    val valueOf = Integer.valueOf(s)
                    if (valueOf > size) {
                        size = valueOf
                        max = icon
                    }
                } catch (e: NumberFormatException) {
                    continue
                }

            }
            return max
        }
    }

    companion object {
        fun fromJson(pwaUrl: String, pwaJsonObject: JSONObject): PwaModel {

            val icons: Icon?
            if (pwaJsonObject.has("icons")) {
                icons = IconParser.findMax(pwaJsonObject.getJSONArray("icons"))
            } else {
                icons = null
            }

            val shortName = pwaJsonObject.optString("short_name")
            val name = pwaJsonObject.optString("name")
            val display = pwaJsonObject.optString("display")
            val link = pwaJsonObject.optString("start_url", null)
                    ?: throw IllegalStateException(" no start_url in manifest")
            val startUrl = PwaPresenter.link(link, pwaUrl)
                    ?: throw IllegalStateException(" can't map start_url to a valid url")


            val theme_color = pwaJsonObject.optString("theme_color")
            val background_color = pwaJsonObject.optString("background_color")
            return PwaModel(shortName,
                    name,
                    display,
                    icons,
                    startUrl,
                    theme_color,
                    background_color)
        }
    }


}
