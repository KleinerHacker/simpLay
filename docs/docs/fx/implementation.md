# fx - Implementation

The `fx` module binds a running simulation from the `engine` core to a JavaFX user
interface.

## Add the dependency

```kotlin
dependencies {
    implementation("org.pcsoft.framework:simplay-engine:<version>")
    implementation("org.pcsoft.framework:simplay-fx:<version>")
}
```

JavaFX is exposed transitively by this module, so no separate JavaFX dependency is
required in the consuming build.

## Entry points

!!! note

    The public API of the JavaFX integration is not available yet. This page will
    document the entry points and configuration as soon as the `fx` API is
    released.
