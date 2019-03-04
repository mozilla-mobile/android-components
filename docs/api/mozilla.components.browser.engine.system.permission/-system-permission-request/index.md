[android-components](../../index.md) / [mozilla.components.browser.engine.system.permission](../index.md) / [SystemPermissionRequest](./index.md)

# SystemPermissionRequest

`class SystemPermissionRequest : `[`PermissionRequest`](../../mozilla.components.concept.engine.permission/-permission-request/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/engine-system/src/main/java/mozilla/components/browser/engine/system/permission/SystemPermissionRequest.kt#L18)

WebView-based implementation of [PermissionRequest](../../mozilla.components.concept.engine.permission/-permission-request/index.md).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SystemPermissionRequest(nativeRequest: `[`PermissionRequest`](https://developer.android.com/reference/android/webkit/PermissionRequest.html)`)`<br>WebView-based implementation of [PermissionRequest](../../mozilla.components.concept.engine.permission/-permission-request/index.md). |

### Properties

| Name | Summary |
|---|---|
| [permissions](permissions.md) | `val permissions: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Permission`](../../mozilla.components.concept.engine.permission/-permission/index.md)`>`<br>List of requested permissions. |
| [uri](uri.md) | `val uri: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>The origin URI which caused the permissions to be requested. |

### Functions

| Name | Summary |
|---|---|
| [grant](grant.md) | `fun grant(permissions: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Permission`](../../mozilla.components.concept.engine.permission/-permission/index.md)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Grants the provided permissions, or all requested permissions, if none are provided. |
| [reject](reject.md) | `fun reject(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Rejects the requested permissions. |

### Inherited Functions

| Name | Summary |
|---|---|
| [containsVideoAndAudioSources](../../mozilla.components.concept.engine.permission/-permission-request/contains-video-and-audio-sources.md) | `open fun containsVideoAndAudioSources(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [grantIf](../../mozilla.components.concept.engine.permission/-permission-request/grant-if.md) | `open fun grantIf(predicate: (`[`Permission`](../../mozilla.components.concept.engine.permission/-permission/index.md)`) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Grants this permission request if the provided predicate is true for any of the requested permissions. |

### Companion Object Properties

| Name | Summary |
|---|---|
| [permissionsMap](permissions-map.md) | `val permissionsMap: `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`Permission`](../../mozilla.components.concept.engine.permission/-permission/index.md)`>` |
