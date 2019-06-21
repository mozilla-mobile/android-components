[android-components](../../index.md) / [mozilla.components.browser.icons.decoder](../index.md) / [AndroidIconDecoder](./index.md)

# AndroidIconDecoder

`class AndroidIconDecoder : `[`IconDecoder`](../-icon-decoder/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/icons/src/main/java/mozilla/components/browser/icons/decoder/AndroidIconDecoder.kt#L19)

[IconDecoder](../-icon-decoder/index.md) that will use Android's [BitmapFactory](https://developer.android.com/reference/android/graphics/BitmapFactory.html) in order to decode the byte data.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `AndroidIconDecoder(ignoreSize: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false)`<br>[IconDecoder](../-icon-decoder/index.md) that will use Android's [BitmapFactory](https://developer.android.com/reference/android/graphics/BitmapFactory.html) in order to decode the byte data. |

### Functions

| Name | Summary |
|---|---|
| [decode](decode.md) | `fun decode(data: `[`ByteArray`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)`, desiredSize: `[`DesiredSize`](../../mozilla.components.browser.icons/-desired-size/index.md)`): `[`Bitmap`](https://developer.android.com/reference/android/graphics/Bitmap.html)`?`<br>Decodes the given [data](../-icon-decoder/decode.md#mozilla.components.browser.icons.decoder.IconDecoder$decode(kotlin.ByteArray, mozilla.components.browser.icons.DesiredSize)/data) into a a [Bitmap](https://developer.android.com/reference/android/graphics/Bitmap.html) or null. |
