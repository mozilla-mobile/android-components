package mozilla.components.service.sync.places

//import mozilla.components.service.sync.places.PlacesAwesomeBarProvider
import org.junit.Test

import org.junit.Assert.*

import android.content.Context;

import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

import org.junit.rules.TemporaryFolder;
import java.io.File

class PlacesAwesomeBarProviderTest {
    @Test
    fun autocomplete_is_correct()
    {
        /* markh doesn't know what he is doing here :)
        val folder = TemporaryFolder()
        folder.create()
        val dbFile = File(folder.root.name + "/places.sqlite")

        val context = mock(Context::class.java)
        `when`(context.getDatabasePath("places.sqlite")).thenReturn(dbFile)

        val provider = PlacesAwesomeBarProvider(context)
        assertEquals(provider.getSuggestion("foo"), null)
        */
    }
}