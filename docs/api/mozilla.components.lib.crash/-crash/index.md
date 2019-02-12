[android-components](../../index.md) / [mozilla.components.lib.crash](../index.md) / [Crash](./index.md)

# Crash

`sealed class Crash` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/lib/crash/src/main/java/mozilla/components/lib/crash/Crash.kt#L26)

Crash types that are handled by this library.

### Types

| Name | Summary |
|---|---|
| [NativeCodeCrash](-native-code-crash/index.md) | `data class NativeCodeCrash : `[`Crash`](./index.md)<br>A crash that happened in native code. |
| [UncaughtExceptionCrash](-uncaught-exception-crash/index.md) | `data class UncaughtExceptionCrash : `[`Crash`](./index.md)<br>A crash caused by an uncaught exception. |

### Companion Object Functions

| Name | Summary |
|---|---|
| [fromIntent](from-intent.md) | `fun fromIntent(intent: `[`Intent`](https://developer.android.com/reference/android/content/Intent.html)`): `[`Crash`](./index.md) |
| [isCrashIntent](is-crash-intent.md) | `fun isCrashIntent(intent: `[`Intent`](https://developer.android.com/reference/android/content/Intent.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [NativeCodeCrash](-native-code-crash/index.md) | `data class NativeCodeCrash : `[`Crash`](./index.md)<br>A crash that happened in native code. |
| [UncaughtExceptionCrash](-uncaught-exception-crash/index.md) | `data class UncaughtExceptionCrash : `[`Crash`](./index.md)<br>A crash caused by an uncaught exception. |
