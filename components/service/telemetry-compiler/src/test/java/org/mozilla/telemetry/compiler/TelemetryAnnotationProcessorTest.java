/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.telemetry.compiler;


import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;
import org.mozilla.telemetry.annotation.TelemetryDoc;

import java.io.File;

import javax.lang.model.SourceVersion;
import javax.tools.JavaFileObject;

public class TelemetryAnnotationProcessorTest {

    @Test(expected = IllegalStateException.class)
    public void missing_default_value_for_object() {
        final ImmutableList<JavaFileObject> telemetryWrapper = Compiler.javac()
                .withProcessors(new TelemetryAnnotationProcessor())
                .compile(
                        JavaFileObjects.forSourceLines(
                                "TelemetryWrapper",
                                "package com.bumptech.glide.test;",
                                "import org.mozilla.telemetry.annotation.TelemetryDoc;\n",
                                "import org.mozilla.telemetry.annotation.TelemetryExtra;\n",
                                "class TelemetryWrapper {" +
                                        "@TelemetryDoc(\n" +
                                        "        name = \"n\",\n" +
                                        "        category = \"a\",\n" +
                                        "        method = \"m\",\n" +
//                                        "        object = \"o\",\n" +
                                        "        value = \"v\",\n" +
                                        "        extras = {@TelemetryExtra(name = \"a\", value = \"v\")})",
                                "        public void send(){" +
                                        "}",
                                "}"
                        )).generatedFiles();
        assert (telemetryWrapper.size() > 0);
        assert (new File(TelemetryAnnotationProcessor.FILE_README).exists());
    }

    @Test
    public void should_generate_one_document() {
        final ImmutableList<JavaFileObject> telemetryWrapper = Compiler.javac()
                .withProcessors(new TelemetryAnnotationProcessor())
                .compile(
                        JavaFileObjects.forSourceLines(
                                "TelemetryWrapper",
                                "package com.bumptech.glide.test;",
                                "import org.mozilla.telemetry.annotation.TelemetryDoc;\n",
                                "import org.mozilla.telemetry.annotation.TelemetryExtra;\n",
                                "class TelemetryWrapper {" +
                                        "@TelemetryDoc(\n" +
                                        "        name = \"n\",\n" +
                                        "        category = \"a\",\n" +
                                        "        method = \"m\",\n" +
                                        "        object = \"o\",\n" +
                                        "        value = \"v\",\n" +
                                        "        extras = {@TelemetryExtra(name = \"a\", value = \"v\")})",
                                "        public void send(){" +
                                        "}",
                                "}"
                        )).generatedFiles();
        assert (telemetryWrapper.size() > 0);
    }

    @org.junit.Test
    public void getSupportedSourceVersion() {
        assert (new TelemetryAnnotationProcessor().getSupportedSourceVersion() == SourceVersion.latestSupported());
    }

    @org.junit.Test
    public void getSupportedAnnotationTypes() {
        assert (new TelemetryAnnotationProcessor().getSupportedAnnotationTypes().contains(TelemetryDoc.class.getCanonicalName()));
    }

}