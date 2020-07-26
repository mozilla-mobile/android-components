[android-components](../../index.md) / [mozilla.components.browser.menu2](../index.md) / [BrowserMenuController](index.md) / [show](./show.md)

# show

`fun show(anchor: <ERROR CLASS>): <ERROR CLASS>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/menu2/src/main/java/mozilla/components/browser/menu2/BrowserMenuController.kt#L39)

Overrides [MenuController.show](../../mozilla.components.concept.menu/-menu-controller/show.md)

### Parameters

`anchor` - The view on which to pin the popup window.`fun show(anchor: <ERROR CLASS>, orientation: `[`Orientation`](../../mozilla.components.concept.menu/-orientation/index.md)`? = null, @Px width: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = anchor.resources.getDimensionPixelSize(R.dimen.mozac_browser_menu2_width)): <ERROR CLASS>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/menu2/src/main/java/mozilla/components/browser/menu2/BrowserMenuController.kt#L46)

### Parameters

`anchor` - The view on which to pin the popup window.

`orientation` - The preferred orientation to show the popup window.

`width` - The width of the popup menu. The height is always set to wrap content.