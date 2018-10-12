/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons.loader;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.IconResponse;
import mozilla.components.browser.icons.decoders.FaviconDecoder;
import mozilla.components.browser.icons.decoders.LoadFaviconResult;

/**
 * Loader for loading icons from a content provider. This loader was primarily written to load icons
 * from the partner bookmarks provider. However it can load icons from arbitrary content providers
 * as long as they return a cursor with a "favicon" or "touchicon" column (blob).
 */
public class ContentProviderLoader implements IconLoader {
    private static final String TOUCHICON = "touchicon";
    private static final String FAVICON = "favicon";

    @Override
    public IconResponse load(IconRequest request) {
        if (request.shouldSkipDisk()) {
            // If we should not load data from disk then we do not load from content providers either.
            return null;
        }

        final String iconUrl = request.getBestIcon().getUrl();
        final Context context = request.getContext();
        final int targetSize = request.getTargetSize();

        if (TextUtils.isEmpty(iconUrl) || !iconUrl.startsWith("content://")) {
            return null;
        }

        Cursor cursor = context.getContentResolver().query(
                Uri.parse(iconUrl),
                new String[] {
                        TOUCHICON,
                        FAVICON,
                },
                null,
                null,
                null
        );

        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            // Try the touch icon first. It has a higher resolution usually.
            Bitmap icon = decodeFromCursor(request.getContext(), cursor, TOUCHICON, targetSize);
            if (icon != null) {
                return IconResponse.create(icon);
            }

            icon = decodeFromCursor(request.getContext(), cursor, FAVICON, targetSize);
            if (icon != null) {
                return IconResponse.create(icon);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    private Bitmap decodeFromCursor(Context context, Cursor cursor, String column, int targetWidthAndHeight) {
        final int index = cursor.getColumnIndex(column);
        if (index == -1) {
            return null;
        }

        if (cursor.isNull(index)) {
            return null;
        }

        final byte[] data = cursor.getBlob(index);
        LoadFaviconResult result = FaviconDecoder.decodeFavicon(context, data, 0, data.length);
        if (result == null) {
            return null;
        }

        return result.getBestBitmap(targetWidthAndHeight);
    }
}
