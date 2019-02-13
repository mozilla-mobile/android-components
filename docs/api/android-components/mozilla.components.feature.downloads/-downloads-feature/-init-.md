[android-components](../../index.md) / [mozilla.components.feature.downloads](../index.md) / [DownloadsFeature](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`DownloadsFeature(applicationContext: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`, onNeedToRequestPermissions: `[`OnNeedToRequestPermissions`](../-on-need-to-request-permissions.md)` = { }, onDownloadCompleted: `[`OnDownloadCompleted`](../-on-download-completed.md)` = { _, _ -> }, downloadManager: `[`DownloadManager`](../-download-manager/index.md)` = DownloadManager(applicationContext, onDownloadCompleted), sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`, sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null, fragmentManager: FragmentManager? = null, dialog: `[`DownloadDialogFragment`](../-download-dialog-fragment/index.md)` = SimpleDownloadDialogFragment.newInstance())`

Feature implementation to provide download functionality for the selected
session. The feature will subscribe to the selected session and listen
for downloads.

