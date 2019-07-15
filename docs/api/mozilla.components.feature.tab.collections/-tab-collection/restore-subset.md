[android-components](../../index.md) / [mozilla.components.feature.tab.collections](../index.md) / [TabCollection](index.md) / [restoreSubset](./restore-subset.md)

# restoreSubset

`abstract fun restoreSubset(context: <ERROR CLASS>, engine: `[`Engine`](../../mozilla.components.concept.engine/-engine/index.md)`, tabs: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Tab`](../-tab/index.md)`>, restoreSessionId: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Session`](../../mozilla.components.browser.session/-session/index.md)`>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/tab-collections/src/main/java/mozilla/components/feature/tab/collections/TabCollection.kt#L50)

Restores a subset of the tabs in this collection and returns a matching list of [Session](../../mozilla.components.browser.session/-session/index.md) objects.

### Parameters

`restoreSessionId` - If true the original [Session.id](../../mozilla.components.browser.session/-session/id.md) of [Session](../../mozilla.components.browser.session/-session/index.md)s will be restored. Otherwise a new ID
will be generated. An app may prefer to use a new ID if it expects sessions to get restored multiple times -
otherwise breaking the promise of a unique ID per [Session](../../mozilla.components.browser.session/-session/index.md).