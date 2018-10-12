/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.processing;

import android.graphics.Bitmap;
import mozilla.components.browser.icons.IconDescriptor;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.IconResponse;
import mozilla.components.browser.icons.Icons;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class TestResizingProcessor {
    private static final String PAGE_URL = "https://www.mozilla.org";
    private static final String ICON_URL = "https://www.mozilla.org/favicon.ico";

    @Test
    public void testBitmapIsNotResizedIfItAlreadyHasTheTargetSize() {
        final IconRequest request = createTestRequest();

        final Bitmap bitmap = createBitmapMock(request.getTargetSize());
        final IconResponse response = spy(IconResponse.create(bitmap));

        final ResizingProcessor processor = spy(new ResizingProcessor());
        processor.process(request, response);

        verify(processor, never()).resize(any(Bitmap.class), anyInt());
        verify(bitmap, never()).recycle();
        verify(response, never()).updateBitmap(any(Bitmap.class));
    }

    @Test
    public void testLargerBitmapsAreResized() {
        final IconRequest request = createTestRequest();

        final Bitmap bitmap = createBitmapMock(request.getTargetSize() * 2);
        final IconResponse response = spy(IconResponse.create(bitmap));

        final ResizingProcessor processor = spy(new ResizingProcessor());
        final Bitmap resizedBitmap = mock(Bitmap.class);
        doReturn(resizedBitmap).when(processor).resize(any(Bitmap.class), anyInt());
        processor.process(request, response);

        verify(processor).resize(bitmap, request.getTargetSize());
        verify(bitmap).recycle();
        verify(response).updateBitmap(resizedBitmap);
    }

    @Test
    public void testBitmapIsUpscaledToTargetSize() {
        final IconRequest request = createTestRequest();

        final Bitmap bitmap = createBitmapMock(request.getTargetSize() / 2 + 1);
        final IconResponse response = spy(IconResponse.create(bitmap));

        final ResizingProcessor processor = spy(new ResizingProcessor());
        final Bitmap resizedBitmap = mock(Bitmap.class);
        doReturn(resizedBitmap).when(processor).resize(any(Bitmap.class), anyInt());
        processor.process(request, response);

        verify(processor).resize(bitmap, request.getTargetSize());
        verify(bitmap).recycle();
        verify(response).updateBitmap(resizedBitmap);
    }

    @Test
    public void testBitmapIsNotScaledMoreThanMaxScaleFactor() {
        final IconRequest request = createTestRequest();

        final int initialSize = 5;
        final Bitmap bitmap = createBitmapMock(initialSize);
        final IconResponse response = spy(IconResponse.create(bitmap));

        final ResizingProcessor processor = spy(new ResizingProcessor());
        final Bitmap resizedBitmap = mock(Bitmap.class);
        doReturn(resizedBitmap).when(processor).resize(any(Bitmap.class), anyInt());
        processor.process(request, response);

        verify(processor).resize(bitmap, initialSize * ResizingProcessor.MAX_SCALE_FACTOR);
        verify(bitmap).recycle();
        verify(response).updateBitmap(resizedBitmap);
    }

    private IconRequest createTestRequest() {
        return Icons.with(RuntimeEnvironment.application)
                .pageUrl(PAGE_URL)
                .icon(IconDescriptor.createGenericIcon(ICON_URL))
                .build();
    }

    private Bitmap createBitmapMock(int size) {
        final Bitmap bitmap = mock(Bitmap.class);

        doReturn(size).when(bitmap).getWidth();
        doReturn(size).when(bitmap).getHeight();

        return bitmap;
    }
}
