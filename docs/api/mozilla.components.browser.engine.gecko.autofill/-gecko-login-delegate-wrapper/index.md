[android-components](../../index.md) / [mozilla.components.browser.engine.gecko.autofill](../index.md) / [GeckoLoginDelegateWrapper](./index.md)

# GeckoLoginDelegateWrapper

`class GeckoLoginDelegateWrapper : `[`Delegate`](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/LoginStorage/Delegate.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/engine-gecko-nightly/src/main/java/mozilla/components/browser/engine/gecko/autofill/GeckoLoginDelegateWrapper.kt#L17)

This class exists only to convert incoming [LoginEntry](#) arguments into [Login](../../mozilla.components.concept.storage/-login/index.md)s, then forward
them to [storageDelegate](#). This allows us to avoid duplicating [LoginStorageDelegate](../../mozilla.components.concept.storage/-login-storage-delegate/index.md) code
between different versions of GeckoView, by duplicating this wrapper instead.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `GeckoLoginDelegateWrapper(storageDelegate: `[`LoginStorageDelegate`](../../mozilla.components.concept.storage/-login-storage-delegate/index.md)`)`<br>This class exists only to convert incoming [LoginEntry](#) arguments into [Login](../../mozilla.components.concept.storage/-login/index.md)s, then forward them to [storageDelegate](#). This allows us to avoid duplicating [LoginStorageDelegate](../../mozilla.components.concept.storage/-login-storage-delegate/index.md) code between different versions of GeckoView, by duplicating this wrapper instead. |

### Functions

| Name | Summary |
|---|---|
| [onLoginFetch](on-login-fetch.md) | `fun onLoginFetch(domain: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`GeckoResult`](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoResult.html)`<`[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`LoginEntry`](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/LoginStorage/LoginEntry.html)`>>?` |
| [onLoginSave](on-login-save.md) | `fun onLoginSave(login: `[`LoginEntry`](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/LoginStorage/LoginEntry.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onLoginUsed](on-login-used.md) | `fun onLoginUsed(login: `[`LoginEntry`](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/LoginStorage/LoginEntry.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>This method has not yet been implemented in GV. Once it has, we should add an override to it here. |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
