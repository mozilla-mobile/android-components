/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fretboard.source.kinto

import android.net.http.X509TrustManagerExtensions
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class TestTrustManagerExtensions(private val trustManager: X509TrustManager) : X509TrustManagerExtensions(trustManager) {
    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, host: String): MutableList<X509Certificate> {
        trustManager.checkServerTrusted(chain, authType)
        return chain.toMutableList()
    }
}
