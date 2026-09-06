// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin in JVM projects.
    kotlin("jvm")
    base
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
    id("app.cash.licensee")
}

// Shared Maven coordinates for every module.
group = "org.pcsoft.framework"

// A release passes the tag as -PreleaseVersion=<tag>; a local build stays on the snapshot.
version = (project.findProperty("releaseVersion") as String?)?.takeIf { it.isNotBlank() } ?: "1.0-SNAPSHOT"

kotlin {
    // Use a specific Java version to make it easier to work in different environments.
    jvmToolchain(25)
}

java {
    withSourcesJar()
}

// Every module produces an artifact named `simplay-<module>`.
base {
    archivesName.set("simplay-${project.name}")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    // Configure all test Gradle tasks to use JUnitPlatform.
    useJUnitPlatform()

    // Log information about all test results, not only the failed ones.
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}

// Publishes the module (main jar + sources jar) to GitHub Packages so it can be consumed via
// Maven/Gradle. Credentials come from the environment - GITHUB_TOKEN is provided by GitHub
// Actions automatically; locally they fall back to empty strings, which simply makes `publish`
// fail authentication rather than fail to configure.
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "simplay-${project.name}"
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/KleinerHacker/simPlay")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}

// The licence allow-list is declared explicitly per module (see each module's build.gradle.kts),
// so every module states exactly which third-party licences it accepts.
