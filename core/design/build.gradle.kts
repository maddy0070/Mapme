import java.net.URL

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mapme.core.design"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.text)
    api(libs.androidx.compose.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}

/**
 * MapMe ships two open-source (SIL OFL 1.1) variable typefaces. They are not
 * committed as binaries; this task pulls them into the asset folder so a build
 * machine with network gets the real brand voice.
 *
 * If the download is unavailable — offline machine, restricted network, or a
 * corporate proxy — the build still succeeds and MapMeTheme falls back to the
 * platform grotesque with the MapMe metrics applied. See BrandFonts.kt.
 */
val brandFonts = mapOf(
    "Inter.ttf" to "https://github.com/google/fonts/raw/main/ofl/inter/Inter%5Bopsz,wght%5D.ttf",
    "SpaceGrotesk.ttf" to "https://github.com/google/fonts/raw/main/ofl/spacegrotesk/SpaceGrotesk%5Bwght%5D.ttf",
    "OFL-Inter.txt" to "https://github.com/google/fonts/raw/main/ofl/inter/OFL.txt",
    "OFL-SpaceGrotesk.txt" to "https://github.com/google/fonts/raw/main/ofl/spacegrotesk/OFL.txt",
)

val fetchBrandFonts by tasks.registering {
    group = "mapme"
    description = "Downloads the MapMe brand typefaces into core/design assets (best effort)."

    val outputDir = layout.projectDirectory.dir("src/main/assets/fonts")
    val sources = brandFonts
    outputs.dir(outputDir)
    outputs.upToDateWhen { sources.keys.all { outputDir.file(it).asFile.exists() } }

    doLast {
        val dir = outputDir.asFile
        dir.mkdirs()
        sources.forEach { (fileName, url) ->
            val target = dir.resolve(fileName)
            if (target.exists() && target.length() > 0L) return@forEach
            runCatching {
                URL(url).openStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }.onFailure { error ->
                target.delete()
                logger.lifecycle(
                    "MapMe: could not fetch $fileName (${error.message}). " +
                        "Building with the fallback typeface — see docs/DESIGN_SYSTEM.md.",
                )
            }
        }
    }
}

tasks.named("preBuild") { dependsOn(fetchBrandFonts) }
