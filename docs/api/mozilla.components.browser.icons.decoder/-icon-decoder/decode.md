[android-components](../../index.md) / [mozilla.components.browser.icons.decoder](../index.md) / [IconDecoder](index.md) / [decode](./decode.md)

# decode

`abstract fun decode(data: `[`ByteArray`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)`, desiredSize: `[`DesiredSize`](../../mozilla.components.browser.icons/-desired-size/index.md)`): `[`Bitmap`](https://developer.android.com/reference/android/graphics/Bitmap.html)`?` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/icons/src/main/java/mozilla/components/browser/icons/decoder/IconDecoder.kt#L23)

Decodes the given [data](decode.md#mozilla.components.browser.icons.decoder.IconDecoder$decode(kotlin.ByteArray, mozilla.components.browser.icons.DesiredSize)/data) into a a [Bitmap](https://developer.android.com/reference/android/graphics/Bitmap.html) or null.

The caller provides a maximum size. This is useful for image formats that may decode into multiple images. The
decoder can use this size to determine which [Bitmap](https://developer.android.com/reference/android/graphics/Bitmap.html) to return.

