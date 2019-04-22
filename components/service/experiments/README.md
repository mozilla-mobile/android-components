# [Android Components](../../../README.md) > Service > Experiments

An Android SDK for running experiments on user segments in multiple branches.

## Usage

### Setting up the dependency

Use Gradle to download the library from [maven.mozilla.org](https://maven.mozilla.org/) ([Setup repository](../../../README.md#maven-repository)):

```Groovy
implementation "org.mozilla.components:service-experiments:{latest-version}"
```

### Initializing the Experiments library
In order to use the library, first you have to initialize it by calling `Experiments.initialize()`. You do this once per app launch 
(typically in your `Application` class `onCreate` method). You simply have to call `Experiments.initialize()` and
provide the `applicationContext` (and optionally a `Configuration` object), like this:

```Kotlin
class SampleApp : Application() {
    override fun onCreate() {
        // Glean needs to be initialized first.
        Glean.initialize(/* ... */)
        Experiments.initialize(
            applicationContext,
            configuration // This is optional, e.g. for overriding the fetch client.
        )
    }
}
```

Note that this library depends on the Glean library, which has to be initialized first. See the [Glean README](../glean/README.md) for more details.

### Updating of experiments

The library updates it's list of experiments automatically and async from Kinto on library initialization. As this is asynchronous, it will not have immediate effect.

Afterwards, the list of experiments will be updated every 6 hours.

### Checking if a user is part of an experiment
In order to check if a user is part of a specific experiment, `Experiments` provides a Kotlin-friendly
`withExperiment` API. You pass the id of the experiment you want to check and if the client is in the experiment, you get the selected branch name passed:

```Kotlin
Experiments.withExperiment("button-color-experiment") {
    when(it) { // `it` is the branch name.
      "red" -> button.setBackgroundColor(Color.RED)
      "control" -> button.setBackgroundColor(DEFAULT_COLOR)
    }
}
```

### ExperimentsDebugActivity usage
Experiments exports the [`ExperimentsDebugActivity`](src/main/java/mozilla/components/service/experiments/debug/ExperimentsDebugActivity.kt)
that can be used to trigger functionality or toggle debug features on or off. Users can invoke this special activity, at
run-time, using the following [`adb`](https://developer.android.com/studio/command-line/adb) command:

`adb shell am start -n [applicationId]/mozilla.components.service.experiments.debug.ExperimentsDebugActivity [extra keys]`

In the above:

- `[applicationId]` is the product's application id as defined in the manifest
  file and/or build script. For the glean sample application, this is
  `org.mozilla.samples.glean` for a release build and
  `org.mozilla.samples.glean.debug` for a debug build.

- `[extra keys]` is a list of extra keys to be passed to the debug activity. See the
  [documentation](https://developer.android.com/studio/command-line/adb#IntentSpec)
  for the command line switches used to pass the extra keys. These are the
  currently supported keys:

    |key|type|description|
    |---|----|-----------|
    | updateExperiments | boolean (--ez) | forces the experiments updater to run and fetch experiments immediately |

For example, to direct a release build of the glean sample application to update experiments immediately, the following command
can be used:

```
adb shell am start -n org.mozilla.samples.glean.debug/mozilla.components.service.experiments.debug.ExperimentsDebugActivity --ez updateExperiments true
```

## Experiments format for Kinto

The library loads its list of experiments from Kinto. Kinto provides data in the following JSON format:
```javascript
{
    // The whole list of experiments lives in a Kinto "collection".
    "data": [
        // Each experiments data is described in a "record".
        {
            "id":"some-experiment",
            // ... 
            "last_modified":1523549895713
        },
        // ... more records.
    ]
}
```

An individual experiment record looks e.g. like this:

```javascript
{
  "id": "button-color-expirement",
  "description": "The button color experiments tests ... per bug XYZ.",
  // Enroll 50% of the population in the experiment.
  "buckets": {
    "start": 0,
    "count": 500
  },
  // Distribute enrolled clients among two branches.
  "branches": [
    {
      "name": "control",
      "ratio": 1
    },
    {
      "name": "red-button-color",
      "ratio": 2 // Two thirds of the enrolled clients get red buttons.
    }
  ],
  "match": {
    "app_id": "^org.mozilla.firefox${'$'}",
    "locale_language": "eng|zho|deu"
    // Possibly more matchers...
  }
}
```

### Experiment fields
The experiments records in Kinto contain the following properties:

| Name                      | Type   | Required | Description                                                                                                                                     | Example                                                        |
|---------------------------|--------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| id                        | string |     x    | The unique id of the experiment.                                                                                                                | `"login-button-color-test"`                                    |
| description               | string |     x    | A detailed description of the experiment.                                                                                                       |                                                                |
| last_modified             | number |     x    | Timestamp of when this experiment was last modified. This is provided by Kinto.                                                                 |                                                                |
| schema                    | number |     x    | Timestamp of when the schema was last modified. This is provided by Kinto.                                                                      |                                                                |
| buckets                   | object |     x    | Object containing a bucket range to match users. Every user is in one of 1000 buckets (0-999).                                                  |                                                                |
|  buckets.start            | number |     x    | The minimum bucket to match.                                                                                                                    |                                                                |
| buckets.count             | number |     x    | The number of buckets to match from start. If (start + count >= 999), evaluation will wrap around.                                              |                                                                |
| branches                  | object |     x    | Object containing the parameters for ratios for randomized enrollment into different branches.                                                  |                                                                |
| branches[i].name          | string |     x    | The name of that branch.                                                                                                                        | `"control"` or `"red-button"`                                  |
| branches[i].ratio         | number |     x    | The weight to randomly distribute enrolled clients among the branches.                                                                          |                                                                |
| match                     | object |     x    | Object containing the filter parameters to match specific user groups.                                                                          |                                                                |
| match.app_id              | regex  |          | The app ID (package name)                                                                                                                       | "^org.mozilla.firefox_beta${'$'}|^org.mozilla.firefox${'$'}" |
| match.device_manufacturer | regex  |          | The Android device manufacturer.                                                                                                                                             |                                                                |
| match.device_model        | regex  |          | The Android device model.                                                                                                                                                |                                                                |
| match.locale_country      |        |          | The default locales country.                                                                                                                                                | "USA|DEU"                                                    |
| match.locale_language     |        |          | The default locales language.                                                                                                                                                | "eng|zho|deu"                                                |
| match.app_display_version |        |          | The application version.                                                                                                                                                |                                                                |
| match.regions             | array  |          | Array of strings. Not currently supported. Custom regions, different from the one from the default locale (like a GeoIP, or something similar). | `["USA", "GBR"]`                                               |
| match.debug_tags          | array  |          | Array of strings. Debug tags to match only specific client for QA of experiments launch & targeting.                                            | `["john-test-1"]`                                              |

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
