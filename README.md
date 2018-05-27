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

* 🔴 **In Development** - Not ready to be used in shipping products.
* ⚪ **Preview** - This component is almost ready and can be (partially) tested in products.
* 🔵 **Production ready** - Used by shipping products.

## Browser

High-level components for building browser(-like) apps.

* 🔵 **Domains** - Localized and customizable domain lists for auto-completion in browsers. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/domains.svg)](https://bintray.com/pocmo/Mozilla-Mobile/domains)

* 🔴 **Engine-Gecko** - *Engine* implementation based on [GeckoView](https://wiki.mozilla.org/Mobile/GeckoView). [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/engine-gecko.svg)](https://bintray.com/pocmo/Mozilla-Mobile/engine-gecko)

* 🔴 **Engine-System** - *Engine* implementation based on the system's WebView. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/engine-system.svg)](https://bintray.com/pocmo/Mozilla-Mobile/engine-system) 

* ⚪ **Erropages** - Responsive browser error pages for Android apps. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/errorpages.svg)](https://bintray.com/pocmo/Mozilla-Mobile/errorpages) 

* 🔴 **Menu** - A generic menu with customizable items primarily for browser toolbars. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/menu.svg)](https://bintray.com/pocmo/Mozilla-Mobile/menu) 

* 🔵 **Search** - Search plugins and companion code to load, parse and use them. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/search.svg)](https://bintray.com/pocmo/Mozilla-Mobile/search)

* 🔴 **Session** - A generic representation of a browser session. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/session.svg)](https://bintray.com/pocmo/Mozilla-Mobile/session)

* 🔴 **Toolbar** - A customizable toolbar for browsers. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/toolbar.svg)](https://bintray.com/pocmo/Mozilla-Mobile/toolbar)

## Concept

_API contracts and abstraction layers for browser components._

* 🔴 **Engine** - Abstraction layer that allows hiding the actual browser engine implementation. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/engine.svg)](https://bintray.com/pocmo/Mozilla-Mobile/engine)

* 🔴 **Session-Storage** - Abstraction layer for hiding the actual session storage implementation. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/session-storage.svg)](https://bintray.com/pocmo/Mozilla-Mobile/session-storeage)

* 🔴 **Toolbar** - Abstract definition of a browser toolbar component. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/abstract-toolbar.svg)](https://bintray.com/pocmo/Mozilla-Mobile/abstract-toolbar)

## Feature

_Combined components to implement feature-specific use cases._

* 🔴 **Search** - Combining an engine implementation with the browser search module. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/feature-search.svg)](https://bintray.com/pocmo/Mozilla-Mobile/feature-search)

* 🔴 **Session** - Combining an engine implementation with the browser session module. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/feature-session.svg)](https://bintray.com/pocmo/Mozilla-Mobile/feature-session)

* 🔴 **Toolbar** - Combining a toolbar implementation with the browser session module. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/feature-toolbar.svg)](https://bintray.com/pocmo/Mozilla-Mobile/feature-toolbar)

## UI

_Generic low-level UI components for building apps._

* 🔵 **Autocomplete** - A set of components to provide autocomplete functionality. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/autocomplete.svg)](https://bintray.com/pocmo/Mozilla-Mobile/autocomplete)

* 🔵 **Colors** - The standard set of [Photon](https://design.firefox.com/photon/) colors. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/colors.svg)](https://bintray.com/pocmo/Mozilla-Mobile/colors)

* 🔵 **Fonts** - The standard set of fonts used by Mozilla Android products. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/fonts.svg)](https://bintray.com/pocmo/Mozilla-Mobile/fonts)

* 🔵 **Icons** - A collection of often used browser icons. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/icons.svg)](https://bintray.com/pocmo/Mozilla-Mobile/icons)

* 🔵 **Progress** - An animated progress bar following the Photon Design System. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/progress.svg)](https://bintray.com/pocmo/Mozilla-Mobile/progress)

## Service

_Components and libraries to interact with backend services._

* 🔵 **Telemetry** - A library for sending pings from Android apps to Mozilla's telemetry service. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/telemetry.svg)](https://bintray.com/pocmo/Mozilla-Mobile/telemetry)

## Support

_Supporting components with generic helper code._

* 🔵 **Ktx** - A set of Kotlin extensions on top of the Android framework and Kotlin standard library. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/ktx.svg)](https://bintray.com/pocmo/Mozilla-Mobile/ktx)

* 🔵 **Utils** - Generic utility classes to be shared between projects. [![Bintray](https://img.shields.io/bintray/v/pocmo/Mozilla-Mobile/utils.svg)](https://bintray.com/pocmo/Mozilla-Mobile/utils)

# License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
