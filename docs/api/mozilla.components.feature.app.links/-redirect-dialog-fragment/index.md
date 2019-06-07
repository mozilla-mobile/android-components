[android-components](../../index.md) / [mozilla.components.feature.app.links](../index.md) / [RedirectDialogFragment](./index.md)

# RedirectDialogFragment

`abstract class RedirectDialogFragment : DialogFragment` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/app-links/src/main/java/mozilla/components/feature/app/links/RedirectDialogFragment.kt#L18)

This is a general representation of a dialog meant to be used in collaboration with [AppLinksFeature](../-app-links-feature/index.md)
to show a dialog before an external link is opened.
If [SimpleRedirectDialogFragment](../-simple-redirect-dialog-fragment/index.md) is not flexible enough for your use case you should inherit for this class.
Be mindful to call [onConfirmRedirect](on-confirm-redirect.md) when you want to open the linked app.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `RedirectDialogFragment()`<br>This is a general representation of a dialog meant to be used in collaboration with [AppLinksFeature](../-app-links-feature/index.md) to show a dialog before an external link is opened. If [SimpleRedirectDialogFragment](../-simple-redirect-dialog-fragment/index.md) is not flexible enough for your use case you should inherit for this class. Be mindful to call [onConfirmRedirect](on-confirm-redirect.md) when you want to open the linked app. |

### Properties

| Name | Summary |
|---|---|
| [onConfirmRedirect](on-confirm-redirect.md) | `var onConfirmRedirect: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>A callback to trigger a download, call it when you are ready to open the linked app. For instance, a valid use case can be in confirmation dialog, after the positive button is clicked, this callback must be called. |

### Functions

| Name | Summary |
|---|---|
| [setAppLinkRedirect](set-app-link-redirect.md) | `fun setAppLinkRedirect(redirect: `[`AppLinkRedirect`](../-app-link-redirect/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>add the metadata of this download object to the arguments of this fragment. |

### Companion Object Properties

| Name | Summary |
|---|---|
| [FRAGMENT_TAG](-f-r-a-g-m-e-n-t_-t-a-g.md) | `const val FRAGMENT_TAG: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [KEY_INTENT_URL](-k-e-y_-i-n-t-e-n-t_-u-r-l.md) | `const val KEY_INTENT_URL: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Key for finding the app link. |

### Inheritors

| Name | Summary |
|---|---|
| [SimpleRedirectDialogFragment](../-simple-redirect-dialog-fragment/index.md) | `class SimpleRedirectDialogFragment : `[`RedirectDialogFragment`](./index.md)<br>This is the default implementation of the [RedirectDialogFragment](./index.md). |
