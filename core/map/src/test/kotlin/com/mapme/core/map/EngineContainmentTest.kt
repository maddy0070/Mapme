package com.mapme.core.map

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The map engine must stay replaceable.
 *
 * The brief for this sprint is explicit that MapMe must not become welded to a
 * map provider, and that is not a promise a code review can keep: a single
 * `import org.maplibre.android.geometry.LatLng` in a screen, added because it
 * was the quickest way to pass a coordinate, is how the welding starts. By the
 * time anyone notices, swapping engines means touching thirty files.
 *
 * So it is checked. The engine may be imported in exactly one file, and this
 * test names it.
 */
class EngineContainmentTest {

    // Two files, both in :core:map, both named. The trail is separate from the
    // surface because it is a different job — one owns a view's lifecycle, the
    // other owns a source and two layers — and a single file doing both is how
    // this stops being reviewable.
    private val allowed = setOf("MapMeMap.kt", "MapTrail.kt")

    @Test
    fun `only the map surface knows which engine draws it`() {
        val root = repositoryRoot()
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.contains("${File.separator}build${File.separator}") }
            .filter { file -> file.readLines().any { it.trimStart().startsWith("import org.maplibre") } }
            .map { it.relativeTo(root).path }
            .toList()

        offenders.forEach { path ->
            assertTrue(
                "$path imports the map engine. Only ${allowed.joinToString()} may — everything " +
                    "else goes through MapMeMap, MapCameraState and MapMeStyle, or the engine " +
                    "stops being replaceable.",
                File(path).name in allowed,
            )
        }

        assertTrue(
            "the engine is imported in ${offenders.size} files. It is allowed in " +
                "${allowed.size}, both inside :core:map. Every file added here makes the " +
                "engine harder to replace, so adding one is a decision, not a detail.",
            offenders.size <= allowed.size,
        )
    }

    /**
     * Walks up from the test's working directory until it finds the build.
     *
     * Gradle runs unit tests with the module as the working directory, but
     * that is a convention rather than a guarantee, so this looks for a
     * landmark instead of counting `..`s.
     */
    private fun repositoryRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("could not find the repository root from ${System.getProperty("user.dir")}")
    }
}
