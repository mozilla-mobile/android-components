[android-components](../../index.md) / [mozilla.components.feature.syncedtabs](../index.md) / [SyncedTabsFeature](./index.md)

# SyncedTabsFeature

`@ExperimentalCoroutinesApi class SyncedTabsFeature` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/syncedtabs/src/main/java/mozilla/components/feature/syncedtabs/SyncedTabsFeature.kt#L28)

A feature that listens to the [BrowserStore](../../mozilla.components.browser.state.store/-browser-store/index.md) changes to update the local remote tabs state
in [RemoteTabsStorage](../../mozilla.components.browser.storage.sync/-remote-tabs-storage/index.md).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SyncedTabsFeature(accountManager: `[`FxaAccountManager`](../../mozilla.components.service.fxa.manager/-fxa-account-manager/index.md)`, store: `[`BrowserStore`](../../mozilla.components.browser.state.store/-browser-store/index.md)`, tabsStorage: `[`RemoteTabsStorage`](../../mozilla.components.browser.storage.sync/-remote-tabs-storage/index.md)` = RemoteTabsStorage())`<br>A feature that listens to the [BrowserStore](../../mozilla.components.browser.state.store/-browser-store/index.md) changes to update the local remote tabs state in [RemoteTabsStorage](../../mozilla.components.browser.storage.sync/-remote-tabs-storage/index.md). |

### Functions

| Name | Summary |
|---|---|
| [getSyncedTabs](get-synced-tabs.md) | `suspend fun getSyncedTabs(): `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`Device`](../../mozilla.components.concept.sync/-device/index.md)`, `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Tab`](../../mozilla.components.browser.storage.sync/-tab/index.md)`>>`<br>Get the list of remote tabs. |
| [start](start.md) | `fun start(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Start listening to browser store changes. |
| [stop](stop.md) | `fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop listening to browser store changes. |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
