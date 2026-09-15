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
}

// A dedicated source set for the runnable demo application. It depends on `main` and its
// dependencies, is never published and is kept out of the licensee scan (only `main`'s runtime
// classpath is scanned) and out of the `java` component. Swing is part of the JDK, so no toolkit
// dependency is added.
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
}

// The UI tests build Swing components and paint them into off-screen images; they never open a
// window, so they run under the headless AWT toolkit.
tasks.withType<Test>().configureEach {
    systemProperty("java.awt.headless", "true")
}

// Run the demo application. The main entry point is a top-level `main` function (main class
// `DemoAppKt`).
tasks.register<JavaExec>("run") {
    group = "application"
    description = "Runs the Swing demo application."
    mainClass.set("org.pcsoft.framework.simplay.swing.demo.DemoAppKt")
    classpath = demo.runtimeClasspath
}

// Third-party licences accepted by this module.
licensee {
    allow("Apache-2.0")
    allow("MIT")
}
