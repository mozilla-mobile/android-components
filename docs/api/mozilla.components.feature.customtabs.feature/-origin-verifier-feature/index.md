[android-components](../../index.md) / [mozilla.components.feature.customtabs.feature](../index.md) / [OriginVerifierFeature](./index.md)

# OriginVerifierFeature

`class OriginVerifierFeature` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/customtabs/src/main/java/mozilla/components/feature/customtabs/feature/OriginVerifierFeature.kt#L22)

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `OriginVerifierFeature(packageManager: <ERROR CLASS>, relationChecker: `[`RelationChecker`](../../mozilla.components.service.digitalassetlinks/-relation-checker/index.md)`, dispatch: (`[`CustomTabsAction`](../../mozilla.components.feature.customtabs.store/-custom-tabs-action/index.md)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`)` |

### Functions

| Name | Summary |
|---|---|
| [verify](verify.md) | `suspend fun verify(state: `[`CustomTabState`](../../mozilla.components.feature.customtabs.store/-custom-tab-state/index.md)`, token: CustomTabsSessionToken, relation: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, origin: <ERROR CLASS>): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
