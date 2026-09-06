plugins {
    // The Kotlin DSL plugin provides a convenient way to develop convention plugins.
    // Convention plugins are located in `src/main/kotlin`, with the file extension `.gradle.kts`,
    // and are applied in the project's `build.gradle.kts` files as required.
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    // Add a dependency on the Kotlin Gradle plugin, so that convention plugins can apply it.
    implementation(libs.kotlinGradlePlugin)
    // Gradle plugin artifacts the convention plugins apply on every module: API docs (Dokka),
    // coverage (Kover) and the license check (licensee).
    implementation(libs.dokkaGradlePlugin)
    implementation(libs.koverGradlePlugin)
    implementation(libs.licenseeGradlePlugin)
}
