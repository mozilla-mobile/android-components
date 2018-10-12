/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.preparation;

import junit.framework.Assert;
import mozilla.components.browser.icons.IconDescriptor;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.Icons;
import mozilla.components.browser.icons.storage.FailureCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TestFilterKnownFailureUrls {
    private static final String TEST_PAGE_URL = "http://www.mozilla.org";
    private static final String TEST_ICON_URL = "https://example.org/favicon.ico";

    @Before
    public void setUp() {
        // Make sure we always start with an empty cache.
        FailureCache.get().evictAll();
    }

    @Test
    public void testFilterDoesNothingByDefault() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl(TEST_PAGE_URL)
                .icon(IconDescriptor.createGenericIcon(TEST_ICON_URL))
                .build();

        Assert.assertEquals(1, request.getIconCount());

        final Preparer preparer = new FilterKnownFailureUrls();
        preparer.prepare(request);

        Assert.assertEquals(1, request.getIconCount());
    }

    @Test
    public void testFilterKnownFailureUrls() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl(TEST_PAGE_URL)
                .icon(IconDescriptor.createGenericIcon(TEST_ICON_URL))
                .build();

        Assert.assertEquals(1, request.getIconCount());

        FailureCache.get().rememberFailure(TEST_ICON_URL);

        final Preparer preparer = new FilterKnownFailureUrls();
        preparer.prepare(request);

        Assert.assertEquals(0, request.getIconCount());
    }
}
