---
layout: page
title: Add a new Gradle module
permalink: /contributing/add-a-gradle-module
---

This document describes how to add a new Gradle module, also known as a Gradle project or subproject. For example, by following this guide, you can add a new module like `:concept-engine`.

Android Studio's built-in module creation tools do not understand android-components's module structure so its built-in tools like `File -> New -> New Module` do not work correctly: instead, we'll create the Gradle module manually.

## Pre-work
1. Check if your code can fit into an existing module
1. Figure out what to name the new module. It will be made up of two parts: e.g. `concept-engine` or `service-pocket`. For a full-list of existing prefixes, see [`./components`](https://github.com/mozilla-mobile/android-components/tree/master/components). If you're unsure, consult the team.
1. Write a one-line description for your module: this text will be repeated in several files.
1. Make your new module's directory, e.g. `mkdir -p components/service/pocket`

## Copy & update the new module template
Copy the new gradle module template to your new module's directory, e.g. `cp -R ./tools/new-gradle-module-template/* components/service/pocket`.

Modify the copied files to reference your module instead of the template:
- In `README.md`:
  - Change page title
  - Replace one-line description with your own
  - Modify dependency example
- In `src/` directory:
  - In `src/main`, update directory hierarchy to match your module, e.g. `src/main/java/mozilla/components/service/pocket`
  - In `src/test`, do the same
  - Update the `package` statement in `Placeholder.kt` and `PlaceholderTest.kt`
  - Update AndroidManifest to point at your package
- **Look out for changes we may have missed**

**N.B.:** our conventions for configuring modules may change so this template may get outdated: consult the team if you run into trouble. Please file a bug if the template is outdated.

## Update references to new module
1. Add your module to `.buildconfig.yml`: follow the existing conventions. **Without this line, gradle won't know to look at your new module and your module will not show up in Android Studio.**
1. Add your new module to the root `README.md`
1. Add your new module to `./docs/components.md`.

## Test your changes
To test your changes in Android Studio, click `File -> Sync Project with Gradle Files` and after it completes ensure your new module appears in the `Project` view.

## Examples
**N.B.:** our conventions for configuring modules may change so this example may get outdated.

This is the PR where we added `:services-pocket`: https://github.com/mozilla-mobile/android-components/pull/2247
