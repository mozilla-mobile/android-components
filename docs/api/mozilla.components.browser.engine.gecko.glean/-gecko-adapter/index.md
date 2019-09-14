[android-components](../../index.md) / [mozilla.components.browser.engine.gecko.glean](../index.md) / [GeckoAdapter](./index.md)

# GeckoAdapter

`class GeckoAdapter` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/engine-gecko-beta/src/main/java/mozilla/components/browser/engine/gecko/glean/GeckoAdapter.kt#L21)

This implements a [RuntimeTelemetry.Delegate](#) that dispatches Gecko runtime
telemetry to the Glean SDK.

Metrics defined in the `metrics.yaml` file in Gecko's mozilla-central repository
will be automatically dispatched to the Glean SDK and sent through the requested
pings.

This can be used, in products collecting data through the Glean SDK, by
providing an instance to `GeckoRuntimeSettings.Builder().telemetryDelegate`.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `GeckoAdapter()`<br>This implements a [RuntimeTelemetry.Delegate](#) that dispatches Gecko runtime telemetry to the Glean SDK. |

### Functions

| Name | Summary |
|---|---|
| [onBooleanScalar](on-boolean-scalar.md) | `fun onBooleanScalar(metric: <ERROR CLASS><`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onHistogram](on-histogram.md) | `fun onHistogram(metric: <ERROR CLASS>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onLongScalar](on-long-scalar.md) | `fun onLongScalar(metric: <ERROR CLASS><`[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onStringScalar](on-string-scalar.md) | `fun onStringScalar(metric: <ERROR CLASS><`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onTelemetryReceived](on-telemetry-received.md) | `fun onTelemetryReceived(metric: <ERROR CLASS>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
