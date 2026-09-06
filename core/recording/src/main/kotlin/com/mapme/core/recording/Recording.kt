package com.mapme.core.recording

import android.content.Context
import java.io.File

/**
 * The one recorder in the process.
 *
 * ## Why a singleton, deliberately
 *
 * A journey outlives every UI object that might otherwise own it. It has to
 * survive rotation, the Activity being recreated, the screen going off, and
 * the person leaving the app entirely with the phone in a pocket — and it has
 * to be the *same* journey seen by both the screen and the foreground service.
 *
 * A ViewModel survives rotation and nothing else. Passing the recorder into
 * the service through an Intent would mean two recorders disagreeing about
 * the same walk. So it lives for the life of the process, which is the actual
 * lifetime of the thing it represents.
 *
 * The cost is a global, and it is worth naming: this is the one in MapMe, it
 * holds no Activity, and it is created from the application context.
 */
object Recording {

    @Volatile
    private var instance: JourneyRecorder? = null

    @Volatile
    private var storeInstance: JourneyStore? = null

    fun store(context: Context): JourneyStore =
        storeInstance ?: synchronized(this) {
            storeInstance ?: FileJourneyStore(journeysDirectory(context)).also { storeInstance = it }
        }

    fun recorder(context: Context): JourneyRecorder =
        instance ?: synchronized(this) {
            instance ?: JourneyRecorder(store(context)).also { instance = it }
        }

    /**
     * Internal storage, not external.
     *
     * Journeys are the most sensitive thing MapMe holds. `filesDir` is private
     * to the app and not readable by other apps or by anyone with the SD card;
     * external storage would make a person's movements world-readable, and
     * `allowBackup` is already false so this never leaves the device.
     */
    private fun journeysDirectory(context: Context) =
        File(context.applicationContext.filesDir, "journeys")
}
