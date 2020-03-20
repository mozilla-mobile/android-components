[android-components](../../index.md) / [mozilla.components.feature.customtabs](../index.md) / [CustomTabsToolbarFeature](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`CustomTabsToolbarFeature(sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`, toolbar: `[`BrowserToolbar`](../../mozilla.components.browser.toolbar/-browser-toolbar/index.md)`, sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null, menuBuilder: `[`BrowserMenuBuilder`](../../mozilla.components.browser.menu/-browser-menu-builder/index.md)`? = null, menuItemIndex: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = menuBuilder?.items?.size ?: 0, window: <ERROR CLASS>? = null, shareListener: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = null, closeListener: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`)`

Initializes and resets the Toolbar for a Custom Tab based on the CustomTabConfig.

### Parameters

`toolbar` - Reference to the browser toolbar, so that the color and menu items can be set.

`sessionId` - ID of the custom tab session. No-op if null or invalid.

`menuBuilder` - Menu builder reference to pull menu options from.

`menuItemIndex` - Location to insert any custom menu options into the predefined menu list.

`window` - Reference to the window so the navigation bar color can be set.

`shareListener` - Invoked when the share button is pressed.

`closeListener` - Invoked when the close button is pressed.