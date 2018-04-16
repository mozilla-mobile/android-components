# Android components

[![Task Status](https://github.taskcluster.net/v1/repository/mozilla-mobile/android-components/master/badge.svg)](https://github.taskcluster.net/v1/repository/mozilla-mobile/android-components/master/latest)
[![Build Status](https://travis-ci.org/mozilla-mobile/android-components.svg?branch=master)](https://travis-ci.org/mozilla-mobile/android-components)
[![codecov](https://codecov.io/gh/mozilla-mobile/android-components/branch/master/graph/badge.svg)](https://codecov.io/gh/mozilla-mobile/android-components)
[![Sputnik](https://sputnik.ci/conf/badge)](https://sputnik.ci/app#/builds/mozilla-mobile/android-components)
![](https://api.bintray.com/packages/pocmo/Mozilla-Mobile/errorpages/images/download.svg)

_A collection of Android libraries to build browsers or browser-like applications._

# Getting Involved

We encourage you to participate in this open source project. We love Pull Requests, Bug Reports, ideas, (security) code reviews or any kind of positive contribution. Please read the [Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/).

# Components

## Browser

High-level components for building browser(-like) apps.

* **Domains** - Localized and customizable domain lists for auto-completion in browsers.

* **Engine** - An abstract layer hiding the actual browser engine implementation.

* **Engine-Gecko** - *Engine* implementation based on [GeckoView](https://wiki.mozilla.org/Mobile/GeckoView).

* **Engine-System** - *Engine* implementation based on the system's WebView.

* **Erropages** - Responsive browser error pages for Android apps.

* **Search** - Search plugins and companion code to load, parse and use them.

* **Session** - A generic representation of a browser session.

* **Toolbar** - A customizable toolbar for browsers.

## UI

_Generic UI components for building apps._

* **Colors** - The standard set of [Photon](https://design.firefox.com/photon/) colors.

* **Autocomplete** - A set of components to provide autocomplete functionality.

## Support

_Supporting components with generic helper code._

* **Ktx** - A set of Kotlin extensions on top of the Android framework and Kotlin standard library.

* **Utils** - Generic utility classes to be shared between projects.

# License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
