/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fretboard.source.kinto

import android.net.http.X509TrustManagerExtensions
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal class CertificatePinner {
    internal fun checkCertificatePinning(
        connection: HttpsURLConnection,
        pinnedKeys: Set<String>,
        trustManagerExtensions: X509TrustManagerExtensions = defaultExtensions()
    ): Boolean {
        if (pinnedKeys.isEmpty()) {
            return true
        }
        val shaDigest = MessageDigest.getInstance("SHA-256")
        return try {
            val trustedChain = getTrustedChain(trustManagerExtensions, connection)
            trustedChain.any { checkPinnedCertificates(it, shaDigest, pinnedKeys) }
        } catch (e: SSLException) {
            false
        }
    }

    internal fun defaultExtensions(): X509TrustManagerExtensions {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        val trustManager = trustManagers.first { it is X509TrustManager } as X509TrustManager
        return X509TrustManagerExtensions(trustManager)
    }

    private fun getTrustedChain(
        trustManagerExtensions: X509TrustManagerExtensions,
        connection: HttpsURLConnection
    ): List<X509Certificate> {
        val serverCertificates = connection.serverCertificates.map { it as X509Certificate }.toTypedArray()
        try {
            val result = trustManagerExtensions.checkServerTrusted(serverCertificates, "RSA", connection.url.host)
            return result
        } catch (e: CertificateException) {
            throw SSLException(e)
        }
    }

    private fun checkPinnedCertificates(
        certificate: X509Certificate,
        digest: MessageDigest,
        pinnedKeys: Set<String>
    ): Boolean {
        val publicKey = certificate.publicKey.encoded
        digest.update(publicKey)
        val pin = Base64.encodeToString(digest.digest(), Base64.NO_WRAP)
        return pinnedKeys.contains(pin)
    }
}
