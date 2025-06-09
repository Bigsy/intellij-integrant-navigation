plugins {
    id("java") // Still needed for IntelliJ platform
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "org.bigsy"
version = "0.0.3"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
intellij {
    version.set("2024.1") // Target IntelliJ IDEA version (more general)
    type.set("IC") // Target IDE Platform (Community Edition)

    downloadSources.set(true) // Download sources for IntelliJ Platform and plugins
    updateSinceUntilBuild.set(true) // Keep sinceBuild/untilBuild in sync with platform

    plugins.set(listOf(
        "org.jetbrains.kotlin", // Kotlin plugin itself
        "com.intellij.java", // Java support, often needed
        "com.cursiveclojure.cursive:2025.1.1-241" // Cursive plugin with specific build number
    ))

    // Optional: Use a custom sandbox directory
    // sandboxDir.set(file("${project.buildDir}/idea-sandbox").absolutePath)
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        // These will be derived from the intellij.version if not set explicitly
        // Or, you can set them in gradle.properties and reference them here
        sinceBuild.set("241") // Corresponds to 2024.1
        untilBuild.set("") // No upper limit for maximum compatibility
    }

    // Skip the buildSearchableOptions task which is causing issues
    buildSearchableOptions {
        enabled = false
    }

    // Plugin verification task
    runPluginVerifier {
        ideVersions.set(listOf("2024.1", "2024.2", "2024.3"))
    }

    // Optional: Configuration for signing and publishing the plugin
    // signPlugin { ... }
    // publishPlugin { ... }
}
