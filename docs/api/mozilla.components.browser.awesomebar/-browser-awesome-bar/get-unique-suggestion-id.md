[android-components](../../index.md) / [mozilla.components.browser.awesomebar](../index.md) / [BrowserAwesomeBar](index.md) / [getUniqueSuggestionId](./get-unique-suggestion-id.md)

# getUniqueSuggestionId

`@MainThread fun getUniqueSuggestionId(suggestion: `[`Suggestion`](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion/index.md)`): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/awesomebar/src/main/java/mozilla/components/browser/awesomebar/BrowserAwesomeBar.kt#L181)

Returns a unique suggestion ID to make sure ID's can't collide
across providers. This method is not thread-safe and must be
invoked on the main thread.

### Parameters

`suggestion` - the suggestion for which a unique ID should be
generated.

**Return**
the unique ID.

