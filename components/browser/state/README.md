# [Android Components](../../../README.md) > Browser > State

🔴 **Note:** This is an **experimental component** still under development. APIs may change at any time. It's not recommended to directly use this component in products yet.

The `browser-state` component is responsible for maintaining the centralized state of a [browser engine](../../concept/engine/README.md).

The immutable `BrowserState` can be accessed and observed via the `BrowserStore`. Apps and other components can dispatch `Action`s on the store in order to trigger the creation of a new `BrowserState`.

Patterns and concepts this component uses are heavily inspired by Redux. Therefore the [Redux documentation](https://redux.js.org/introduction/getting-started) is an excellent resource for learning about some of those concepts.

## Usage

### Setting up the dependency

Use gradle to download the library from JCenter:

```Groovy
implementation "org.mozilla.components:browser-state:{latest-version}
```

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
