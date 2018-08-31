---
title: Observable.register - 
---

[mozilla.components.support.base.observer](../index.html) / [Observable](index.html) / [register](./register.html)

# register

`abstract fun register(observer: `[`T`](index.html#T)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Registers an observer to get notified about changes.

`abstract fun register(observer: `[`T`](index.html#T)`, owner: LifecycleOwner): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Registers an observer to get notified about changes.

The observer will automatically unsubscribe if the lifecycle of the provided LifecycleOwner
becomes DESTROYED.

`abstract fun register(observer: `[`T`](index.html#T)`, view: View): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Registers an observer to get notified about changes.

The observer will automatically unsubscribe if the provided view gets detached.

