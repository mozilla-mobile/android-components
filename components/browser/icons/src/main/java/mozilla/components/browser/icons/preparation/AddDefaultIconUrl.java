/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons.preparation;

import android.text.TextUtils;
import mozilla.components.browser.icons.IconDescriptor;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.IconsHelper;
import mozilla.components.support.ktx.kotlin.StringKt;

/**
 * Preparer to add the "default/guessed" favicon URL (domain/favicon.ico) to the list of URLs to
 * try loading the favicon from.
 *
 * The default URL will be added with a very low priority so that we will only try to load from this
 * URL if all other options failed.
 */
public class AddDefaultIconUrl implements Preparer {
    @Override
    public void prepare(IconRequest request) {
        if (!StringKt.isHttpOrHttps(request.getPageUrl())) {
            return;
        }

        final String defaultFaviconUrl = IconsHelper.guessDefaultFaviconURL(request.getPageUrl());
        if (TextUtils.isEmpty(defaultFaviconUrl)) {
            // We couldn't generate a default favicon URL for this URL. Nothing to do here.
            return;
        }

        request.modify()
                .icon(IconDescriptor.createGenericIcon(defaultFaviconUrl))
                .deferBuild();
    }
}
