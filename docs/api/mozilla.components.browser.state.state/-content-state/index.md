[android-components](../../index.md) / [mozilla.components.browser.state.state](../index.md) / [ContentState](./index.md)

# ContentState

`data class ContentState` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/state/src/main/java/mozilla/components/browser/state/state/ContentState.kt#L20)

Value type that represents the state of the content within a [SessionState](../-session-state/index.md).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ContentState(url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, private: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false, title: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = "", progress: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 0, loading: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false, searchTerms: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = "", securityInfo: `[`SecurityInfoState`](../-security-info-state/index.md)` = SecurityInfoState())`<br>Value type that represents the state of the content within a [SessionState](../-session-state/index.md). |

### Properties

| Name | Summary |
|---|---|
| [loading](loading.md) | `val loading: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [private](private.md) | `val private: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>whether or not the session is private. |
| [progress](progress.md) | `val progress: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>the loading progress of the current page. |
| [searchTerms](search-terms.md) | `val searchTerms: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the last used search terms, or an empty string if no search was executed for this session. |
| [securityInfo](security-info.md) | `val securityInfo: `[`SecurityInfoState`](../-security-info-state/index.md)<br>the security information as [SecurityInfoState](../-security-info-state/index.md), describing whether or not the current session is for a secure URL, as well as the host and SSL certificate authority. |
| [title](title.md) | `val title: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the title of the current page. |
| [url](url.md) | `val url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the loading or loaded URL. |
