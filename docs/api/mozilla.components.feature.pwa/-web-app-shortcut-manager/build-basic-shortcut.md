[android-components](../../index.md) / [mozilla.components.feature.pwa](../index.md) / [WebAppShortcutManager](index.md) / [buildBasicShortcut](./build-basic-shortcut.md)

# buildBasicShortcut

`suspend fun buildBasicShortcut(context: <ERROR CLASS>, session: `[`Session`](../../mozilla.components.browser.session/-session/index.md)`, overrideShortcutName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null): ShortcutInfoCompat` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/pwa/src/main/java/mozilla/components/feature/pwa/WebAppShortcutManager.kt#L108)

Create a new basic pinned website shortcut using info from the session.
Consuming `SHORTCUT_CATEGORY` in `AndroidManifest` is required for the package to be launched

