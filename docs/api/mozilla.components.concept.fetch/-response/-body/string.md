[android-components](../../../index.md) / [mozilla.components.concept.fetch](../../index.md) / [Response](../index.md) / [Body](index.md) / [string](./string.md)

# string

`fun string(charset: `[`Charset`](https://developer.android.com/reference/java/nio/charset/Charset.html)`? = null): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/fetch/src/main/java/mozilla/components/concept/fetch/Response.kt#L104)

Reads this body completely as a String.

Takes care of closing the body down correctly whether an exception is thrown or not.

### Parameters

`charset` - the optional charset to use when decoding the body. If not specified,
the charset provided in the response content-type header will be used. If the header
is missing or the charset not supported, UTF-8 will be used.