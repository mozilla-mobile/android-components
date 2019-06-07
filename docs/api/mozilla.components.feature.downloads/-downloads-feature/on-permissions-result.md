[android-components](../../index.md) / [mozilla.components.feature.downloads](../index.md) / [DownloadsFeature](index.md) / [onPermissionsResult](./on-permissions-result.md)

# onPermissionsResult

`fun onPermissionsResult(permissions: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>, grantResults: `[`IntArray`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int-array/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/downloads/src/main/java/mozilla/components/feature/downloads/DownloadsFeature.kt#L98)

Notifies the feature that the permissions request was completed. It will then
either trigger or clear the pending download.

