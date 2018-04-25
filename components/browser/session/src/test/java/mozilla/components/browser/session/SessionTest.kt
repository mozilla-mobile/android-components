/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class SessionTest {
    @Test
    fun `registered observers get notified`() {
        val observer = mock(Session.Observer::class.java)

        val session = Session("https://www.mozilla.org")
        session.register(observer)

        session.notifyObservers { onUrlChanged() }

        verify(observer).onUrlChanged()
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `observer is not notified after unregistering`() {
        val observer = mock(Session.Observer::class.java)

        val session = Session("https://www.mozilla.org")
        session.register(observer)

        session.notifyObservers { onUrlChanged() }

        verify(observer).onUrlChanged()
        verifyNoMoreInteractions(observer)

        reset(observer)

        session.unregister(observer)
        session.notifyObservers { onUrlChanged() }

        verify(observer, never()).onUrlChanged()
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `observer is notified when URL changes`() {
        val observer = mock(Session.Observer::class.java)

        val session = Session("https://www.mozilla.org")
        session.register(observer)

        session.url = "http://www.firefox.com"

        assertEquals("http://www.firefox.com", session.url)
        verify(observer).onUrlChanged()
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `observer is notified when progress changes`() {
        val observer = mock(Session.Observer::class.java)

        val session = Session("https://www.mozilla.org")
        session.register(observer)

        session.progress = 75

        assertEquals(75, session.progress)
        verify(observer).onProgress()
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `observer is notified when loading state changes`() {
        val observer = mock(Session.Observer::class.java)

        val session = Session("https://www.mozilla.org")
        session.register(observer)

        session.loading = true

        assertEquals(true, session.loading)
        verify(observer).onLoadingStateChanged()
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `observer is notified when navigation state changes`() {
        val observer = mock(Session.Observer::class.java)

        val session = Session("https://www.mozilla.org")
        session.register(observer)

        session.canGoBack = true
        assertEquals(true, session.canGoBack)
        verify(observer).onNavigationStateChanged()

        session.canGoForward = true
        assertEquals(true, session.canGoForward)
        verify(observer, times(2)).onNavigationStateChanged()

        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `observer is not notified when property is set but hasn't changed`() {
        val observer = mock(Session.Observer::class.java)

        val session = Session("https://www.mozilla.org")
        session.register(observer)

        session.url = "https://www.mozilla.org"

        assertEquals("https://www.mozilla.org", session.url)
        verify(observer, never()).onUrlChanged()
        verifyNoMoreInteractions(observer)
    }

    @Test
    fun `Session returns initial URL`() {
        val session = Session("https://www.mozilla.org")

        assertEquals("https://www.mozilla.org", session.url)
    }
}
