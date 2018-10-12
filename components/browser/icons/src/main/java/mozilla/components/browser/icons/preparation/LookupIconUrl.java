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

import mozilla.components.browser.icons.IconDescriptor;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.storage.MemoryStorage;

/**
 * Preparer implementation to lookup the icon URL for the page URL in the request. This class tries
 * to locate the icon URL by looking through previously stored mappings on disk and in memory.
 */
public class LookupIconUrl implements Preparer {
    @Override
    public void prepare(IconRequest request) {
        if (lookupFromMemory(request)) {
            return;
        }
    }

    private boolean lookupFromMemory(IconRequest request) {
        final String iconUrl = MemoryStorage.get()
                .getMapping(request.getPageUrl());

        if (iconUrl != null) {
            request.modify()
                    .icon(IconDescriptor.createLookupIcon(iconUrl))
                    .deferBuild();

            return true;
        }

        return false;
    }
}
