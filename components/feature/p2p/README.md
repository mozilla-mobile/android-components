# [Android Components](../../../README.md) > Feature > Find In Page

A feature that provides [Find in Page functionality](https://support.mozilla.org/en-US/kb/search-contents-current-page-text-or-links).

## Usage

### Setting up the dependency

Use Gradle to download the library from [maven.mozilla.org](https://maven.mozilla.org/) ([Setup repository](../../../README.md#maven-repository)):

```Groovy
implementation "org.mozilla.components:feature-p2p:{latest-version}"
```

### Adding feature to application

To use this feature you have to do two things:

**1. Add the `P2PBar` widget to you layout:**

```xml
<mozilla.components.feature.findinpage.view.P2PBar
        android:id="@+id/find_in_page"
        android:layout_width="match_parent"
        android:background="#FFFFFFFF"
        android:elevation="10dp"
        android:layout_height="56dp"
        android:padding="4dp" />
```

These are the properties that you can customize of this widget.
```xml
<attr name="p2PQueryTextColor" format="reference|color"/>
<attr name="p2PQueryHintTextColor" format="reference|color"/>
<attr name="p2PQueryTextSize" format="dimension"/>
<attr name="p2PResultCountTextColor" format="reference|color"/>
<attr name="p2PResultCountTextSize" format="dimension"/>
<attr name="p2PButtonsTint" format="reference|color"/>
<attr name="p2PNoMatchesTextColor" format="reference|color"/>
```

**2. Add the `P2PFeature` to your activity/fragment:**

```kotlin
val p2PBar = layout.findViewById<P2PBar>(R.id.find_in_page)

val p2PFeature = P2PFeature(
    sessionManager,
    p2PView
) {
    // Optional: Handle clicking of "close" button.
}

lifecycle.addObservers(p2PFeature)


## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
