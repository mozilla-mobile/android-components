[android-components](../../index.md) / [mozilla.components.feature.session.bundling](../index.md) / [SessionBundle](index.md) / [restoreSnapshot](./restore-snapshot.md)

# restoreSnapshot

`abstract fun restoreSnapshot(engine: `[`Engine`](../../mozilla.components.concept.engine/-engine/index.md)`): `[`Snapshot`](../../mozilla.components.browser.session/-session-manager/-snapshot/index.md)`?` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/session-bundling/src/main/java/mozilla/components/feature/session/bundling/SessionBundle.kt#L33)

Restore a [SessionManager.Snapshot](../../mozilla.components.browser.session/-session-manager/-snapshot/index.md) from this bundle. The returned snapshot can be used with [SessionManager](../../mozilla.components.browser.session/-session-manager/index.md) to
restore the sessions and their state.

