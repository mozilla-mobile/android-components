[android-components](../../index.md) / [mozilla.components.feature.awesomebar](../index.md) / [AwesomeBarFeature](./index.md)

# AwesomeBarFeature

`class AwesomeBarFeature` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/awesomebar/src/main/java/mozilla/components/feature/awesomebar/AwesomeBarFeature.kt#L28)

Connects an [AwesomeBar](../../mozilla.components.concept.awesomebar/-awesome-bar/index.md) with a [Toolbar](../../mozilla.components.concept.toolbar/-toolbar/index.md) and allows adding multiple [AwesomeBar.SuggestionProvider](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion-provider/index.md) implementations.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `AwesomeBarFeature(awesomeBar: `[`AwesomeBar`](../../mozilla.components.concept.awesomebar/-awesome-bar/index.md)`, toolbar: `[`Toolbar`](../../mozilla.components.concept.toolbar/-toolbar/index.md)`, engineView: `[`EngineView`](../../mozilla.components.concept.engine/-engine-view/index.md)`? = null, icons: `[`BrowserIcons`](../../mozilla.components.browser.icons/-browser-icons/index.md)`? = null, onEditStart: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = null, onEditComplete: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = null)`<br>Connects an [AwesomeBar](../../mozilla.components.concept.awesomebar/-awesome-bar/index.md) with a [Toolbar](../../mozilla.components.concept.toolbar/-toolbar/index.md) and allows adding multiple [AwesomeBar.SuggestionProvider](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion-provider/index.md) implementations. |

### Functions

| Name | Summary |
|---|---|
| [addClipboardProvider](add-clipboard-provider.md) | `fun addClipboardProvider(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`, loadUrlUseCase: `[`LoadUrlUseCase`](../../mozilla.components.feature.session/-session-use-cases/-load-url-use-case/index.md)`): `[`AwesomeBarFeature`](./index.md) |
| [addHistoryProvider](add-history-provider.md) | `fun addHistoryProvider(historyStorage: `[`HistoryStorage`](../../mozilla.components.concept.storage/-history-storage/index.md)`, loadUrlUseCase: `[`LoadUrlUseCase`](../../mozilla.components.feature.session/-session-use-cases/-load-url-use-case/index.md)`): `[`AwesomeBarFeature`](./index.md)<br>Add a [AwesomeBar.SuggestionProvider](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion-provider/index.md) for browsing history to the [AwesomeBar](../../mozilla.components.concept.awesomebar/-awesome-bar/index.md). |
| [addSearchProvider](add-search-provider.md) | `fun addSearchProvider(searchEngine: `[`SearchEngine`](../../mozilla.components.browser.search/-search-engine/index.md)`, searchUseCase: `[`SearchUseCase`](../../mozilla.components.feature.search/-search-use-cases/-search-use-case/index.md)`, fetchClient: `[`Client`](../../mozilla.components.concept.fetch/-client/index.md)`, limit: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = 15, mode: `[`Mode`](../../mozilla.components.feature.awesomebar.provider/-search-suggestion-provider/-mode/index.md)` = SearchSuggestionProvider.Mode.SINGLE_SUGGESTION): `[`AwesomeBarFeature`](./index.md)<br>Add a [AwesomeBar.SuggestionProvider](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion-provider/index.md) for search engine suggestions to the [AwesomeBar](../../mozilla.components.concept.awesomebar/-awesome-bar/index.md). |
| [addSessionProvider](add-session-provider.md) | `fun addSessionProvider(sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`, selectTabUseCase: `[`SelectTabUseCase`](../../mozilla.components.feature.tabs/-tabs-use-cases/-select-tab-use-case/index.md)`): `[`AwesomeBarFeature`](./index.md)<br>Add a [AwesomeBar.SuggestionProvider](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion-provider/index.md) for "Open tabs" to the [AwesomeBar](../../mozilla.components.concept.awesomebar/-awesome-bar/index.md). |
