[android-components](../../index.md) / [mozilla.components.feature.customtabs.store](../index.md) / [CustomTabState](./index.md)

# CustomTabState

`data class CustomTabState` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/customtabs/src/main/java/mozilla/components/feature/customtabs/store/CustomTabsServiceState.kt#L30)

Value type that represents the state of a single custom tab
accessible from both the service and activity.

This data is meant to supplement [mozilla.components.browser.session.tab.CustomTabConfig](#),
not replace it. It only contains data that the service also needs to work with.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `CustomTabState(creatorPackageName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null, relationships: `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`OriginRelationPair`](../-origin-relation-pair/index.md)`, `[`VerificationStatus`](../-verification-status/index.md)`> = emptyMap())`<br>Value type that represents the state of a single custom tab accessible from both the service and activity. |

### Properties

| Name | Summary |
|---|---|
| [creatorPackageName](creator-package-name.md) | `val creatorPackageName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?`<br>Package name of the app that created the custom tab. |
| [relationships](relationships.md) | `val relationships: `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`OriginRelationPair`](../-origin-relation-pair/index.md)`, `[`VerificationStatus`](../-verification-status/index.md)`>`<br>Map of origin and relationship type to current verification state. |

### Extension Properties

| Name | Summary |
|---|---|
| [trustedOrigins](../../mozilla.components.feature.pwa.ext/trusted-origins.md) | `val `[`CustomTabState`](./index.md)`.trustedOrigins: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Nothing`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing/index.html)`>`<br>Returns a list of trusted (or pending) origins. |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
