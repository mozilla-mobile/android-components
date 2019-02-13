[android-components](../../index.md) / [mozilla.components.lib.crash.service](../index.md) / [SentryService](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`SentryService(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`, dsn: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, tags: `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`> = emptyMap(), sendEventForNativeCrashes: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false, clientFactory: SentryClientFactory = AndroidSentryClientFactory(context))`

A [CrashReporterService](../-crash-reporter-service/index.md) implementation that uploads crash reports to a Sentry server.

### Parameters

`context` - The application [Context](https://developer.android.com/reference/android/content/Context.html).

`dsn` - Data Source Name of the Sentry server.

`tags` - A list of additional tags that will be sent together with crash reports.