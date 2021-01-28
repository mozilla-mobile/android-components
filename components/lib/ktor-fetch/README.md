# [Android Components](../../../README.md) > Libraries > Ktor-Fetch

A component that can translate between `concept-fetch` and [Ktor]()(https://ktor.io/).

## Usage

### Setting up the dependency

Use Gradle to download the library from [maven.mozilla.org](https://maven.mozilla.org/) ([Setup repository](../../../README.md#maven-repository)):

```Groovy
implementation "org.mozilla.components:lib-fetch-okhttp:{latest-version}"
```

### Creating a `HttpClientEngine` (Ktor) from a `Client` (`concept-fetch`)

```Kotlin
val client = ...
HttpClient(client.toKtorEngine()).use {
    // ...
}
```

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
