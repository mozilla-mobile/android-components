[android-components](../../index.md) / [mozilla.components.browser.tabstray](../index.md) / [TabTouchCallback](./index.md)

# TabTouchCallback

`open class TabTouchCallback : SimpleCallback` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/tabstray/src/main/java/mozilla/components/browser/tabstray/TabTouchCallback.kt#L18)

An [ItemTouchHelper.Callback](#) for support gestures on tabs in the tray.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `TabTouchCallback(observable: `[`Observable`](../../mozilla.components.support.base.observer/-observable/index.md)`<`[`Observer`](../../mozilla.components.concept.tabstray/-tabs-tray/-observer/index.md)`>)`<br>An [ItemTouchHelper.Callback](#) for support gestures on tabs in the tray. |

### Functions

| Name | Summary |
|---|---|
| [alphaForItemSwipe](alpha-for-item-swipe.md) | `open fun alphaForItemSwipe(dX: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`, distanceToAlphaMin: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)<br>Sets the alpha value for a swipe gesture. This is useful for inherited classes to provide their own values. |
| [onChildDraw](on-child-draw.md) | `open fun onChildDraw(c: `[`Canvas`](https://developer.android.com/reference/android/graphics/Canvas.html)`, recyclerView: RecyclerView, viewHolder: ViewHolder, dX: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`, dY: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`, actionState: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, isCurrentlyActive: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onMove](on-move.md) | `open fun onMove(p0: RecyclerView, p1: ViewHolder, p2: ViewHolder): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onSwiped](on-swiped.md) | `open fun onSwiped(viewHolder: ViewHolder, direction: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
