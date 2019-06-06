/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.base.observer

import android.view.View
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.util.Collections
import java.util.WeakHashMap

/**
 * A helper for classes that want to get observed. This class keeps track of registered observers
 * and can automatically unregister observers if a LifecycleOwner is provided.
 */
class ObserverRegistry<T> : Observable<T> {
    private val observers = mutableSetOf<T>()
    private val lifecycleObservers = WeakHashMap<T, LifecycleBoundObserver<T>>()
    private val viewObservers = WeakHashMap<T, ViewBoundObserver<T>>()
    private val pausedObservers = Collections.newSetFromMap(WeakHashMap<T, Boolean>())

    /**
     * Registers an observer to get notified about changes. Does nothing if [observer] is already registered.
     * This method is thread-safe.
     *
     * @param observer the observer to register.
     */
    @Synchronized
    override fun register(observer: T) {
        observers.add(observer)
    }

    @Synchronized
    override fun register(observer: T, owner: LifecycleOwner, autoPause: Boolean) {
        // Don't register if the owner is already destroyed
        if (owner.lifecycle.currentState == DESTROYED) {
            return
        }

        register(observer)

        val lifecycleObserver = if (autoPause) {
            AutoPauseLifecycleBoundObserver(owner, registry = this, observer = observer)
        } else {
            LifecycleBoundObserver(owner, registry = this, observer = observer)
        }

        lifecycleObservers[observer] = lifecycleObserver

        owner.lifecycle.addObserver(lifecycleObserver)
    }

    @Synchronized
    override fun register(observer: T, view: View) {
        val viewObserver = ViewBoundObserver(
                view,
                registry = this,
                observer = observer)

        viewObservers[observer] = viewObserver

        view.addOnAttachStateChangeListener(viewObserver)

        if (view.isAttachedToWindow) {
            register(observer)
        }
    }

    /**
     * Unregisters an observer. Does nothing if [observer] is not registered.
     * This method is thread-safe.
     *
     * @param observer the observer to unregister.
     */
    @Synchronized
    override fun unregister(observer: T) {
        // Remove observer
        observers.remove(observer)
        pausedObservers.remove(observer)

        // Unregister lifecycle/view observers
        lifecycleObservers[observer]?.remove()
        viewObservers[observer]?.remove()

        // Remove lifecyle/view observers from map
        lifecycleObservers.remove(observer)
        viewObservers.remove(observer)
    }

    @Synchronized
    override fun unregisterObservers() {
        observers.forEach {
            lifecycleObservers[it]?.remove()
        }

        observers.clear()
        pausedObservers.clear()
        lifecycleObservers.clear()
        viewObservers.clear()
    }

    @Synchronized
    override fun pauseObserver(observer: T) {
        pausedObservers.add(observer)
    }

    @Synchronized
    override fun resumeObserver(observer: T) {
        pausedObservers.remove(observer)
    }

    @Synchronized
    override fun notifyObservers(block: T.() -> Unit) {
        observers.forEach {
            if (!pausedObservers.contains(it)) {
                it.block()
            }
        }
    }

    @Synchronized
    override fun <V> wrapConsumers(block: T.(V) -> Boolean): List<(V) -> Boolean> {
        val consumers: MutableList<(V) -> Boolean> = mutableListOf()

        observers.forEach { observer ->
            consumers.add { value -> observer.block(value) }
        }

        return consumers
    }

    @Synchronized
    override fun isObserved(): Boolean {
        return !observers.isEmpty()
    }

    /**
     * LifecycleObserver implementation to bind an observer to a Lifecycle.
     */
    private open class LifecycleBoundObserver<T>(
        private val owner: LifecycleOwner,
        protected val registry: ObserverRegistry<T>,
        protected val observer: T
    ) : LifecycleObserver {
        @OnLifecycleEvent(ON_DESTROY)
        fun onDestroy() = registry.unregister(observer)

        fun remove() = owner.lifecycle.removeObserver(this)
    }

    /**
     * LifecycleObserver implementation to bind an observer to a Lifecycle and pause observing
     * automatically for the pause and resume events.
     */
    private class AutoPauseLifecycleBoundObserver<T>(
        owner: LifecycleOwner,
        registry: ObserverRegistry<T>,
        observer: T
    ) : LifecycleBoundObserver<T>(owner, registry, observer) {
        init {
            if (!owner.lifecycle.currentState.isAtLeast(RESUMED)) {
                registry.pauseObserver(observer)
            }
        }

        @OnLifecycleEvent(ON_PAUSE)
        fun onPause() = registry.pauseObserver(observer)

        @OnLifecycleEvent(ON_RESUME)
        fun onResume() = registry.resumeObserver(observer)
    }

    /**
     * View.OnAttachStateChangeListener implementation to automatically unregister an observer if
     * the bound view gets detached.
     */
    private class ViewBoundObserver<T>(
        private val view: View,
        private val registry: ObserverRegistry<T>,
        private val observer: T
    ) : View.OnAttachStateChangeListener {
        override fun onViewDetachedFromWindow(view: View) {
            registry.unregister(observer)
        }

        fun remove() {
            view.removeOnAttachStateChangeListener(this)
        }

        override fun onViewAttachedToWindow(view: View) {
            registry.register(observer)
        }
    }
}
