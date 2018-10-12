/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.preparation;

import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.Icons;
import mozilla.components.browser.icons.storage.MemoryStorage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TestLookupIconUrl {
    private static final String TEST_PAGE_URL = "http://www.mozilla.org";

    private static final String TEST_ICON_URL_1 = "http://www.mozilla.org/favicon.ico";
    private static final String TEST_ICON_URL_2 = "http://example.org/favicon.ico";
    private static final String TEST_ICON_URL_3 = "http://example.com/favicon.ico";
    private static final String TEST_ICON_URL_4 = "http://example.net/favicon.ico";


    @Before
    public void setUp() {
        MemoryStorage.get().evictAll();
    }

    @Test
    public void testNoIconUrlIsAddedByDefault() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl(TEST_PAGE_URL)
                .build();

        Assert.assertEquals(0, request.getIconCount());

        Preparer preparer = new LookupIconUrl();
        preparer.prepare(request);

        Assert.assertEquals(0, request.getIconCount());
    }

    @Test
    public void testIconUrlIsAddedFromMemory() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl(TEST_PAGE_URL)
                .build();

        MemoryStorage.get().putMapping(request, TEST_ICON_URL_1);

        Assert.assertEquals(0, request.getIconCount());

        Preparer preparer = new LookupIconUrl();
        preparer.prepare(request);

        Assert.assertEquals(1, request.getIconCount());

        Assert.assertEquals(TEST_ICON_URL_1, request.getBestIcon().getUrl());
    }
}
