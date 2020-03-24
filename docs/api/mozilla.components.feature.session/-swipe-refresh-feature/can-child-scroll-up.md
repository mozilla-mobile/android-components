[android-components](../../index.md) / [mozilla.components.feature.session](../index.md) / [SwipeRefreshFeature](index.md) / [canChildScrollUp](./can-child-scroll-up.md)

# canChildScrollUp

`fun canChildScrollUp(parent: SwipeRefreshLayout, child: <ERROR CLASS>?): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/session/src/main/java/mozilla/components/feature/session/SwipeRefreshFeature.kt#L44)

Callback that checks whether it is possible for the child view to scroll up.
If the child view cannot scroll up, attempted to scroll up triggers a refresh gesture.

