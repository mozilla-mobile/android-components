---
layout: page
title: RFC process
permalink: /rfc/0005-json-library
---

* Start date: 2020-07-28
* RFC PR: [#7892](https://github.com/mozilla-mobile/android-components/pull/7892)

## Summary

Introduce a library for reading and writing JSON into Android Components and Fenix.

## Motivation

We currently interact with JSON in many areas of Android Components and Fenix. Currently serialization and deserialization is all done manually with `JSONObject`, which creates more work for developers and potential bugs. Fenix also is interested in using a library and we'd like to avoid shipping multiple JSON libraries to users. Using a JSON library allows us to reduce code that needs to be written, potentially gain some performance wins from a well maintained lib, and avoid bugs in our custom JSON implementations. It would also be easier to build streaming JSON parsers.

Using a standard library would also create some consistency in our JSON API surface. Parsing JSON looks totally different for every API that interacts with it, with no guarantees about error handling or null safety.

Some examples of JSON usage today are:
- Reading a `WebAppManifest`
- Parsing API calls in various service components
- Sending messages to and from extensions

## Guide-level explanation

We'll start with selecting a JSON library to integrate into Android Components and Fenix. Some options we've looked at include:
- [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization): Multi-platform, multi-format, built by JetBrains. Also much newer than other solutions so documentation is relatively lacking.
- [Gson](https://github.com/google/gson): Long standing library built by Google. Slower than other options.
- [Moshi](https://github.com/square/moshi): Simpler API, built by Square. Fewer features than Gson but also faster.

kotlinx-serialization feels the most in-line with our design axioms as it extends the Kotlin standard library. We can use it when building new components that use JSON and slowly migrate existing code.

## Drawbacks

* Using a new tool means we have to invest some time in learning how to use it. We're at least familiar with JSONObject as are most people. This "having another thing to learn" thing is especially problematic if Fenix and other apps adopt different technologies to fill the same gap.
* We want to be conservative adding third-party libraries.

## Rationale and alternatives

We currently use `JSONObject` for our JSON decoding and encoding. This forces duplication of logic (need to functions for both String -> JSON and JSON -> String) and creates potential crashes if we don't catch JSON exceptions properly. A well maintained library allows us to write less code and potentially gain performance wins.

## Prior art

Many other open-source projects are using an JSON library.
