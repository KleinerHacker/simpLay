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

// Toolkit-agnostic building blocks shared by every interactive GUI binding (`fx`, `swing`, and
// future toolkit modules): the linear text index, the glyph hit-test, the selection span geometry,
// the document text editor and the styled-text clipboard serialisation. Nothing here depends on a
// concrete UI toolkit; text measuring is taken through the `engine` `FontMeasureCalculator`.
dependencies {
    implementation(project(":engine"))

    testImplementation(kotlin("test"))
}

// Third-party licences accepted by this module.
licensee {
    allow("Apache-2.0")
    allow("MIT")
}
