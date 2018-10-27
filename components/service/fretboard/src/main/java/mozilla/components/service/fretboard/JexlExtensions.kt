/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fretboard

import android.content.Context
import mozilla.components.lib.jexl.evaluator.JexlContext
import mozilla.components.lib.jexl.ext.toJexl

fun ValuesProvider.toJexlContext(context: Context): JexlContext {
    return JexlContext(
        "language" to getLanguage(context).toJexl(),
        "appId" to getAppId(context).toJexl(),
        "version" to getVersion(context).toJexl(),
        "manufacturer" to getManufacturer(context).toJexl(),
        "device" to getDevice(context).toJexl(),
        "country" to getCountry(context).toJexl(),
        "clientId" to getClientId(context).toJexl()
    ).apply {
        getRegion(context)?.let { set("region", it.toJexl()) }
        getReleaseChannel(context)?.let { set("releaseChannel", it.toJexl()) }
        addCustomValuesToJexlContext(values, this)
    }
}

private fun addCustomValuesToJexlContext(values: Map<String, Any>, jexlContext: JexlContext) {
    values.forEach {
        val value = it.value
        when (value) {
            is String -> jexlContext.set(it.key, value.toJexl())
            is Int -> jexlContext.set(it.key, value.toJexl())
            is Double -> jexlContext.set(it.key, value.toJexl())
            is Float -> jexlContext.set(it.key, value.toJexl())
            is Boolean -> jexlContext.set(it.key, value.toJexl())
        }
    }
}
