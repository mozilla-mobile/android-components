[android-components](../../index.md) / [mozilla.components.feature.customtabs.store](../index.md) / [CustomTabsAction](./index.md)

# CustomTabsAction

`sealed class CustomTabsAction : `[`Action`](../../mozilla.components.lib.state/-action.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/customtabs/src/main/java/mozilla/components/feature/customtabs/store/CustomTabsAction.kt#L12)

### Properties

| Name | Summary |
|---|---|
| [token](token.md) | `abstract val token: CustomTabsSessionToken` |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |

### Inheritors

| Name | Summary |
|---|---|
| [SaveCreatorPackageNameAction](../-save-creator-package-name-action/index.md) | `data class SaveCreatorPackageNameAction : `[`CustomTabsAction`](./index.md)<br>Saves the package name corresponding to a custom tab token. |
| [ValidateRelationshipAction](../-validate-relationship-action/index.md) | `data class ValidateRelationshipAction : `[`CustomTabsAction`](./index.md)<br>Marks the state of a custom tabs [Relation](#) verification. |
