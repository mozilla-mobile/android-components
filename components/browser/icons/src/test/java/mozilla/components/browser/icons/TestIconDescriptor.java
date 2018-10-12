/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TestIconDescriptor {
    private static final String ICON_URL = "https://www.mozilla.org/favicon.ico";
    private static final String MIME_TYPE = "image/png";
    private static final int ICON_SIZE = 64;

    @Test
    public void testGenericIconDescriptor() {
        final IconDescriptor descriptor = IconDescriptor.createGenericIcon(ICON_URL);

        Assert.assertEquals(ICON_URL, descriptor.getUrl());
        Assert.assertNull(descriptor.getMimeType());
        Assert.assertEquals(0, descriptor.getSize());
        Assert.assertEquals(IconDescriptor.TYPE_GENERIC, descriptor.getType());
    }

    @Test
    public void testFaviconIconDescriptor() {
        final IconDescriptor descriptor = IconDescriptor.createFavicon(ICON_URL, ICON_SIZE, MIME_TYPE);

        Assert.assertEquals(ICON_URL, descriptor.getUrl());
        Assert.assertEquals(MIME_TYPE, descriptor.getMimeType());
        Assert.assertEquals(ICON_SIZE, descriptor.getSize());
        Assert.assertEquals(IconDescriptor.TYPE_FAVICON, descriptor.getType());
    }

    @Test
    public void testTouchIconDescriptor() {
        final IconDescriptor descriptor = IconDescriptor.createTouchicon(ICON_URL, ICON_SIZE, MIME_TYPE);

        Assert.assertEquals(ICON_URL, descriptor.getUrl());
        Assert.assertEquals(MIME_TYPE, descriptor.getMimeType());
        Assert.assertEquals(ICON_SIZE, descriptor.getSize());
        Assert.assertEquals(IconDescriptor.TYPE_TOUCHICON, descriptor.getType());
    }

    @Test
    public void testLookupIconDescriptor() {
        final IconDescriptor descriptor = IconDescriptor.createLookupIcon(ICON_URL);

        Assert.assertEquals(ICON_URL, descriptor.getUrl());
        Assert.assertNull(descriptor.getMimeType());
        Assert.assertEquals(0, descriptor.getSize());
        Assert.assertEquals(IconDescriptor.TYPE_LOOKUP, descriptor.getType());
    }

    @Test
    public void testBundledTileIconDescriptor() {
        final IconDescriptor descriptor = IconDescriptor.createBundledTileIcon(ICON_URL);

        Assert.assertEquals(ICON_URL, descriptor.getUrl());
        Assert.assertNull(descriptor.getMimeType());
        Assert.assertEquals(0, descriptor.getSize());
        Assert.assertEquals(IconDescriptor.TYPE_BUNDLED_TILE, descriptor.getType());
    }
}
