package mozilla.components.support.test.robolectric

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface DelegatedReadOnlyProperty<T> : ReadOnlyProperty<Any?, T> {

    fun get(): T
}

internal class ApplicationContextProvider<T : Context> : DelegatedReadOnlyProperty<T> {

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        get()

    override fun get(): T = ApplicationProvider.getApplicationContext()
}

/**
 * Delegate for providing application context
 */
fun applicationContext(): DelegatedReadOnlyProperty<Context> = ApplicationContextProvider()
