[android-components](../../index.md) / [mozilla.components.feature.media](../index.md) / [MediaFeature](./index.md)

# MediaFeature

`class MediaFeature` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/media/src/main/java/mozilla/components/feature/media/MediaFeature.kt#L25)

Feature implementation for media playback in web content. This feature takes care of:

* Background playback without the app getting killed.
* Showing a media notification with play/pause controls.
* Audio Focus handling (pausing/resuming in agreement with other media apps).
* Support for hardware controls to toggle play/pause (e.g. buttons on a headset)

This feature should get initialized globally once on app start and requires a started
[MediaStateMachine](../../mozilla.components.feature.media.state/-media-state-machine/index.md).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `MediaFeature(context: <ERROR CLASS>)`<br>Feature implementation for media playback in web content. This feature takes care of: |

### Functions

| Name | Summary |
|---|---|
| [enable](enable.md) | `fun enable(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Enables the feature. |

### Companion Object Properties

| Name | Summary |
|---|---|
| [ACTION_SWITCH_TAB](-a-c-t-i-o-n_-s-w-i-t-c-h_-t-a-b.md) | `const val ACTION_SWITCH_TAB: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [EXTRA_TAB_ID](-e-x-t-r-a_-t-a-b_-i-d.md) | `const val EXTRA_TAB_ID: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [NOTIFICATION_TAG](-n-o-t-i-f-i-c-a-t-i-o-n_-t-a-g.md) | `const val NOTIFICATION_TAG: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [PENDING_INTENT_TAG](-p-e-n-d-i-n-g_-i-n-t-e-n-t_-t-a-g.md) | `const val PENDING_INTENT_TAG: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
