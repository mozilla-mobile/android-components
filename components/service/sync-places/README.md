# Building the Places component

This is very messy, but we are working on (well, thinking about ;) the best way to improve this.

However, this should still offer enough so that we can discuss the general shape of the component and how it is used in the reference-browser.

## Building the Rust pieces

* Check out the [places-ffi branch of the application-services repo](https://github.com/mozilla/application-services/tree/ffi-places) repo - we expect this to merge to master soon.

* Follow the [build instructions](https://github.com/mozilla/application-services/wiki/Building-the-Android-Components) to compile the rust code.

* This is where it gets really messy - after the build we need to copy:
  * `target/i686-linux-android/release/libplaces_ffi.so` into `components/service/sync-places/src/main/jniLibs/x86`

  * `components/places/android/library/src/main/java/org/mozilla/places/*.kt` into `components/service/sync-places/src/main/java/mozilla/components/service/sync/places`

You should end up with the following tree:

    components/service/sync-places/src/main
    components/service/sync-places/src/main/AndroidManifest.xml
    components/service/sync-places/src/main/java
    components/service/sync-places/src/main/java/mozilla
    components/service/sync-places/src/main/java/mozilla/components
    components/service/sync-places/src/main/java/mozilla/components/service
    components/service/sync-places/src/main/java/mozilla/components/service/sync
    components/service/sync-places/src/main/java/mozilla/components/service/sync/places
    components/service/sync-places/src/main/java/mozilla/components/service/sync/places/LibPlacesFFI.kt
    components/service/sync-places/src/main/java/mozilla/components/service/sync/places/PlacesConnection.kt
    components/service/sync-places/src/main/java/mozilla/components/service/sync/places/RustError.kt
    components/service/sync-places/src/main/jniLibs
    components/service/sync-places/src/main/jniLibs/x86
    components/service/sync-places/src/main/jniLibs/x86/libplaces_ffi.so
    components/service/sync-places/src/main/res
    components/service/sync-places/src/main/res/drawable
    components/service/sync-places/src/main/res/values
    components/service/sync-places/src/main/res/values/strings.xml

## Publish this new component to your local maven repository

Please read [this guide](https://mozilla-mobile.github.io/android-components/contributing/testing-components-inside-app) for all the gritty details, but, roughly:

* Note that this branch has already modified `buildSrc/src/main/java/Config.kt` by appending `-SNAPSHOT` to the components version.

* Execute `gradlew service-sync-places:install` to publish it locally. Sanity check that there is stuff under `~/.m2/repository/org/mozilla/components/sync-places/`

* Use the reference-browser branch, which includes a change so the local maven repository is used for that component - and you should have a very basic auto-complete in the reference browser!
