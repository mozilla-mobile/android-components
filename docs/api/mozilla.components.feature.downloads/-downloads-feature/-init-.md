[android-components](../../index.md) / [mozilla.components.feature.downloads](../index.md) / [DownloadsFeature](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`DownloadsFeature(applicationContext: <ERROR CLASS>, onNeedToRequestPermissions: `[`OnNeedToRequestPermissions`](../../mozilla.components.support.base.feature/-on-need-to-request-permissions.md)` = { }, onDownloadCompleted: `[`OnDownloadCompleted`](../../mozilla.components.feature.downloads.manager/-on-download-completed.md)` = noop, downloadManager: `[`DownloadManager`](../../mozilla.components.feature.downloads.manager/-download-manager/index.md)` = AndroidDownloadManager(applicationContext), sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`, sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null, fragmentManager: FragmentManager? = null, dialog: `[`DownloadDialogFragment`](../-download-dialog-fragment/index.md)` = SimpleDownloadDialogFragment.newInstance())`

Feature implementation to provide download functionality for the selected
session. The feature will subscribe to the selected session and listen
for downloads.

