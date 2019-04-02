[android-components](../../index.md) / [mozilla.components.support.ktx.android.view](../index.md) / [android.view.MotionEvent](index.md) / [use](./use.md)

# use

`inline fun <R> `[`MotionEvent`](https://developer.android.com/reference/android/view/MotionEvent.html)`.use(functionBlock: (`[`MotionEvent`](https://developer.android.com/reference/android/view/MotionEvent.html)`) -> `[`R`](use.md#R)`): `[`R`](use.md#R) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/support/ktx/src/main/java/mozilla/components/support/ktx/android/view/MotionEvent.kt#L13)

Executes the given [functionBlock](use.md#mozilla.components.support.ktx.android.view$use(android.view.MotionEvent, kotlin.Function1((android.view.MotionEvent, mozilla.components.support.ktx.android.view.use.R)))/functionBlock) function on this resource and then closes it down correctly whether
an exception is thrown or not. This is inspired by [java.lang.AutoCloseable.use](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/use.html).

