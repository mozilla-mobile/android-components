/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

// This code is based on AOSP /libcore/luni/src/main/java/java/net/ProxySelectorImpl.java

package mozilla.components.browser.icons.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;

public class ProxySelector {
    public static URLConnection openConnectionWithProxy(URI uri) throws IOException {
        java.net.ProxySelector ps = java.net.ProxySelector.getDefault();
        Proxy proxy = Proxy.NO_PROXY;
        if (ps != null) {
            List<Proxy> proxies = ps.select(uri);
            if (proxies != null && !proxies.isEmpty()) {
                proxy = proxies.get(0);
            }
        }

        return uri.toURL().openConnection(proxy);
    }

    public ProxySelector() {
    }

    public Proxy select(String scheme, String host) {
        int port = -1;
        Proxy proxy = null;
        String nonProxyHostsKey = null;
        boolean httpProxyOkay = true;
        if ("http".equalsIgnoreCase(scheme)) {
            port = 80;
            nonProxyHostsKey = "http.nonProxyHosts";
            proxy = lookupProxy("http.proxyHost", "http.proxyPort", Proxy.Type.HTTP, port);
        } else if ("https".equalsIgnoreCase(scheme)) {
            port = 443;
            nonProxyHostsKey = "https.nonProxyHosts"; // RI doesn't support this
            proxy = lookupProxy("https.proxyHost", "https.proxyPort", Proxy.Type.HTTP, port);
        } else if ("ftp".equalsIgnoreCase(scheme)) {
            port = 80; // not 21 as you might guess
            nonProxyHostsKey = "ftp.nonProxyHosts";
            proxy = lookupProxy("ftp.proxyHost", "ftp.proxyPort", Proxy.Type.HTTP, port);
        } else if ("socket".equalsIgnoreCase(scheme)) {
            httpProxyOkay = false;
        } else {
            return Proxy.NO_PROXY;
        }

        if (nonProxyHostsKey != null
                && isNonProxyHost(host, System.getProperty(nonProxyHostsKey))) {
            return Proxy.NO_PROXY;
        }

        if (proxy != null) {
            return proxy;
        }

        if (httpProxyOkay) {
            proxy = lookupProxy("proxyHost", "proxyPort", Proxy.Type.HTTP, port);
            if (proxy != null) {
                return proxy;
            }
        }

        proxy = lookupProxy("socksProxyHost", "socksProxyPort", Proxy.Type.SOCKS, 1080);
        if (proxy != null) {
            return proxy;
        }

        return Proxy.NO_PROXY;
    }

    /**
     * Returns the proxy identified by the {@code hostKey} system property, or
     * null.
     */
    @Nullable
    private Proxy lookupProxy(String hostKey, String portKey, Proxy.Type type, int defaultPort) {
        final String host = System.getProperty(hostKey);
        if (TextUtils.isEmpty(host)) {
            return null;
        }

        final int port = getSystemPropertyInt(portKey, defaultPort);
        if (port == -1) {
            // Port can be -1. See bug 1270529.
            return null;
        }

        return new Proxy(type, InetSocketAddress.createUnresolved(host, port));
    }

    private int getSystemPropertyInt(String key, int defaultValue) {
        String string = System.getProperty(key);
        if (string != null) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * Returns true if the {@code nonProxyHosts} system property pattern exists
     * and matches {@code host}.
     */
    private boolean isNonProxyHost(String host, String nonProxyHosts) {
        if (host == null || nonProxyHosts == null) {
            return false;
        }

        // construct pattern
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < nonProxyHosts.length(); i++) {
            char c = nonProxyHosts.charAt(i);
            switch (c) {
            case '.':
                patternBuilder.append("\\.");
                break;
            case '*':
                patternBuilder.append(".*");
                break;
            default:
                patternBuilder.append(c);
            }
        }
        // check whether the host is the nonProxyHosts.
        String pattern = patternBuilder.toString();
        return host.matches(pattern);
    }
}

