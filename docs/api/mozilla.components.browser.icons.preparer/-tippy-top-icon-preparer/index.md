[android-components](../../index.md) / [mozilla.components.browser.icons.preparer](../index.md) / [TippyTopIconPreparer](./index.md)

# TippyTopIconPreparer

`class TippyTopIconPreparer : `[`IconPreprarer`](../-icon-preprarer/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/icons/src/main/java/mozilla/components/browser/icons/preparer/TippyTopIconPreparer.kt#L28)

[IconPreprarer](../-icon-preprarer/index.md) implementation that looks up the host in our "tippy top" list. If it can find a match then it inserts
the icon URL into the request.

The "tippy top" list is a list of "good" icons for top pages maintained by Mozilla:
https://github.com/mozilla/tippy-top-sites

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `TippyTopIconPreparer(assetManager: <ERROR CLASS>)`<br>[IconPreprarer](../-icon-preprarer/index.md) implementation that looks up the host in our "tippy top" list. If it can find a match then it inserts the icon URL into the request. |

### Functions

| Name | Summary |
|---|---|
| [prepare](prepare.md) | `fun prepare(context: <ERROR CLASS>, request: `[`IconRequest`](../../mozilla.components.browser.icons/-icon-request/index.md)`): `[`IconRequest`](../../mozilla.components.browser.icons/-icon-request/index.md) |
