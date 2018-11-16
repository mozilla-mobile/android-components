/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// Synchronized version numbers for dependencies used by (some) modules
private object Versions {
    const val kotlin = "1.3.0"
    const val coroutines = "1.0.1"

    const val junit = "4.12"
    const val robolectric = "4.0-alpha-3"
    const val mockito = "2.23.0"

    const val mockwebserver = "3.10.0"

    const val support_libraries = "28.0.0"
    const val constraint_layout = "1.1.2"
    const val workmanager = "1.0.0-alpha09"

    const val dokka = "0.9.16"
    const val android_gradle_plugin = "3.1.4"
    const val maven_gradle_plugin = "2.1"
    const val lint = "26.1.3"

    const val jna = "4.5.2"

    const val sentry = "1.7.10"
    const val okhttp = "3.11.0"

    const val mozilla_fxa = "0.10.0"
    const val mozilla_sync_logins = "0.10.0"
    const val mozilla_places = "0.10.0"
    const val servo = "0.0.1.20181017.aa95911"
}

// Synchronized dependencies used by (some) modules
@Suppress("MaxLineLength")
object Dependencies {
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val kotlin_coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    const val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"

    const val testing_junit = "junit:junit:${Versions.junit}"
    const val testing_robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    const val testing_mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val testing_mockwebserver = "com.squareup.okhttp3:mockwebserver:${Versions.mockwebserver}"

    const val support_annotations = "com.android.support:support-annotations:${Versions.support_libraries}"
    const val support_cardview = "com.android.support:cardview-v7:${Versions.support_libraries}"
    const val support_design = "com.android.support:design:${Versions.support_libraries}"
    const val support_recyclerview = "com.android.support:recyclerview-v7:${Versions.support_libraries}"
    const val support_appcompat = "com.android.support:appcompat-v7:${Versions.support_libraries}"
    const val support_customtabs = "com.android.support:customtabs:${Versions.support_libraries}"
    const val support_fragment = "com.android.support:support-fragment:${Versions.support_libraries}"
    const val support_constraintlayout = "com.android.support.constraint:constraint-layout:${Versions.constraint_layout}"
    const val support_compat = "com.android.support:support-compat:${Versions.support_libraries}"

    const val arch_workmanager = "android.arch.work:work-runtime:${Versions.workmanager}"

    const val tools_dokka = "org.jetbrains.dokka:dokka-android-gradle-plugin:${Versions.dokka}"
    const val tools_androidgradle = "com.android.tools.build:gradle:${Versions.android_gradle_plugin}"
    const val tools_kotlingradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val tools_mavengradle = "com.github.dcendents:android-maven-gradle-plugin:${Versions.maven_gradle_plugin}"

    const val tools_lint = "com.android.tools.lint:lint:${Versions.lint}"
    const val tools_lintapi = "com.android.tools.lint:lint-api:${Versions.lint}"
    const val tools_linttests = "com.android.tools.lint:lint-tests:${Versions.lint}"

    const val mozilla_fxa = "org.mozilla.fxaclient:fxaclient:${Versions.mozilla_fxa}"
    const val mozilla_sync_logins = "org.mozilla.sync15:logins:${Versions.mozilla_sync_logins}"
    const val mozilla_places = "org.mozilla.places:places:${Versions.mozilla_places}"
    const val mozilla_servo = "org.mozilla.servoview:servoview-armv7:${Versions.servo}"

    const val thirdparty_okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    const val thirdparty_sentry = "io.sentry:sentry-android:${Versions.sentry}"

    const val jna = "net.java.dev.jna:jna:${Versions.jna}@aar"
    const val jnaForTest = "net.java.dev.jna:jna:${Versions.jna}@jar"
}
