/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

/**
 * A class describing the location and properties of an icon that can be loaded.
 */
public class IconDescriptor {
    @IntDef({ TYPE_GENERIC, TYPE_FAVICON, TYPE_TOUCHICON, TYPE_LOOKUP, TYPE_BUNDLED_TILE })
    @interface IconType {}

    // The type values are used for ranking icons (higher values = try to load first).
    @VisibleForTesting
    static final int TYPE_GENERIC = 0;
    @VisibleForTesting
    static final int TYPE_LOOKUP = 1;
    @VisibleForTesting
    static final int TYPE_FAVICON = 5;
    @VisibleForTesting
    static final int TYPE_TOUCHICON = 10;
    @VisibleForTesting
    static final int TYPE_BUNDLED_TILE = 15;

    private final String url;
    private final int size;
    private final String mimeType;
    private final int type;

    /**
     * Create a generic icon located at the given URL. No MIME type or size is known.
     */
    public static IconDescriptor createGenericIcon(@NonNull String url) {
        return new IconDescriptor(TYPE_GENERIC, url, 0, null);
    }

    /**
     * Create a favicon located at the given URL and with a known size and MIME type.
     */
    public static IconDescriptor createFavicon(@NonNull String url, int size, String mimeType) {
        return new IconDescriptor(TYPE_FAVICON, url, size, mimeType);
    }

    /**
     * Create a touch icon located at the given URL and with a known MIME type and size.
     */
    public static IconDescriptor createTouchicon(@NonNull String url, int size, String mimeType) {
        return new IconDescriptor(TYPE_TOUCHICON, url, size, mimeType);
    }

    /**
     * Create an icon located at an URL that has been returned from a disk or memory storage. This
     * is an icon with an URL we loaded an icon from previously. Therefore we give it a little higher
     * ranking than a generic icon - even though we do not know the MIME type or size of the icon.
     */
    public static IconDescriptor createLookupIcon(@NonNull String url) {
        return new IconDescriptor(TYPE_LOOKUP, url, 0, null);
    }

    /**
     * Create a bundled tile icon at the given URL. MIME type or size is not known until we load
     * the icons, but we know these icons are high fidelity. (Although the icons are png's at time
     * of writing, they could be changed to webp or VectorDrawable in future.)
     */
    public static IconDescriptor createBundledTileIcon(@NonNull String url) {
        return new IconDescriptor(TYPE_BUNDLED_TILE, url, 0, null);
    }


    private IconDescriptor(@IconType int type, @NonNull String url, int size, String mimeType) {
        this.type = type;
        this.url = url;
        this.size = size;
        this.mimeType = mimeType;
    }

    /**
     * Get the URL of the icon.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the (assumed) size of the icon. Returns 0 if no size is known.
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the type of the icon (favicon, touch icon, generic, lookup).
     */
    @IconType
    public int getType() {
        return type;
    }

    /**
     * Get the (assumed) MIME type of the icon. Returns null if no MIME type is known.
     */
    @Nullable
    public String getMimeType() {
        return mimeType;
    }
}
