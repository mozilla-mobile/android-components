[android-components](../index.md) / [mozilla.components.feature.pwa.ext](./index.md)

## Package mozilla.components.feature.pwa.ext

### Functions

| Name | Summary |
|---|---|
| [applyOrientation](apply-orientation.md) | `fun <ERROR CLASS>.applyOrientation(manifest: `[`WebAppManifest`](../mozilla.components.concept.engine.manifest/-web-app-manifest/index.md)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Sets the requested orientation of the [Activity](#) to the orientation provided by the given [WebAppManifest](../mozilla.components.concept.engine.manifest/-web-app-manifest/index.md) (See [WebAppManifest.orientation](../mozilla.components.concept.engine.manifest/-web-app-manifest/orientation.md) and [WebAppManifest.Orientation](../mozilla.components.concept.engine.manifest/-web-app-manifest/-orientation/index.md). |
| [getWebAppManifest](get-web-app-manifest.md) | `fun <ERROR CLASS>.getWebAppManifest(): `[`WebAppManifest`](../mozilla.components.concept.engine.manifest/-web-app-manifest/index.md)`?`<br>Parses and returns the [WebAppManifest](../mozilla.components.concept.engine.manifest/-web-app-manifest/index.md) associated with this [Bundle](#), or null if no mapping of the desired type exists. |
| [installableManifest](installable-manifest.md) | `fun `[`Session`](../mozilla.components.browser.session/-session/index.md)`.installableManifest(): `[`WebAppManifest`](../mozilla.components.concept.engine.manifest/-web-app-manifest/index.md)`?`<br>Checks if the current session represents an installable web app. If so, return the web app manifest. Otherwise, return null. |
| [putWebAppManifest](put-web-app-manifest.md) | `fun <ERROR CLASS>.putWebAppManifest(webAppManifest: `[`WebAppManifest`](../mozilla.components.concept.engine.manifest/-web-app-manifest/index.md)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Serializes and inserts a [WebAppManifest](../mozilla.components.concept.engine.manifest/-web-app-manifest/index.md) value into the mapping of this [Bundle](#), replacing any existing web app manifest. |
| [toCustomTabConfig](to-custom-tab-config.md) | `fun `[`WebAppManifest`](../mozilla.components.concept.engine.manifest/-web-app-manifest/index.md)`.toCustomTabConfig(): `[`CustomTabConfig`](../mozilla.components.browser.session.tab/-custom-tab-config/index.md) |
| [toTaskDescription](to-task-description.md) | `fun `[`WebAppManifest`](../mozilla.components.concept.engine.manifest/-web-app-manifest/index.md)`.toTaskDescription(icon: <ERROR CLASS>?): <ERROR CLASS>`<br>Create a [TaskDescription](#) for the activity manager based on the manifest. |
