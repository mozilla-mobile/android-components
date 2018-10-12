/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.processing;

import android.graphics.Bitmap;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.IconResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class TestMinimumSizeProcessor {

    private MinimumSizeProcessor processor;

    @Before
    public void setUp() {
        processor = new MinimumSizeProcessor();
    }

    @Test
    public void testProcessMinimumSizeZeroDoesNotReplaceSmallBitmap() throws Exception {
        final IconResponse responseMock = getMockResponse(1);
        processor.process(getMockRequest(0), responseMock);

        verify(responseMock, never()).updateBitmap(any(Bitmap.class));
        verify(responseMock, never()).updateColor(anyInt());
    }

    @Test
    public void testProcessMinimumSizeZeroDoesNotReplaceLargeBitmap() throws Exception {
        final IconResponse responseMock = getMockResponse(1000);
        processor.process(getMockRequest(0), responseMock);

        verify(responseMock, never()).updateBitmap(any(Bitmap.class));
        verify(responseMock, never()).updateColor(anyInt());
    }

    @Test
    public void testProcessMinimumSizeFiftyReplacesSmallerBitmap() throws Exception {
        final IconResponse responseMock = getMockResponse(25);
        processor.process(getMockRequest(50), responseMock);

        verify(responseMock, atLeastOnce()).updateBitmap(any(Bitmap.class));
        verify(responseMock, atLeastOnce()).updateColor(anyInt());
    }

    @Test
    public void testProcessMinimumSizeFiftyDoesNotReplaceLargerBitmap() throws Exception {
        final IconResponse responseMock = getMockResponse(1000);
        processor.process(getMockRequest(50), responseMock);

        verify(responseMock, never()).updateBitmap(any(Bitmap.class));
        verify(responseMock, never()).updateColor(anyInt());
    }

    private IconRequest getMockRequest(final int minimumSizePx) {
        final IconRequest requestMock = mock(IconRequest.class);

        // Under testing.
        when(requestMock.getMinimumSizePxAfterScaling()).thenReturn(minimumSizePx);

        // Happened to be called.
        when(requestMock.getPageUrl()).thenReturn("https://mozilla.org");
        when(requestMock.getContext()).thenReturn(RuntimeEnvironment.application);
        return requestMock;
    }

    private IconResponse getMockResponse(final int bitmapWidth) {
        final Bitmap bitmapMock = mock(Bitmap.class);
        when(bitmapMock.getWidth()).thenReturn(bitmapWidth);
        when(bitmapMock.getHeight()).thenReturn(bitmapWidth); // not strictly necessary with the current impl.

        final IconResponse responseMock = mock(IconResponse.class);
        when(responseMock.getBitmap()).thenReturn(bitmapMock);
        return responseMock;
    }
}