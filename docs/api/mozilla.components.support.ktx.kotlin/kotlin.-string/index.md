[android-components](../../index.md) / [mozilla.components.support.ktx.kotlin](../index.md) / [kotlin.String](./index.md)

### Extensions for kotlin.String

| Name | Summary |
|---|---|
| [isEmail](is-email.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isEmail(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isExtensionUrl](is-extension-url.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isExtensionUrl(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Checks if this String is a URL of an extension page. |
| [isGeoLocation](is-geo-location.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isGeoLocation(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isPhone](is-phone.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isPhone(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isSameOriginAs](is-same-origin-as.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isSameOriginAs(other: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Compares 2 URLs and returns true if they have the same origin, which means: same protocol, same host, same port. |
| [isUrl](is-url.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isUrl(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Checks if this String is a URL. |
| [sanitizeURL](sanitize-u-r-l.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.sanitizeURL(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Remove any unwanted character in url like spaces at the beginning or end. |
| [sha1](sha1.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.sha1(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Calculates a SHA1 hash for this string. |
| [toDate](to-date.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.toDate(format: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, locale: `[`Locale`](http://docs.oracle.com/javase/7/docs/api/java/util/Locale.html)` = Locale.ROOT): `[`Date`](http://docs.oracle.com/javase/7/docs/api/java/util/Date.html)<br>Converts a [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) to a [Date](http://docs.oracle.com/javase/7/docs/api/java/util/Date.html) object.`fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.toDate(vararg possibleFormats: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = arrayOf(
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd",
            "yyyy-'W'ww",
            "yyyy-MM",
            "HH:mm"
    )): `[`Date`](http://docs.oracle.com/javase/7/docs/api/java/util/Date.html)`?`<br>Tries to convert a [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) to a [Date](http://docs.oracle.com/javase/7/docs/api/java/util/Date.html) using a list of [possibleFormats](to-date.md#mozilla.components.support.ktx.kotlin$toDate(kotlin.String, kotlin.Array((kotlin.String)))/possibleFormats). |
| [toNormalizedUrl](to-normalized-url.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.toNormalizedUrl(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [tryGetHostFromUrl](try-get-host-from-url.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.tryGetHostFromUrl(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Tries to parse and get host part if this [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) is valid URL. Otherwise returns the string. |
