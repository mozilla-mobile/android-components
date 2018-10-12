/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons.processing;


import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.IconResponse;

/**
 * Generic interface for a class that processes a response object after an icon has been loaded and
 * decoded. A class implementing this interface can attach additional data to the response or modify
 * the bitmap (e.g. resizing).
 */
public interface Processor {
    /**
     * Process a response object containing an icon loaded for this request.
     */
    void process(IconRequest request, IconResponse response);
}
