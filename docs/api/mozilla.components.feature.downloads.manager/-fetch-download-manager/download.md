[android-components](../../index.md) / [mozilla.components.feature.downloads.manager](../index.md) / [FetchDownloadManager](index.md) / [download](./download.md)

# download

`fun download(download: `[`DownloadState`](../../mozilla.components.browser.state.state.content/-download-state/index.md)`, cookie: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`?` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/downloads/src/main/java/mozilla/components/feature/downloads/manager/FetchDownloadManager.kt#L57)

Overrides [DownloadManager.download](../-download-manager/download.md)

Schedules a download through the [AbstractFetchDownloadService](../../mozilla.components.feature.downloads/-abstract-fetch-download-service/index.md).

### Parameters

`download` - metadata related to the download.

`cookie` - any additional cookie to add as part of the download request.

**Return**
the id reference of the scheduled download.

