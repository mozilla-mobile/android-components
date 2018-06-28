/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.fxa

import android.util.Log

class Config(override var rawPointer: FxaClient.RawConfig?) : RustObject<FxaClient.RawConfig>() {

    override fun destroyPointer(cfg: FxaClient.RawConfig) {
        FxaClient.INSTANCE.fxa_config_free(cfg)
    }

    companion object {
        fun release(): Config? {
            val e = Error.ByReference()
            val cfg = FxaClient.INSTANCE.fxa_get_release_config(e)
            if (e.isSuccess()) {
                return Config(cfg)
            } else {
                Log.e("Config.release", e.consumeMessage())
                return null
            }
        }

        fun custom(content_base: String): Config? {
            val e = Error.ByReference()
            val cfg = FxaClient.INSTANCE.fxa_get_custom_config(content_base, e)
            if (e.isSuccess()) {
                return Config(cfg)
            } else {
                Log.e("Config.custom", e.consumeMessage())
                return null
            }
        }
    }
}
