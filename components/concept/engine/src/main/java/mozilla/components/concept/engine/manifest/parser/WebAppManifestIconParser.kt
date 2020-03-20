/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package mozilla.components.concept.engine.manifest.parser

import mozilla.components.concept.engine.manifest.Size
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.support.ktx.android.org.json.asSequence
import mozilla.components.support.ktx.android.org.json.tryGet
import mozilla.components.support.ktx.android.org.json.tryGetString
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private val whitespace = "\\s+".toRegex()

/**
 * Parses the icons array from a web app manifest.
 */
internal fun parseIcons(json: JSONObject): List<WebAppManifest.Icon> {
    val array = json.optJSONArray("icons") ?: return emptyList()

    return array
        .asSequence { i -> getJSONObject(i) }
        .mapNotNull { obj ->
            val purpose = parsePurposes(obj).ifEmpty {
                return@mapNotNull null
            }
            WebAppManifest.Icon(
                src = obj.getString("src"),
                sizes = parseIconSizes(obj),
                type = obj.tryGetString("type"),
                purpose = purpose
            )
        }
        .toList()
}

/**
 * Parses a string set, which is expressed as either a space-delimited string or JSONArray of strings.
 *
 * Gecko returns a JSONArray to represent the intermediate infra type for some properties.
 */
private fun parseStringSet(set: Any?): Sequence<String>? = when (set) {
    is String -> set.split(whitespace).asSequence()
    is JSONArray -> set.asSequence { i -> getString(i) }
    else -> null
}

private fun parseIconSizes(json: JSONObject): List<Size> {
    val sizes = parseStringSet(json.tryGet("sizes"))
        ?: return emptyList()

    return sizes.mapNotNull { Size.parse(it) }.toList()
}

private fun parsePurposes(json: JSONObject): Set<WebAppManifest.Icon.Purpose> {
    val purpose = parseStringSet(json.tryGet("purpose"))
        ?: return setOf(WebAppManifest.Icon.Purpose.ANY)

    return purpose
        .mapNotNull {
            when (it.toLowerCase(Locale.ROOT)) {
                "badge" -> WebAppManifest.Icon.Purpose.BADGE
                "maskable" -> WebAppManifest.Icon.Purpose.MASKABLE
                "any" -> WebAppManifest.Icon.Purpose.ANY
                else -> null
            }
        }
        .toSet()
}

internal fun serializeEnumName(name: String) = name.toLowerCase(Locale.ROOT).replace('_', '-')

internal fun serializeIcons(icons: List<WebAppManifest.Icon>): JSONArray {
    val list = icons.map { icon ->
        JSONObject().apply {
            put("src", icon.src)
            put("sizes", icon.sizes.joinToString(" ") { it.toString() })
            putOpt("type", icon.type)
            put("purpose", icon.purpose.joinToString(" ") { serializeEnumName(it.name) })
        }
    }
    return JSONArray(list)
}
