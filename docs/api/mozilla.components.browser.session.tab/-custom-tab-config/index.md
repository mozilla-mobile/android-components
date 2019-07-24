[android-components](../../index.md) / [mozilla.components.browser.session.tab](../index.md) / [CustomTabConfig](./index.md)

# CustomTabConfig

`data class CustomTabConfig` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/session/src/main/java/mozilla/components/browser/session/tab/CustomTabConfig.kt#L31)

Holds configuration data for a Custom Tab.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `CustomTabConfig(id: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, toolbarColor: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`?, closeButtonIcon: <ERROR CLASS>?, enableUrlbarHiding: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`, actionButtonConfig: `[`CustomTabActionButtonConfig`](../-custom-tab-action-button-config/index.md)`?, showShareMenuItem: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`, menuItems: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`CustomTabMenuItem`](../-custom-tab-menu-item/index.md)`> = emptyList(), exitAnimations: <ERROR CLASS>? = null, titleVisible: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false)`<br>Holds configuration data for a Custom Tab. |

### Properties

| Name | Summary |
|---|---|
| [actionButtonConfig](action-button-config.md) | `val actionButtonConfig: `[`CustomTabActionButtonConfig`](../-custom-tab-action-button-config/index.md)`?`<br>Custom action button on the toolbar. |
| [closeButtonIcon](close-button-icon.md) | `val closeButtonIcon: <ERROR CLASS>?`<br>Custom icon of the back button on the toolbar. |
| [disableUrlbarHiding](disable-urlbar-hiding.md) | `val disableUrlbarHiding: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [enableUrlbarHiding](enable-urlbar-hiding.md) | `val enableUrlbarHiding: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Enables the toolbar to hide as the user scrolls down on the page. |
| [exitAnimations](exit-animations.md) | `val exitAnimations: <ERROR CLASS>?`<br>Bundle containing custom exit animations for the tab. |
| [id](id.md) | `val id: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [menuItems](menu-items.md) | `val menuItems: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`CustomTabMenuItem`](../-custom-tab-menu-item/index.md)`>`<br>Custom overflow menu items. |
| [options](options.md) | `val options: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [showShareMenuItem](show-share-menu-item.md) | `val showShareMenuItem: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Specifies whether a default share button will be shown in the menu. |
| [titleVisible](title-visible.md) | `val titleVisible: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Whether the title should be shown in the custom tab. |
| [toolbarColor](toolbar-color.md) | `val toolbarColor: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`?`<br>Background color for the toolbar. |

### Companion Object Functions

| Name | Summary |
|---|---|
| [createFromIntent](create-from-intent.md) | `fun ~~createFromIntent~~(intent: `[`SafeIntent`](../../mozilla.components.support.utils/-safe-intent/index.md)`, displayMetrics: <ERROR CLASS>? = null): `[`CustomTabConfig`](./index.md)<br>Creates a CustomTabConfig instance based on the provided intent. |
| [isCustomTabIntent](is-custom-tab-intent.md) | `fun ~~isCustomTabIntent~~(intent: `[`SafeIntent`](../../mozilla.components.support.utils/-safe-intent/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Checks if the provided intent is a custom tab intent. |
