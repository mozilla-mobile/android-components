[android-components](../../index.md) / [mozilla.components.browser.state.state](../index.md) / [CustomTabConfig](./index.md)

# CustomTabConfig

`data class CustomTabConfig` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/state/src/main/java/mozilla/components/browser/state/state/CustomTabConfig.kt#L30)

Holds configuration data for a Custom Tab.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `CustomTabConfig(id: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = UUID.randomUUID().toString(), toolbarColor: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`? = null, closeButtonIcon: <ERROR CLASS>? = null, enableUrlbarHiding: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false, actionButtonConfig: `[`CustomTabActionButtonConfig`](../-custom-tab-action-button-config/index.md)`? = null, showShareMenuItem: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false, menuItems: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`CustomTabMenuItem`](../-custom-tab-menu-item/index.md)`> = emptyList(), exitAnimations: <ERROR CLASS>? = null, navigationBarColor: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`? = null, titleVisible: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false, sessionToken: CustomTabsSessionToken? = null)`<br>Holds configuration data for a Custom Tab. |

### Properties

| Name | Summary |
|---|---|
| [actionButtonConfig](action-button-config.md) | `val actionButtonConfig: `[`CustomTabActionButtonConfig`](../-custom-tab-action-button-config/index.md)`?`<br>Custom action button on the toolbar. |
| [closeButtonIcon](close-button-icon.md) | `val closeButtonIcon: <ERROR CLASS>?`<br>Custom icon of the back button on the toolbar. |
| [enableUrlbarHiding](enable-urlbar-hiding.md) | `val enableUrlbarHiding: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Enables the toolbar to hide as the user scrolls down on the page. |
| [exitAnimations](exit-animations.md) | `val exitAnimations: <ERROR CLASS>?`<br>Bundle containing custom exit animations for the tab. |
| [id](id.md) | `val id: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>a unique ID of this custom tab. |
| [menuItems](menu-items.md) | `val menuItems: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`CustomTabMenuItem`](../-custom-tab-menu-item/index.md)`>`<br>Custom overflow menu items. |
| [navigationBarColor](navigation-bar-color.md) | `val navigationBarColor: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`?`<br>Background color for the navigation bar. |
| [sessionToken](session-token.md) | `val sessionToken: CustomTabsSessionToken?`<br>The token associated with the custom tab. |
| [showShareMenuItem](show-share-menu-item.md) | `val showShareMenuItem: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Specifies whether a default share button will be shown in the menu. |
| [titleVisible](title-visible.md) | `val titleVisible: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Whether the title should be shown in the custom tab. |
| [toolbarColor](toolbar-color.md) | `val toolbarColor: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`?`<br>Background color for the toolbar. |

### Companion Object Properties

| Name | Summary |
|---|---|
| [EXTRA_ADDITIONAL_TRUSTED_ORIGINS](-e-x-t-r-a_-a-d-d-i-t-i-o-n-a-l_-t-r-u-s-t-e-d_-o-r-i-g-i-n-s.md) | `const val EXTRA_ADDITIONAL_TRUSTED_ORIGINS: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [EXTRA_NAVIGATION_BAR_COLOR](-e-x-t-r-a_-n-a-v-i-g-a-t-i-o-n_-b-a-r_-c-o-l-o-r.md) | `const val EXTRA_NAVIGATION_BAR_COLOR: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
