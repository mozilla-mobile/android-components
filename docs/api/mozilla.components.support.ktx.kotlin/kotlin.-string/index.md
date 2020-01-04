[android-components](../../index.md) / [mozilla.components.support.ktx.kotlin](../index.md) / [kotlin.String](./index.md)

### Extensions for kotlin.String

| Name | Summary |
|---|---|
| [isEmail](is-email.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isEmail(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isGeoLocation](is-geo-location.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isGeoLocation(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isPhone](is-phone.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isPhone(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isUrl](is-url.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isUrl(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Checks if this String is a URL. |
| [isUrlStrict](is-url-strict.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.isUrlStrict(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Checks if this string is a URL. |
| [sha1](sha1.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.sha1(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Calculates a SHA1 hash for this string. |
| [toDate](to-date.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.toDate(format: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, locale: `[`Locale`](https://developer.android.com/reference/java/util/Locale.html)` = Locale.ROOT): `[`Date`](https://developer.android.com/reference/java/util/Date.html)<br>Converts a [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) to a [Date](https://developer.android.com/reference/java/util/Date.html) object.`fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.toDate(vararg possibleFormats: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = arrayOf(
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd",
            "yyyy-'W'ww",
            "yyyy-MM",
            "HH:mm"
    )): `[`Date`](https://developer.android.com/reference/java/util/Date.html)`?`<br>Tries to convert a [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) to a [Date](https://developer.android.com/reference/java/util/Date.html) using a list of [possibleFormats](to-date.md#mozilla.components.support.ktx.kotlin$toDate(kotlin.String, kotlin.Array((kotlin.String)))/possibleFormats). |
| [toNormalizedUrl](to-normalized-url.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.toNormalizedUrl(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [tryGetHostFromUrl](try-get-host-from-url.md) | `fun `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`.tryGetHostFromUrl(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Tries to parse and get host part if this [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) is valid URL. Otherwise returns the string. |
