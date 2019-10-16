package mozilla.components.browser.state.state.content

/**
 * Enum to describe the current status of a [DownloadState].
 *
 * @property QUEUED means that the [DownloadState] is registered with the DownloadManager and ready
 * to be executed.
 * @property RUNNING means that the [DownloadState] is currently ongoing the downloading process.
 * @property COMPLETED means that the [DownloadState] has been completed successfully.
 * @property FAILED means that the [DownloadState] has been completed but with failure.
 */
enum class DownloadStatus {
    QUEUED, RUNNING, COMPLETED, FAILED
}
