plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Deliberately a plain Kotlin/JVM module: the domain vocabulary of MapMe has
// no business knowing about Android. It keeps the geometry testable on the JVM
// in milliseconds, which is where the GPS-cleaning logic will live.
kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
