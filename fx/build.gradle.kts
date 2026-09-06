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
