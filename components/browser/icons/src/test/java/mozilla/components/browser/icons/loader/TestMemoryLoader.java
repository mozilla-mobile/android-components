/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.loader;

import android.graphics.Bitmap;
import android.graphics.Color;
import junit.framework.Assert;
import mozilla.components.browser.icons.IconDescriptor;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.IconResponse;
import mozilla.components.browser.icons.Icons;
import mozilla.components.browser.icons.storage.MemoryStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class TestMemoryLoader {
    private static final String TEST_PAGE_URL = "http://www.mozilla.org";
    private static final String TEST_ICON_URL = "https://example.org/favicon.ico";

    @Before
    public void setUp() {
        // Make sure to start with an empty memory cache.
        MemoryStorage.get().evictAll();
    }

    @Test
    public void testStoringAndLoadingFromMemory() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl(TEST_PAGE_URL)
                .icon(IconDescriptor.createGenericIcon(TEST_ICON_URL))
                .build();

        final IconLoader loader = new MemoryLoader();

        Assert.assertNull(loader.load(request));

        final Bitmap bitmap = mock(Bitmap.class);
        final IconResponse response = IconResponse.create(bitmap);
        response.updateColor(Color.MAGENTA);

        MemoryStorage.get().putIcon(TEST_ICON_URL, response);

        final IconResponse loadedResponse = loader.load(request);

        Assert.assertNotNull(loadedResponse);
        Assert.assertEquals(bitmap, loadedResponse.getBitmap());
        Assert.assertEquals(Color.MAGENTA, loadedResponse.getColor());
    }

    @Test
    public void testNothingIsLoadedIfMemoryShouldBeSkipped() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl(TEST_PAGE_URL)
                .icon(IconDescriptor.createGenericIcon(TEST_ICON_URL))
                .skipMemory()
                .build();

        final IconLoader loader = new MemoryLoader();

        Assert.assertNull(loader.load(request));

        final Bitmap bitmap = mock(Bitmap.class);
        final IconResponse response = IconResponse.create(bitmap);

        MemoryStorage.get().putIcon(TEST_ICON_URL, response);

        Assert.assertNull(loader.load(request));
    }
}
