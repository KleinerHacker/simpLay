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

dependencies {
    implementation(project(":engine"))
    testImplementation(kotlin("test"))
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
