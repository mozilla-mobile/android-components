/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.processing;

import android.graphics.Bitmap;
import android.graphics.Color;
import mozilla.components.browser.icons.IconResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class TestColorProcessor {
    @Test
    public void testExtractingColor() {
        final IconResponse response = IconResponse.create(createRedBitmapMock());

        Assert.assertFalse(response.hasColor());
        Assert.assertEquals(0, response.getColor());

        final Processor processor = new ColorProcessor();
        processor.process(null, response);

        Assert.assertTrue(response.hasColor());
        Assert.assertEquals(Color.RED, response.getColor());
    }

    private Bitmap createRedBitmapMock() {
        final Bitmap bitmap = mock(Bitmap.class);

        doReturn(1).when(bitmap).getWidth();
        doReturn(1).when(bitmap).getHeight();

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                int[] pixels = (int[]) args[0];
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = Color.RED;
                }
                return null;
            }
        }).when(bitmap).getPixels(any(int[].class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        return bitmap;
    }
}
