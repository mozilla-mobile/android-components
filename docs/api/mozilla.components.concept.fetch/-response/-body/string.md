[android-components](../../../index.md) / [mozilla.components.concept.fetch](../../index.md) / [Response](../index.md) / [Body](index.md) / [string](./string.md)

# string

`fun string(charset: `[`Charset`](https://developer.android.com/reference/java/nio/charset/Charset.html)` = Charsets.UTF_8): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/fetch/src/main/java/mozilla/components/concept/fetch/Response.kt#L76)

Reads this body completely as a String.

Takes care of closing the body down correctly whether an exception is thrown or not.

