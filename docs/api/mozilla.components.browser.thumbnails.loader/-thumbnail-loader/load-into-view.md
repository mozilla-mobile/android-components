[android-components](../../index.md) / [mozilla.components.browser.thumbnails.loader](../index.md) / [ThumbnailLoader](index.md) / [loadIntoView](./load-into-view.md)

# loadIntoView

`fun loadIntoView(view: <ERROR CLASS>, id: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, placeholder: <ERROR CLASS>?, error: <ERROR CLASS>?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/thumbnails/src/main/java/mozilla/components/browser/thumbnails/loader/ThumbnailLoader.kt#L26)

Overrides [ImageLoader.loadIntoView](../../mozilla.components.support.images.loader/-image-loader/load-into-view.md)

Loads an image asynchronously and then displays it in the [ImageView](#).
If the view is detached from the window before loading is completed, then loading is cancelled.

### Parameters

`view` - [ImageView](#) to load the image into.

`id` - Load image for this given ID.

`placeholder` - [Drawable](#) to display while image is loading.

`error` - [Drawable](#) to display if loading fails.