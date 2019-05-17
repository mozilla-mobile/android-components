[android-components](../index.md) / [mozilla.components.service.fxa.manager](./index.md)

## Package mozilla.components.service.fxa.manager

### Types

| Name | Summary |
|---|---|
| [AccountState](-account-state/index.md) | `enum class AccountState`<br>States of the [FxaAccountManager](-fxa-account-manager/index.md). |
| [DeviceTuple](-device-tuple/index.md) | `data class DeviceTuple`<br>Helper data class that wraps common device initialization parameters. |
| [FxaAccountManager](-fxa-account-manager/index.md) | `open class FxaAccountManager : `[`Closeable`](https://developer.android.com/reference/java/io/Closeable.html)`, `[`Observable`](../mozilla.components.support.base.observer/-observable/index.md)`<`[`AccountObserver`](../mozilla.components.concept.sync/-account-observer/index.md)`>`<br>An account manager which encapsulates various internal details of an account lifecycle and provides an observer interface along with a public API for interacting with an account. The internal state machine abstracts over state space as exposed by the fxaclient library, not the internal states experienced by lower-level representation of a Firefox Account; those are opaque to us. |
| [PeriodicRefreshManager](-periodic-refresh-manager/index.md) | `interface PeriodicRefreshManager` |

### Exceptions

| Name | Summary |
|---|---|
| [FailedToLoadAccountException](-failed-to-load-account-exception/index.md) | `class FailedToLoadAccountException : `[`Exception`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-exception/index.html)<br>Propagated via [AccountObserver.onError](../mozilla.components.concept.sync/-account-observer/on-error.md) if we fail to load a locally stored account during initialization. No action is necessary from consumers. Account state has been re-initialized. |
