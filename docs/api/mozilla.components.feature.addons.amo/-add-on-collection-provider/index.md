[android-components](../../index.md) / [mozilla.components.feature.addons.amo](../index.md) / [AddOnCollectionProvider](./index.md)

# AddOnCollectionProvider

`class AddOnCollectionProvider : `[`AddOnsProvider`](../../mozilla.components.feature.addons/-add-ons-provider/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/addons/src/main/java/mozilla/components/feature/addons/amo/AddOnCollectionProvider.kt#L46)

Provide access to the collections AMO API.
https://addons-server.readthedocs.io/en/latest/topics/api/collections.html

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `AddOnCollectionProvider(context: <ERROR CLASS>, client: `[`Client`](../../mozilla.components.concept.fetch/-client/index.md)`, serverURL: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = DEFAULT_SERVER_URL, collectionName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = DEFAULT_COLLECTION_NAME, maxCacheAgeInMinutes: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = -1)`<br>Provide access to the collections AMO API. https://addons-server.readthedocs.io/en/latest/topics/api/collections.html |

### Functions

| Name | Summary |
|---|---|
| [getAddOnIconBitmap](get-add-on-icon-bitmap.md) | `suspend fun getAddOnIconBitmap(addOn: `[`AddOn`](../../mozilla.components.feature.addons/-add-on/index.md)`): <ERROR CLASS>?`<br>Fetches given AddOn icon from the url and returns a decoded Bitmap |
| [getAvailableAddOns](get-available-add-ons.md) | `suspend fun getAvailableAddOns(allowCache: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`AddOn`](../../mozilla.components.feature.addons/-add-on/index.md)`>`<br>Interacts with the collections endpoint to provide a list of available add-ons. May return a cached response, if available, not expired (see [maxCacheAgeInMinutes](#)) and allowed (see [allowCache](get-available-add-ons.md#mozilla.components.feature.addons.amo.AddOnCollectionProvider$getAvailableAddOns(kotlin.Boolean)/allowCache)). |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
