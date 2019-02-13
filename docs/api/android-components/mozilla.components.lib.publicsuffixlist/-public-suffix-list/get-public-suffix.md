[android-components](../../index.md) / [mozilla.components.lib.publicsuffixlist](../index.md) / [PublicSuffixList](index.md) / [getPublicSuffix](./get-public-suffix.md)

# getPublicSuffix

`fun getPublicSuffix(domain: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): <ERROR CLASS>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/lib/publicsuffixlist/src/main/java/mozilla/components/lib/publicsuffixlist/PublicSuffixList.kt#L92)

Returns the public suffix of the given [domain](get-public-suffix.md#mozilla.components.lib.publicsuffixlist.PublicSuffixList$getPublicSuffix(kotlin.String)/domain). Returns `null` if the [domain](get-public-suffix.md#mozilla.components.lib.publicsuffixlist.PublicSuffixList$getPublicSuffix(kotlin.String)/domain) is a public suffix itself.

E.g.:

```
wwww.mozilla.org -> org
www.bcc.co.uk    -> co.uk
a.b.ide.kyoto.jp -> ide.kyoto.jp
```

