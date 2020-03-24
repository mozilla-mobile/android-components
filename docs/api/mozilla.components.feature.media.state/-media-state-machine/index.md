[android-components](../../index.md) / [mozilla.components.feature.media.state](../index.md) / [MediaStateMachine](./index.md)

# MediaStateMachine

`object MediaStateMachine : `[`Observable`](../../mozilla.components.support.base.observer/-observable/index.md)`<`[`Observer`](-observer/index.md)`>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/media/src/main/java/mozilla/components/feature/media/state/MediaStateMachine.kt#L33)

A state machine that subscribes to all [Session](../../mozilla.components.browser.session/-session/index.md) instances and watches changes to their [Media](../../mozilla.components.concept.engine.media/-media/index.md) to create an
aggregated [MediaState](../-media-state/index.md).

Other components can subscribe to the state machine to get notified about [MediaState](../-media-state/index.md) changes.

### Types

| Name | Summary |
|---|---|
| [Observer](-observer/index.md) | `interface Observer`<br>Interface for observers that are interested in [MediaState](../-media-state/index.md) changes. |

### Properties

| Name | Summary |
|---|---|
| [state](state.md) | `var state: `[`MediaState`](../-media-state/index.md)<br>The current [MediaState](../-media-state/index.md). |

### Functions

| Name | Summary |
|---|---|
| [reset](reset.md) | `fun reset(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Resets the [MediaState](../-media-state/index.md) to [MediaState.None](../-media-state/-none/index.md). |
| [start](start.md) | `fun start(sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Start observing [Session](../../mozilla.components.browser.session/-session/index.md) and their [Media](../../mozilla.components.concept.engine.media/-media/index.md) and create an aggregated [MediaState](../-media-state/index.md) that can be observed. |
| [stop](stop.md) | `fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop observing [Session](../../mozilla.components.browser.session/-session/index.md) and their [Media](../../mozilla.components.concept.engine.media/-media/index.md). |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
