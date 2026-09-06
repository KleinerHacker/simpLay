# engine - Implementation

The `engine` module is the platform-independent simulation core, built with Kotlin
Multiplatform.

## Add the dependency

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:<version>")
}
```

The artifacts are published to GitHub Packages
(`https://maven.pkg.github.com/KleinerHacker/simPlay`). For a Kotlin Multiplatform
consumer add the target-specific artifact (for example `simplay-engine-jvm`) or
rely on Gradle metadata resolution.

## Entry points

!!! note

    The public API of the simulation core is not available yet. This page will
    document the entry points, configuration and lifecycle as soon as the
    `engine` API is released.
