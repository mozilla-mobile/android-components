[android-components](../../index.md) / [mozilla.components.feature.downloads](../index.md) / [SimpleDownloadDialogFragment](./index.md)

# SimpleDownloadDialogFragment

`class SimpleDownloadDialogFragment : `[`DownloadDialogFragment`](../-download-dialog-fragment/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/downloads/src/main/java/mozilla/components/feature/downloads/SimpleDownloadDialogFragment.kt#L26)

A confirmation dialog to be called before a download is triggered.
Meant to be used in collaboration with [DownloadsFeature](../-downloads-feature/index.md)

[SimpleDownloadDialogFragment](./index.md) is the default dialog use by DownloadsFeature if you don't provide a value.
It is composed by a title, a negative and a positive bottoms. When the positive button is clicked
the download it triggered.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SimpleDownloadDialogFragment()`<br>A confirmation dialog to be called before a download is triggered. Meant to be used in collaboration with [DownloadsFeature](../-downloads-feature/index.md) |

### Inherited Properties

| Name | Summary |
|---|---|
| [onStartDownload](../-download-dialog-fragment/on-start-download.md) | `var onStartDownload: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>A callback to trigger a download, call it when you are ready to start a download. For instance, a valid use case can be in confirmation dialog, after the positive button is clicked, this callback must be called. |

### Functions

| Name | Summary |
|---|---|
| [onCreateDialog](on-create-dialog.md) | `fun onCreateDialog(savedInstanceState: <ERROR CLASS>?): <ERROR CLASS>` |

### Inherited Functions

| Name | Summary |
|---|---|
| [setDownload](../-download-dialog-fragment/set-download.md) | `fun setDownload(download: `[`Download`](../../mozilla.components.browser.session/-download/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>add the metadata of this download object to the arguments of this fragment. |

### Companion Object Properties

| Name | Summary |
|---|---|
| [KEY_CANCELABLE](-k-e-y_-c-a-n-c-e-l-a-b-l-e.md) | `const val KEY_CANCELABLE: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [KEY_NEGATIVE_TEXT](-k-e-y_-n-e-g-a-t-i-v-e_-t-e-x-t.md) | `const val KEY_NEGATIVE_TEXT: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [KEY_POSITIVE_TEXT](-k-e-y_-p-o-s-i-t-i-v-e_-t-e-x-t.md) | `const val KEY_POSITIVE_TEXT: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [KEY_THEME_ID](-k-e-y_-t-h-e-m-e_-i-d.md) | `const val KEY_THEME_ID: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [KEY_TITLE_TEXT](-k-e-y_-t-i-t-l-e_-t-e-x-t.md) | `const val KEY_TITLE_TEXT: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Companion Object Functions

| Name | Summary |
|---|---|
| [newInstance](new-instance.md) | `fun newInstance(dialogTitleText: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = mozac_feature_downloads_dialog_download, positiveButtonText: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = mozac_feature_downloads_dialog_download, negativeButtonText: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = mozac_feature_downloads_dialog_cancel, themeResId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0, cancelable: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false): `[`SimpleDownloadDialogFragment`](./index.md)<br>A builder method for creating a [SimpleDownloadDialogFragment](./index.md) |
