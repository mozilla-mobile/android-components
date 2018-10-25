# Android components

[![Task Status](https://github.taskcluster.net/v1/repository/mozilla-mobile/android-components/master/badge.svg)](https://github.taskcluster.net/v1/repository/mozilla-mobile/android-components/master/latest)
[![Build Status](https://travis-ci.org/mozilla-mobile/android-components.svg?branch=master)](https://travis-ci.org/mozilla-mobile/android-components)
[![codecov](https://codecov.io/gh/mozilla-mobile/android-components/branch/master/graph/badge.svg)](https://codecov.io/gh/mozilla-mobile/android-components)

_A collection of Android libraries to build browsers or browser-like applications._

ℹ️ For more information **[see the website](https://mozilla-mobile.github.io/android-components/)**.

A full featured reference browser implementation based on the components can be found in the [reference-browser repository](https://github.com/mozilla-mobile/reference-browser).

# Getting Involved

We encourage you to participate in this open source project. We love pull requests, bug reports, ideas, (security) code reviews or any kind of positive contribution.

Before you attempt to make a contribution please read the [Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/).

* [View current Issues](https://github.com/mozilla-mobile/android-components/issues) or [View current Pull Requests](https://github.com/mozilla-mobile/android-components/pulls).

* [List of good first issues](https://github.com/mozilla-mobile/android-components/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) (**New contributors start here!**) and [List of "help wanted" issues](https://github.com/mozilla-mobile/android-components/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22).

* IRC: [#android-components (irc.mozilla.org)](https://wiki.mozilla.org/IRC) | [view logs](https://mozilla.logbot.info/android-components/)

* Subscribe to our mailing list [android-components@](https://lists.mozilla.org/listinfo/android-components) to keep up to date ([Archives](https://lists.mozilla.org/pipermail/android-components/)).

# Maven repository

All components are getting published on [maven.mozilla.org](http://maven.mozilla.org/).

```groovy
repositories {
    maven {
       url "https://maven.mozilla.org/maven2"
    }
}
```

# Components

* 🔴 **In Development** - Not ready to be used in shipping products.
* ⚪ **Preview** - This component is almost/partially ready and can be tested in products.
* 🔵 **Production ready** - Used by shipping products.

## Browser

High-level components for building browser(-like) apps.

* 🔵 [**Domains**](components/browser/domains/README.md) Localized and customizable domain lists for auto-completion in browsers.

* 🔴 [**Engine-Gecko**](components/browser/engine-gecko/README.md) - *Engine* implementation based on [GeckoView](https://wiki.mozilla.org/Mobile/GeckoView) (Release channel).

* 🔴 [**Engine-Gecko-Beta**](components/browser/engine-gecko-beta/README.md) - *Engine* implementation based on [GeckoView](https://wiki.mozilla.org/Mobile/GeckoView) (Beta channel).

* 🔴 [**Engine-Gecko-Nightly**](components/browser/engine-gecko-nightly/README.md) - *Engine* implementation based on [GeckoView](https://wiki.mozilla.org/Mobile/GeckoView) (Nightly channel).

* 🔴 [**Engine-Servo**](components/browser/engine-servo/README.md) - *Engine* implementation based on the [Servo Browser Engine](https://servo.org/).

* ⚪ [**Engine-System**](components/browser/engine-system/README.md) - *Engine* implementation based on the system's WebView.

* ⚪ [**Errorpages**](components/browser/errorpages/README.md) - Responsive browser error pages for Android apps.

* 🔴 [**Menu**](components/browser/menu/README.md) - A generic menu with customizable items primarily for browser toolbars.

* 🔵 [**Search**](components/browser/search/README.md) - Search plugins and companion code to load, parse and use them.

* ⚪ [**Session**](components/browser/session/README.md) - A generic representation of a browser session.

* 🔴 [**Tabstray**](components/browser/tabstray/README.md) - A customizable tabs tray for browsers.

* ⚪ [**Toolbar**](components/browser/toolbar/README.md) - A customizable toolbar for browsers.

## Concept

_API contracts and abstraction layers for browser components._

* 🔴 [**Engine**](components/concept/engine/README.md) - Abstraction layer that allows hiding the actual browser engine implementation.

* 🔴 [**Tabstray**](components/concept/tabstray/README.md) - Abstract definition of a tabs tray component.

* ⚪ [**Toolbar**](components/concept/toolbar/README.md) - Abstract definition of a browser toolbar component.

## Feature

_Combined components to implement feature-specific use cases._

* 🔴 [**Downloads**](components/feature/downloads/README.md) - A component to perform downloads using the [Android downloads manager](https://developer.android.com/reference/android/app/DownloadManager).

* 🔴 [**Intent**](components/feature/intent/README.md) - A component that provides intent processing functionality by combining various other feature modules.

* 🔴 [**Search**](components/feature/search/README.md) - A component that connects an (concept) engine implementation with the browser search module.

* 🔴 [**Session**](components/feature/session/README.md) - A component that connects an (concept) engine implementation with the browser session module.

* 🔴 [**Tabs**](components/feature/tabs/README.md) - A component that connects a tabs tray implementation with the session and toolbar modules.

* 🔴 [**Toolbar**](components/feature/toolbar/README.md) - A component that connects a (concept) toolbar implementation with the browser session module.

## UI

_Generic low-level UI components for building apps._

* 🔵 [**Autocomplete**](components/ui/autocomplete/README.md) - A set of components to provide autocomplete functionality.

* 🔵 [**Colors**](components/ui/colors/README.md) - The standard set of [Photon](https://design.firefox.com/photon/) colors.

* 🔵 [**Fonts**](components/ui/fonts/README.md) - The standard set of fonts used by Mozilla Android products.

* 🔵 [**Icons**](components/ui/icons/README.md) - A collection of often used browser icons.

* 🔵 [**Progress**](components/ui/progress/README.md) - An animated progress bar following the Photon Design System.

* ⚪ [**Tabcounter**](components/ui/tabcounter/README.md) - A button that shows the current tab count and can animate state changes.

## Service

_Components and libraries to interact with backend services._

* 🔵 [**Firefox Accounts (FxA)**](components/service/firefox-accounts/README.md) - A library for integrating with Firefox Accounts.

* 🔴 [**Firefox Sync - Logins**](components/service/sync-logins/README.md) - A library for integrating with Firefox Sync - Logins.

* ⚪ [**Fretboard**](components/service/fretboard/README.md) - An Android framework for segmenting users in order to run A/B tests and roll out features gradually.

* 🔴 [**Glean**](components/service/glean/README.md) - A client-side telemetry SDK for collecting metrics and sending them to Mozilla's telemetry service (eventually replacing [service-telemetry](components/service/telemetry/README.md)).

* 🔵 [**Telemetry**](components/service/telemetry/README.md) - A generic library for sending telemetry pings from Android applications to Mozilla's telemetry service.

## Support

_Supporting components with generic helper code._

* ⚪ [**Base**](components/support/base/README.md) - Base component containing building blocks for components.

* 🔵 [**Ktx**](components/support/ktx/README.md) - A set of Kotlin extensions on top of the Android framework and Kotlin standard library.

* ⚪ [**Test**](components/support/test/README.md) - A collection of helpers for testing components.

* 🔵 [**Utils**](components/support/utils/README.md) - Generic utility classes to be shared between projects.

## Standalone libraries

* ⚪ [**Crash**](components/lib/crash/README.md) - A generic crash reporter component that can report crashes to multiple services.

* 🔴 [**Dataprotect**](components/lib/dataprotect/README.md) - A component using AndroidKeyStore to protect user data.

* ⚪ [**JEXL**](components/lib/jexl/README.md) - Javascript Expression Language: Context-based expression parser and evaluator.

## Tooling

* 🔵 [**Lint**](components/tooling/lint/README.md) - Custom Lint rules for the components repository.

# Sample apps

_Sample apps using various components._

* [**Browser**](samples/browser) - A simple browser composed from browser components. This sample application is only a very basic browser. For a full-featured reference browser implementation see the **[reference-browser repository](https://github.com/mozilla-mobile/reference-browser)**.

* [**Firefox Accounts (FxA)**](samples/firefox-accounts) - A simple app demoing Firefox Accounts integration.

* [**Firefox Sync - Logins**](samples/sync-logins) - A simple app demoing Firefox Sync (Logins) integration.

* [**Toolbar**](samples/toolbar) - An app demoing multiple customized toolbars using the [**browser-toolbar**](components/browser/toolbar/README.md) component.

* [**DataProtect**](samples/dataprotect) - An app demoing how to use the [**Dataprotect**](components/lib/dataprotect/README.md) component to load and store encrypted data in `SharedPreferences`.

* [**Glean**](samples/glean) - An app demoing how to use the [**Glean**](components/service/glean/README.md) library to collect and send telemetry data.

# License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
