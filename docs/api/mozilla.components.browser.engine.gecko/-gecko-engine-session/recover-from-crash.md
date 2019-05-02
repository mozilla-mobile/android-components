[android-components](../../index.md) / [mozilla.components.browser.engine.gecko](../index.md) / [GeckoEngineSession](index.md) / [recoverFromCrash](./recover-from-crash.md)

# recoverFromCrash

`fun recoverFromCrash(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/engine-gecko-beta/src/main/java/mozilla/components/browser/engine/gecko/GeckoEngineSession.kt#L270)

Overrides [EngineSession.recoverFromCrash](../../mozilla.components.concept.engine/-engine-session/recover-from-crash.md)

Tries to recover from a crash by restoring the last know state.

Returns true if a last known state was restored, otherwise false.

