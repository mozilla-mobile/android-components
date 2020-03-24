[android-components](../../index.md) / [mozilla.components.support.sync.telemetry](../index.md) / [SyncTelemetry](index.md) / [processPasswordsPing](./process-passwords-ping.md)

# processPasswordsPing

`fun processPasswordsPing(ping: SyncTelemetryPing, sendPing: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = { Pings.loginsSync.submit() }): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/support/sync-telemetry/src/main/java/mozilla/components/support/sync/telemetry/SyncTelemetry.kt#L84)

Processes a passwords-related ping information from the [ping](process-passwords-ping.md#mozilla.components.support.sync.telemetry.SyncTelemetry$processPasswordsPing(mozilla.appservices.sync15.SyncTelemetryPing, kotlin.Function0((kotlin.Unit)))/ping).

**Return**
'false' if global error was encountered, 'true' otherwise.

