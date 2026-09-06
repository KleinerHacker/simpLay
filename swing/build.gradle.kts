plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":engine"))
    testImplementation(kotlin("test"))
}

// Third-party licences accepted by this module.
licensee {
    allow("Apache-2.0")
    allow("MIT")
}
