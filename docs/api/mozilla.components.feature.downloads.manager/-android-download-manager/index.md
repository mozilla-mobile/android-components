[android-components](../../index.md) / [mozilla.components.feature.downloads.manager](../index.md) / [AndroidDownloadManager](./index.md)

# AndroidDownloadManager

`class AndroidDownloadManager : `[`DownloadManager`](../-download-manager/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/downloads/src/main/java/mozilla/components/feature/downloads/manager/AndroidDownloadManager.kt#L37)

Handles the interactions with the [AndroidDownloadManager](./index.md).

### Types

| Name | Summary |
|---|---|
| [DownloadStateWithRequest](-download-state-with-request/index.md) | `data class DownloadStateWithRequest`<br>Holds both the state and the Android DownloadManager.Request for the queued download |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `AndroidDownloadManager(applicationContext: <ERROR CLASS>, onDownloadStopped: `[`onDownloadStopped`](../on-download-stopped.md)` = noop)`<br>Handles the interactions with the [AndroidDownloadManager](./index.md). |

### Properties

| Name | Summary |
|---|---|
| [onDownloadStopped](on-download-stopped.md) | `var onDownloadStopped: `[`onDownloadStopped`](../on-download-stopped.md) |
| [permissions](permissions.md) | `val permissions: <ERROR CLASS>` |

### Functions

| Name | Summary |
|---|---|
| [download](download.md) | `fun download(download: `[`DownloadState`](../../mozilla.components.browser.state.state.content/-download-state/index.md)`, cookie: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`?`<br>Schedules a download through the [AndroidDownloadManager](./index.md). |
| [onReceive](on-receive.md) | `fun onReceive(context: <ERROR CLASS>, intent: <ERROR CLASS>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Invoked when a download is complete. Calls [onDownloadStopped](../on-download-stopped.md) and unregisters the broadcast receiver if there are no more queued downloads. |
| [tryAgain](try-again.md) | `fun tryAgain(downloadId: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Schedules another attempt at downloading the given download. |
| [unregisterListeners](unregister-listeners.md) | `fun unregisterListeners(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Remove all the listeners. |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
| [validatePermissionGranted](../validate-permission-granted.md) | `fun `[`DownloadManager`](../-download-manager/index.md)`.validatePermissionGranted(context: <ERROR CLASS>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
