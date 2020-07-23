---
layout: page
title: Adding a `feature-theme` component
permalink: /rfc/0004-feature-theme-component
---

* Start date: 2020-07-23
* RFC PR: [#7835](https://github.com/mozilla-mobile/android-components/pull/7835)

## Summary

Adding a `feature-theme` component for exclusively managing the status bar, navigation bar, and app theme.

## Motivation

We currently have many different components and applications that need to style the status bar and navigation bar on an Android device. However, there is no explicit coordination between these components, and as a result theming is a race condition where the last to theme wins.

* Custom Tabs, Progressive Web Apps, and Trusted Web Activities all want to set a custom status bar and navigation bar color to match the corresponding external app.
* Custom Tabs and Trusted Web Activities can specify different colors for light mode and dark mode.
* Fenix overrides the Custom Tab toolbar to inject custom private tab theming, which has bugs.
* Fenix overrides Trusted Web Activity styling because its theme call happens after.
* In addition to light and dark mode, Fenix and other browsers may want different themes for private mode, containers, and user themes.

## Reference-level explanation

We introduce a `feature-theme` component that will be the only class that touches status bar or navigation bar styles. Components and browsers can make requests to the theme component for certain themes with different priority levels and configurations.

The current `Window.setStatusBarTheme` & `Window.setNavigationBarTheme` would be moved into a helper class that would manage setting these values. We may want to additionally include setting the toolbar color here. Browser apps would either set values in a store instance for the helper class to observe, or call methods on the helper class itself. Either system should let us associate priorities to the themes (i.e.: default styles (normal/private theme), user styles (containers?), external app styles (custom tabs)).

## Drawbacks

* Allowing custom theme entries means that we cannot use sealed classes or enums to represent the different themes.

## Rationale and alternatives

Ideally only one component should mess with theming at a time. Custom Tabs, PWAs, and TWAs all delegate to `CustomTabsToolbarFeature`. It may be possible to only use custom Fenix themes in normal browsing and leave external app styling to `feature-customtabs`.

## Prior art

* [`CustomTabColorSchemeParams`](https://developer.android.com/reference/androidx/browser/customtabs/CustomTabColorSchemeParams)
* Fenix has a [`ThemeManager`](https://github.com/mozilla-mobile/fenix/blob/master/app/src/main/java/org/mozilla/fenix/theme/ThemeManager.kt) class for handling normal/private mode themes.
