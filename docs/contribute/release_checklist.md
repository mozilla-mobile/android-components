---
layout: page
title: Release checklist
permalink: /contributing/release-checklist
---

## Before the release

- Make sure version number is correct or update version in Config.kt 
- Update [CHANGELOG](https://github.com/mozilla-mobile/android-components/blob/master/docs/changelog.md)
  - Use milestone and commit log for identifying interesting changes
- Close milestone
  - Milestone should be 100% complete. Incomplete issues should have been moved in planning meeting.

## Release

- Create a new tag/release on GitHub.
  - Tag: v2.0 (_v_ prefix!)
  - Release 2.0
- Wait for taskcluster to complete successfully
- Check that new versions are on [maven.mozilla.org](https://maven.mozilla.org/?prefix=maven2/org/mozilla/components/):

## After the release

- Send release message to the [Matrix channel](https://chat.mozilla.org/#/room/#android-components:mozilla.org):
```
*Android Components 0.17 Release* (:ocean:)
Release notes: https://mozilla-mobile.github.io/android-components/changelog/
Milestone: https://github.com/mozilla-mobile/android-components/milestone/15?closed=1
```
- Update version number in repository
  - [.buildconfig.yml](https://github.com/mozilla-mobile/android-components/blob/master/.buildconfig.yml#L1)
