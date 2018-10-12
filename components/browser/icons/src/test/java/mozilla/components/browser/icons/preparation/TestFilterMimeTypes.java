/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.preparation;

import mozilla.components.browser.icons.IconDescriptor;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.Icons;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TestFilterMimeTypes {
    private static final String TEST_PAGE_URL = "http://www.mozilla.org";
    private static final String TEST_ICON_URL = "https://example.org/favicon.ico";
    private static final String TEST_ICON_URL_2 = "https://mozilla.org/favicon.ico";

    @Test
    public void testUrlsWithoutMimeTypesAreNotFiltered() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl(TEST_PAGE_URL)
                .icon(IconDescriptor.createGenericIcon(TEST_ICON_URL))
                .build();

        Assert.assertEquals(1, request.getIconCount());

        final Preparer preparer = new FilterMimeTypes();
        preparer.prepare(request);

        Assert.assertEquals(1, request.getIconCount());
    }

    @Test
    public void testUnknownMimeTypesAreFiltered() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl(TEST_PAGE_URL)
                .icon(IconDescriptor.createFavicon(TEST_ICON_URL, 256, "image/zaphod"))
                .icon(IconDescriptor.createFavicon(TEST_ICON_URL_2, 128, "audio/mpeg"))
                .build();

        Assert.assertEquals(2, request.getIconCount());

        final Preparer preparer = new FilterMimeTypes();
        preparer.prepare(request);

        Assert.assertEquals(0, request.getIconCount());
    }

    @Test
    public void testKnownMimeTypesAreNotFiltered() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl(TEST_PAGE_URL)
                .icon(IconDescriptor.createFavicon(TEST_ICON_URL, 256, "image/x-icon"))
                .icon(IconDescriptor.createFavicon(TEST_ICON_URL_2, 128, "image/png"))
                .build();

        Assert.assertEquals(2, request.getIconCount());

        final Preparer preparer = new FilterMimeTypes();
        preparer.prepare(request);

        Assert.assertEquals(2, request.getIconCount());
    }
}
