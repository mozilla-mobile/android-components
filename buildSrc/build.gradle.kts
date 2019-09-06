/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

plugins {
    `kotlin-dsl`
    java
    `java-gradle-plugin`
}

repositories {
    jcenter()
}

gradlePlugin {
    plugins {
        create("gleanPlugins") {
            id = "mozilla.components.tooling.glean-gradle-plugin"
            implementationClass = "GleanPlugin"
        }
    }
}

dependencies {
    compile("com.squareup.okhttp3:okhttp:3.12.1")
    compile("com.squareup.okio:okio:1.17.2@jar")
    compileOnly(gradleApi())
}
