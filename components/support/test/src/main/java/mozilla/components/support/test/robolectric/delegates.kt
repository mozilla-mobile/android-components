package mozilla.components.support.test.robolectric

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal class ApplicationContextProvider<T : Context> : ReadOnlyProperty<Any?, T> {

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        ApplicationProvider.getApplicationContext()
}

/**
 * Delegate for providing application context
 */
fun applicationContext(): ReadOnlyProperty<Any?, Context> = ApplicationContextProvider()
