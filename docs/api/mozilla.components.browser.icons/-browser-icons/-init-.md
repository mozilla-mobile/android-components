[android-components](../../index.md) / [mozilla.components.browser.icons](../index.md) / [BrowserIcons](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`BrowserIcons(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`, httpClient: `[`Client`](../../mozilla.components.concept.fetch/-client/index.md)`, generator: `[`IconGenerator`](../../mozilla.components.browser.icons.generator/-icon-generator/index.md)` = DefaultIconGenerator(context), loaders: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`IconLoader`](../../mozilla.components.browser.icons.loader/-icon-loader/index.md)`> = listOf(
        HttpIconLoader(httpClient)
    ), decoders: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`IconDecoder`](../../mozilla.components.browser.icons.decoder/-icon-decoder/index.md)`> = listOf(
        AndroidIconDecoder(),
        ICOIconDecoder()
    ), jobDispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(THREADS).asCoroutineDispatcher())`

Entry point for loading icons for websites.

### Parameters

`generator` - The [IconGenerator](../../mozilla.components.browser.icons.generator/-icon-generator/index.md) to generate an icon if no icon could be loaded.

`decoders` - List of [IconDecoder](../../mozilla.components.browser.icons.decoder/-icon-decoder/index.md) instances to use when decoding a loaded icon into a [android.graphics.Bitmap](https://developer.android.com/reference/android/graphics/Bitmap.html).