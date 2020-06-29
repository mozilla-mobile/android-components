[android-components](../../index.md) / [mozilla.components.browser.tabstray](../index.md) / [TabsAdapter](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`TabsAdapter(thumbnailLoader: `[`ImageLoader`](../../mozilla.components.support.images.loader/-image-loader/index.md)`? = null, viewHolderProvider: `[`ViewHolderProvider`](../-view-holder-provider.md)` = { parent, tabsTray ->
        DefaultTabViewHolder(
                LayoutInflater.from(parent.context).inflate(
                        R.layout.mozac_browser_tabstray_item,
                        parent,
                        false),
                tabsTray,
                thumbnailLoader
        )
    }, delegate: `[`Observable`](../../mozilla.components.support.base.observer/-observable/index.md)`<`[`Observer`](../../mozilla.components.concept.tabstray/-tabs-tray/-observer/index.md)`> = ObserverRegistry())`

RecyclerView adapter implementation to display a list/grid of tabs.

### Parameters

`delegate` - TabsTray.Observer registry to allow `TabsAdapter` to conform to `Observable<TabsTray.Observer>`.

`viewHolderProvider` - a function that creates a `TabViewHolder`.