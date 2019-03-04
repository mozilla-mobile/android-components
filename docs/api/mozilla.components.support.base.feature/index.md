[android-components](../index.md) / [mozilla.components.support.base.feature](./index.md)

## Package mozilla.components.support.base.feature

### Types

| Name | Summary |
|---|---|
| [BackHandler](-back-handler/index.md) | `interface BackHandler`<br>Generic interface for fragments, features and other components that want to handle 'back' button presses. |
| [LifecycleAwareFeature](-lifecycle-aware-feature/index.md) | `interface LifecycleAwareFeature : LifecycleObserver`<br>An interface for all entry points to feature components to implement in order to make them lifecycle aware. |
| [ViewBoundFeatureWrapper](-view-bound-feature-wrapper/index.md) | `class ViewBoundFeatureWrapper<T : `[`LifecycleAwareFeature`](-lifecycle-aware-feature/index.md)`>`<br>Wrapper for [LifecycleAwareFeature](-lifecycle-aware-feature/index.md) instances that keep a strong references to a [View](https://developer.android.com/reference/android/view/View.html). This wrapper is helpful when the lifetime of the [View](https://developer.android.com/reference/android/view/View.html) may be shorter than the [Lifecycle](#) and you need to keep a reference to the [LifecycleAwareFeature](-lifecycle-aware-feature/index.md) that may outlive the [View](https://developer.android.com/reference/android/view/View.html). |
