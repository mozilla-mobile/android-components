/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.preparation;

import mozilla.components.browser.icons.IconDescriptor;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.support.ktx.kotlin.StringKt;

import java.util.Iterator;

/**
 * Filter non http/https URLs if the request is not from privileged code.
 */
public class FilterPrivilegedUrls implements Preparer {
    @Override
    public void prepare(IconRequest request) {
        if (request.isPrivileged()) {
            // This request is privileged. No need to filter anything.
            return;
        }

        final Iterator<IconDescriptor> iterator = request.getIconIterator();

        while (iterator.hasNext()) {
            IconDescriptor descriptor = iterator.next();

            if (!StringKt.isHttpOrHttps(descriptor.getUrl())) {
                iterator.remove();
            }
        }
    }
}
