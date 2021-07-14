---
title: Persistent logging component
layout: page
permalink: /rfc/0000-persistent-logger
---

* Start date: 2020-10-10
* RFC PR: [#0000](https://github.com/mozilla-mobile/android-components/pull/0000)

## Summary
Introduce a component for recording application logs on a device to a persistent store that can be shared with developers for debugging.

## Motivation
Reproducing bugs that a user encounters in the wild is a difficult process for the developer to get their device in a similar state in order to debug. We have useful information in our Android logs today that aid in debugging that we also do not have access to, but give us insight into the app state.

With a component that logs to disk for later, we can follow changes of state within the browser and crash info that can be shared with developers or SUMO to allow for more useful debugging.

Asking users for `adb logcat` output is a common step when trying to debug certain kinds of problems (such as account, push, sync, etc). However, for non-technical users this could be tricky, and for technical users this request assumes presence of `adb`, or willingness to install it. The easier we can make log extraction, the more accessible this process becomes for a diverse audience, helping us build a more robust application.

## Guide-level explanation

One requirement that we wanted to introduce is that logging should not happen in the background unless explicitly turned on. Some advantages for this:
 - We want to target reproducible bugs, and not random crashes, which we have a component for that (`lib-crash`).
 - There is less IO done on production devices that are running just fine.
   - Performing on-demand recording allows us to vastly simplify how we process the logs.
   - The UI becomes more intuitive, since we now have a recording unit which can be viewed, shared, deleted, etc. A simple stream of log statements doesn't have similar affordances.
 - We can avoid many maintainence tasks like an ever growing database of logs that need to be flushed.
 - The user can be explicit with the scope of what is collected.

### Recording

> **Note**: We use the term "recording" here to mean an event which allows logs to be stored to the database that begin with a 'start' action and ends with a 'stop' action from the user or the app crashes. Each 'start' generates a new recording ID. This is a similar concept to a screen recording of a bug being reproduced in a GIF.

In order to do this, we want to introduce a persistent notification service that would be show up only when enabled, similar to how USB debugging requires explicit action to enable. When a bug is ready to be reproduced, the user can click on a 'Record' button in the notification, perform the steps to reproduce the bug, and click the 'Stop' button right after. To capture, a 'Record after on startup' option will be possible as well to capture startup logging.

From there, a user can share the logging via the UI or notification.

### User Interface

The UI would list all the recordings in the database that you can select to see the full logs for that record.

In the UI for a selected recording, the rows of data collected with options to filter by the various attributes and their combinations (a description of these types can be see in the Table Schema section):

 - recording ID
 - tag
 - severity

The logs can then be shared with the Android system share service to be attached to an email, Github issue, clipboard, etc. This possibly not ideal, see the Drawbacks section for more details.

Similar to `lib-crash` most of the basic UI will be written into an abtract activity/fragment.

## Reference-level explanation

### Table Schema

The schema below describes the primary log table that will be used to store logs:

| Column       | Description                                                  | Type    |
| ------------ | ------------------------------------------------------------ | ------- |
| id           | Primary key for each log line.                               | Long    |
| timestamp    | The time when the log event had occurred.                    | Long    |
| recording_id | A random UUID generated at the start of a log recording.     | String  |
| severity     | The priority of the log statement. See Android's [Log](https://developer.android.com/reference/android/util/Log#ERROR) constants. | Char    |
| tag          | Used to identify the source of a log message.  It usually identifies the class or activity where the log call occurs. | String? |
| log          | The message you would like logged.                           | String? |
| throwable    | Optional. An exception to log.                               | String? |

A secondary table is for storing metadata for each recording:

| Column          | Description                            | Type |
| --------------- | -------------------------------------- | ---- |
| recording_id    | Primary key; foreign key for log table | Long |
| start_timestamp | When the recording was started.        | Long |
| end_timestamp   | When the recording was ended.          | Long |



## Drawbacks

- Sharing logs through an intent is fallible and can throw exceptions if the contents are more than the Android OS can handle.
- User-initiated recording would mean we can lose logs that are not easily reproducible. We can always re-consider changing this in the future.

## Future Work

- We can request an API to stream GeckoView logs to our `LogSink` .
- Additional useful device info can be prepended to the logs. For example:
  - Delete a log entry
  - System information
  - Feature flags
  - Account state (etc. logged in/out, sync engines enabled)
  - Number of tabs open
  - Size of Places database
  - Active experiments
  - Permissions are granted to the app
  - WorkManager statue (e.g. scheduled/active/running)

## Unresolved questions

- Can we allow users to share logs with PII?
  - This requires PII-aware logging and requires auditing our logs to ensure this.
- Is there an existing service at Mozilla we can use to upload logs so they only need to share a link to it?
  - It can be a self-cleaning service that deletes logs n-days later.
  - Kinto?
  - Can Socorro parse logs?

## Prior art

- The Signal app has similar constraints to PII and have solved our sharing log problem by uploading the logs to an owner S3 bucket for the user to share in a support request or Github issue. They also collect similar device info listed in 'Future Work'.
  - Logging source code: https://github.com/signalapp/Signal-Android/tree/master/app/src/main/java/org/thoughtcrime/securesms/logging
  - Log viewer code: https://github.com/signalapp/Signal-Android/tree/master/app/src/main/java/org/thoughtcrime/securesms/logsubmit

