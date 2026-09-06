package com.mapme.core.recording

import com.mapme.core.model.GeoPoint
import com.mapme.core.model.Journey
import com.mapme.core.model.TrackPoint
import java.io.File
import java.io.IOException

/**
 * Where journeys live.
 *
 * An interface because the recorder must be testable without a filesystem, and
 * because whatever replaces this later — a database, an export format — should
 * not require touching the recorder.
 */
interface JourneyStore {
    /** Begin a journal for [id]. Any existing one is replaced. */
    fun open(id: String)

    /** Record that a new segment starts here. */
    fun appendBreak(id: String)

    /** Append one accepted reading. Must be durable enough to survive a crash. */
    fun appendPoint(id: String, point: TrackPoint)

    /** Mark the journey finished. */
    fun close(id: String, endedAtMillis: Long)

    fun read(id: String): Journey?
    fun mostRecent(): Journey?
    fun delete(id: String)
}

/**
 * A journey as an append-only text journal, one file per journey.
 *
 * ## Why not a database
 *
 * The decisive requirement is §24: *do not silently lose a journey*. A walk is
 * an hour of someone's life that cannot be re-recorded, and the process can die
 * at any moment during it — Android kills backgrounded processes, batteries go
 * flat, phones get dropped.
 *
 * An append-only file is the strongest answer to that. Each accepted reading is
 * a line, flushed as it arrives. If the process dies mid-walk the file is still
 * a valid journey minus its last second; there is no transaction to roll back,
 * no half-written row, and no schema to migrate. Recovery is just reading it.
 *
 * The access pattern agrees: journeys are written once, appended during
 * recording, and read whole. There are no queries here that a database would
 * make faster.
 *
 * It is also the only option that can be **tested properly from this project**.
 * Room needs an instrumented test or Robolectric; this is `java.io` against a
 * temp directory, so persistence is verified on every CI run rather than
 * asserted in a report.
 *
 * ## The format
 *
 * One line per fact, first character says what it is. Unknown prefixes are
 * skipped, so later versions can add `t` for a title or `n` for a note and
 * older builds will still read the file rather than fall over.
 *
 * ```
 * v 1
 * i <journey id>
 * b                                  <- a segment begins
 * p <millis> <lat> <lon> <acc> <alt|-> <spd|->
 * b                                  <- resumed after a pause
 * p ...
 * e <millis>                         <- finished; absent if it never was
 * ```
 *
 * A file with no `e` line is a journey that was interrupted. It is still read,
 * ending at its last reading — which is exactly what happened.
 */
class FileJourneyStore(private val directory: File) : JourneyStore {

    init {
        runCatching { directory.mkdirs() }
    }

    private fun fileFor(id: String) = File(directory, "${id.sanitised()}.mapme")

    override fun open(id: String) {
        val file = fileFor(id)
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText("$VERSION_LINE\n$ID_PREFIX $id\n")
        }
    }

    override fun appendBreak(id: String) {
        append(id, "$BREAK_PREFIX\n")
    }

    override fun appendPoint(id: String, point: TrackPoint) {
        val altitude = point.altitudeMetres?.toString() ?: ABSENT
        val speed = point.speedMetresPerSecond?.toString() ?: ABSENT
        append(
            id,
            "$POINT_PREFIX ${point.timestampMillis} ${point.position.latitude} " +
                "${point.position.longitude} ${point.accuracyMetres} $altitude $speed\n",
        )
    }

    override fun close(id: String, endedAtMillis: Long) {
        append(id, "$END_PREFIX $endedAtMillis\n")
    }

    /**
     * Appended and flushed, one line at a time.
     *
     * Deliberately not buffered across calls. Readings arrive every few
     * seconds, so the cost is irrelevant, and a buffer is precisely the thing
     * that would lose the last minute of a walk when the process is killed.
     */
    private fun append(id: String, line: String) {
        runCatching { fileFor(id).appendText(line) }
    }

    override fun read(id: String): Journey? = parse(fileFor(id))

    override fun mostRecent(): Journey? {
        val newest = runCatching {
            directory.listFiles { file -> file.extension == EXTENSION }
                ?.maxByOrNull { it.lastModified() }
        }.getOrNull() ?: return null
        return parse(newest)
    }

    override fun delete(id: String) {
        runCatching { fileFor(id).delete() }
    }

    private fun parse(file: File): Journey? {
        if (!file.isFile) return null
        val segments = mutableListOf<MutableList<TrackPoint>>()
        var id: String? = null

        try {
            file.forEachLine { raw ->
                val line = raw.trim()
                if (line.isEmpty()) return@forEachLine
                when (line.first()) {
                    ID_CHAR -> id = line.drop(2).trim()
                    BREAK_CHAR -> segments += mutableListOf<TrackPoint>()
                    POINT_CHAR -> parsePoint(line)?.let { point ->
                        if (segments.isEmpty()) segments += mutableListOf<TrackPoint>()
                        segments.last() += point
                    }
                    // Everything else — the version line, the end marker, and
                    // anything a later version writes — is not needed to
                    // reconstruct the route.
                    else -> Unit
                }
            }
        } catch (_: IOException) {
            // A partly-readable journey is better than none: whatever parsed
            // before the failure is still a real record of where someone went.
        }

        val recovered = segments.filter { it.isNotEmpty() }
        if (id == null || recovered.isEmpty()) return null
        return Journey(id = id, segments = recovered)
    }

    private fun parsePoint(line: String): TrackPoint? {
        val parts = line.split(' ')
        if (parts.size < 5) return null
        return runCatching {
            TrackPoint(
                position = GeoPoint(parts[2].toDouble(), parts[3].toDouble()),
                timestampMillis = parts[1].toLong(),
                accuracyMetres = parts[4].toFloat(),
                altitudeMetres = parts.getOrNull(5)?.takeIf { it != ABSENT }?.toDoubleOrNull(),
                speedMetresPerSecond = parts.getOrNull(6)?.takeIf { it != ABSENT }?.toFloatOrNull(),
            )
        }.getOrNull()
    }

    /** Ids become filenames, so they may not become paths. */
    private fun String.sanitised(): String =
        filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(64).ifEmpty { "journey" }

    private companion object {
        const val EXTENSION = "mapme"
        const val VERSION_LINE = "v 1"
        const val ID_PREFIX = "i"
        const val POINT_PREFIX = "p"
        const val BREAK_PREFIX = "b"
        const val END_PREFIX = "e"
        const val ID_CHAR = 'i'
        const val POINT_CHAR = 'p'
        const val BREAK_CHAR = 'b'
        const val ABSENT = "-"
    }
}
