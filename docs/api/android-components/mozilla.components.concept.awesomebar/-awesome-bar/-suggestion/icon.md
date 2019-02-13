[android-components](../../../index.md) / [mozilla.components.concept.awesomebar](../../index.md) / [AwesomeBar](../index.md) / [Suggestion](index.md) / [icon](./icon.md)

# icon

`val icon: (width: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, height: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`) -> `[`Bitmap`](https://developer.android.com/reference/android/graphics/Bitmap.html)`?` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/awesomebar/src/main/java/mozilla/components/concept/awesomebar/AwesomeBar.kt#L75)

A lambda that can be invoked by the [AwesomeBar](../index.md) implementation to receive an icon [Bitmap](https://developer.android.com/reference/android/graphics/Bitmap.html) for
this [Suggestion](index.md). The [AwesomeBar](../index.md) will pass in its desired width and height for the Bitmap.

### Property

`icon` - A lambda that can be invoked by the [AwesomeBar](../index.md) implementation to receive an icon [Bitmap](https://developer.android.com/reference/android/graphics/Bitmap.html) for
this [Suggestion](index.md). The [AwesomeBar](../index.md) will pass in its desired width and height for the Bitmap.