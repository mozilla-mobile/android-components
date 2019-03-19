# [Android Components](../../../README.md) > Libraries > QR

A QR reader component that can read QR codes.

Main features:

* Reading QR codes.

## Usage

### Setting up the dependency

Use Gradle to download the library from [maven.mozilla.org](https://maven.mozilla.org/) ([Setup repository](../../../README.md#maven-repository)):

```Groovy
implementation "org.mozilla.components:feature-qr:{latest-version}"
```

### Setting up the QR reader

Create the feature:
```
qrFeature = QrFeature(
    this,
    fragmentManager = supportFragmentManager,
    onNeedToRequestPermissions = { permissions ->
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_PERMISSIONS)
    },
    onScanResult = { scanResult ->

    }
)
```

When ready to scan use the following:

```
qrFeature.scan(android.R.id.content);
```

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
