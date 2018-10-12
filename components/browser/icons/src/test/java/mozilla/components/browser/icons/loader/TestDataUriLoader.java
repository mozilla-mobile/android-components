/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.loader;

import mozilla.components.browser.icons.IconDescriptor;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.IconResponse;
import mozilla.components.browser.icons.Icons;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TestDataUriLoader {
    @Test
    public void testNothingIsLoadedForHttpUrls() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl("http://www.mozilla.org")
                .icon(IconDescriptor.createGenericIcon(
                        "https://www.mozilla.org/media/img/favicon/apple-touch-icon-180x180.00050c5b754e.png"))
                .build();

        IconLoader loader = new DataUriLoader();
        IconResponse response = loader.load(request);

        Assert.assertNull(response);
    }

    @Test
    public void testIconIsLoadedFromDataUri() {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl("http://www.mozilla.org")
                .icon(IconDescriptor.createGenericIcon(
                        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEklEQVR4AWP4z8AAxCDiP8N/AB3wBPxcBee7AAAAAElFTkSuQmCC"))
                .build();

        IconLoader loader = new DataUriLoader();
        IconResponse response = loader.load(request);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getBitmap());
    }
}
