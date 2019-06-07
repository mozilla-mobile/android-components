[android-components](../../index.md) / [mozilla.components.browser.domains.autocomplete](../index.md) / [BaseDomainAutocompleteProvider](./index.md)

# BaseDomainAutocompleteProvider

`open class BaseDomainAutocompleteProvider : `[`DomainAutocompleteProvider`](../-domain-autocomplete-provider/index.md)`, CoroutineScope` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/domains/src/main/java/mozilla/components/browser/domains/autocomplete/Providers.kt#L48)

Provides common autocomplete functionality powered by domain lists.

### Parameters

`list` - source of domains

`domainsLoader` - provider for all available domains

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `BaseDomainAutocompleteProvider(list: `[`DomainList`](../-domain-list/index.md)`, domainsLoader: `[`DomainsLoader`](../-domains-loader.md)`)`<br>Provides common autocomplete functionality powered by domain lists. |

### Properties

| Name | Summary |
|---|---|
| [domains](domains.md) | `var domains: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Domain`](../../mozilla.components.browser.domains/-domain/index.md)`>` |

### Functions

| Name | Summary |
|---|---|
| [getAutocompleteSuggestion](get-autocomplete-suggestion.md) | `open fun getAutocompleteSuggestion(query: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`DomainAutocompleteResult`](../-domain-autocomplete-result/index.md)`?`<br>Computes an autocomplete suggestion for the given text, and invokes the provided callback, passing the result. |
| [initialize](initialize.md) | `fun initialize(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [launchGeckoResult](../../mozilla.components.browser.engine.gecko/kotlinx.coroutines.-coroutine-scope/launch-gecko-result.md) | `fun <T> CoroutineScope.launchGeckoResult(context: `[`CoroutineContext`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html)` = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> `[`T`](../../mozilla.components.browser.engine.gecko/kotlinx.coroutines.-coroutine-scope/launch-gecko-result.md#T)`): `[`GeckoResult`](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/GeckoResult.html)`<`[`T`](../../mozilla.components.browser.engine.gecko/kotlinx.coroutines.-coroutine-scope/launch-gecko-result.md#T)`>`<br>Create a GeckoResult from a co-routine. |

### Inheritors

| Name | Summary |
|---|---|
| [CustomDomainsProvider](../-custom-domains-provider/index.md) | `class CustomDomainsProvider : `[`BaseDomainAutocompleteProvider`](./index.md)<br>Provides autocomplete functionality for domains based on a list managed by [CustomDomains](../../mozilla.components.browser.domains/-custom-domains/index.md). |
| [ShippedDomainsProvider](../-shipped-domains-provider/index.md) | `class ShippedDomainsProvider : `[`BaseDomainAutocompleteProvider`](./index.md)<br>Provides autocomplete functionality for domains based on provided list of assets (see [Domains](../../mozilla.components.browser.domains/-domains/index.md)). |
