# Building the Places component

This is very much a WIP, but should still offer enough so that we can discuss
the general shape of the component and how it is used in the reference-browser.

## Building the Rust pieces

* Check out the [master branch of the application-services repo](https://github.com/mozilla/application-services/) repo
  (although please check [this PR](https://github.com/mozilla/application-services/pull/324) has been merged)

* Edit `build.gradle` in the root and change the version from `0.7.1` to `0.7.1-SNAPSHOT`

* Follow the [build instructions](https://github.com/mozilla/application-services/wiki/Building-the-Android-Components)
  to setup a build environment for the rust code.

* In the root of the application-services repo, execute `./gradlew service-sync-places:install`

* Sanity check that `~/.m2/repository/org/mozilla/places/places/0.7.1-SNAPSHOT/` exists.

## Publish this new component to your local maven repository

Please read [this guide](https://mozilla-mobile.github.io/android-components/contributing/testing-components-inside-app) for all the gritty details, but, roughly:

* Note that this branch is based on `v0.28.0` so the corresponding `reference-browser` PR works.
  It has also modified `buildSrc/src/main/java/Config.kt` by appending `-SNAPSHOT` to the components version.

* Execute `gradlew service-sync-places:install` to publish it locally. Sanity check that there is stuff under `~/.m2/repository/org/mozilla/components/sync-places/`

* Use the reference-browser branch, which includes a change so the local maven repository is used for that component - and you should have a very basic auto-complete in the reference browser!
