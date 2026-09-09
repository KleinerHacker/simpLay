/*
 * Copyright (c) KleinerHacker alias Pfeiffer C Soft 2026.
 * This work is licensed under the Apache License, Version 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, this software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations.
 */

plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    // JavaFX is only needed in this module (the JavaFX integration), not project-wide.
    alias(libs.plugins.javafx)
}

javafx {
    version = "25"
    modules("javafx.controls", "javafx.fxml")
    // Expose JavaFX transitively: the public API of this module uses JavaFX types.
    configuration = "api"
}

// A dedicated source set for the runnable demo application. It depends on `main` and its
// dependencies, is never published and is kept out of the licensee scan (only `main`'s runtime
// classpath is scanned) and out of the `java` component.
val demo: SourceSet = sourceSets.create("demo") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations.named("demoImplementation") { extendsFrom(configurations.named("implementation").get()) }
configurations.named("demoRuntimeOnly") { extendsFrom(configurations.named("runtimeOnly").get()) }

dependencies {
    implementation(project(":engine"))
    implementation(project(":ui:common"))

    testImplementation(kotlin("test"))
    testImplementation(libs.testfxCore)
    testImplementation(libs.testfxJunit5)
    testImplementation(libs.testfxMonocle)
}

// Run the demo application. The main entry point is a top-level `main` function (main class
// `DemoAppKt`), not an `Application` subclass, so JavaFX starts fine from the plain classpath
// without an explicit module path.
tasks.register<JavaExec>("run") {
    group = "application"
    description = "Runs the JavaFX demo application."
    mainClass.set("org.pcsoft.framework.simplay.fx.demo.DemoAppKt")
    classpath = demo.runtimeClasspath
}

// Headless JavaFX for the UI tests via TestFX + Monocle.
tasks.withType<Test>().configureEach {
    systemProperty("java.awt.headless", "true")
    systemProperty("testfx.robot", "glass")
    systemProperty("testfx.headless", "true")
    systemProperty("glass.platform", "Monocle")
    systemProperty("monocle.platform", "Headless")
    systemProperty("prism.order", "sw")
    systemProperty("prism.text", "t2k")
}

// Third-party licences accepted by this module.
licensee {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-2-Clause")
    allow("BSD-3-Clause")

    // JavaFX publishes GPL-2.0 with Classpath Exception, but only as a URL - it carries no SPDX id
    // in its POM, so it has to be allowed by that URL.
    allowUrl("https://openjdk.java.net/legal/gplv2+ce.html") {
        because("GPL-2.0 with Classpath Exception")
    }
}
