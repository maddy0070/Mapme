package com.mapme.core.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.mapme.core.location.LocationProvider
import com.mapme.core.location.SystemLocationProvider
import com.mapme.core.model.GeoPoint
import com.mapme.core.model.TrackPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * Keeps the walk going when the screen does not.
 *
 * ## Why this exists at all
 *
 * A journey is recorded by someone walking with a phone in a pocket. The
 * screen locks within a minute, and from that moment an app without a
 * foreground service is a process Android is free to stop sampling or kill.
 * Recording that quietly loses most of every walk would be worse than not
 * offering recording — so this is a `location`-typed foreground service, with
 * the notification Android requires and the disclosure the person deserves.
 *
 * ## What it does not do
 *
 * It does not own the journey. [Recording] does, for the life of the process,
 * so the screen and the service are always looking at the same walk. This
 * class only supplies two things the recorder cannot get for itself: a reason
 * for Android to keep the process alive, and a stream of locations.
 *
 * ## Honest limits
 *
 * A foreground service survives the screen going off and the app being
 * backgrounded. It does **not** make the process immortal — under real memory
 * pressure Android can still kill it. That case is covered by the journal
 * rather than pretended away: every accepted reading is already on disk, so a
 * killed process loses seconds, not the walk, and the journey is recovered
 * on next launch.
 */
class RecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collection: Job? = null
    private var provider: LocationProvider? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        provider = SystemLocationProvider(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val recorder = Recording.recorder(applicationContext)

        when (intent?.action) {
            ACTION_START -> {
                goForeground(recorder.snapshot.value)
                recorder.start()
                collect(recorder)
            }

            ACTION_PAUSE -> {
                recorder.pause()
                collection?.cancel()
                collection = null
                notify(recorder.snapshot.value)
            }

            ACTION_RESUME -> {
                recorder.resume()
                collect(recorder)
                notify(recorder.snapshot.value)
            }

            ACTION_STOP -> {
                recorder.stop()
                stop()
            }

            else -> {
                // Restarted by the system with no intent to replay. There is
                // no live recorder in a fresh process, so there is nothing to
                // continue — the journal holds whatever was walked, and the
                // app offers it back on next launch.
                stop()
            }
        }
        return START_NOT_STICKY
    }

    private fun collect(recorder: JourneyRecorder) {
        collection?.cancel()
        val source = provider ?: return
        if (!source.access().canLocate) {
            recorder.fail(RecordingProblem.PermissionLost)
            stop()
            return
        }
        collection = scope.launch {
            source.stream().collectLatest { location ->
                recorder.onLocation(
                    TrackPoint(
                        position = GeoPoint(location.point.latitude, location.point.longitude),
                        timestampMillis = System.currentTimeMillis(),
                        accuracyMetres = location.accuracyMetres,
                    ),
                )
                notify(recorder.snapshot.value)
            }
        }
    }

    private fun stop() {
        collection?.cancel()
        collection = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        collection?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun goForeground(snapshot: RecordingSnapshot) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, build(snapshot), type)
    }

    private fun notify(snapshot: RecordingSnapshot) {
        val manager = getSystemService<NotificationManager>() ?: return
        // Silently ignored when the person has not granted notifications. The
        // service keeps running; only its label is missing.
        runCatching { manager.notify(NOTIFICATION_ID, build(snapshot)) }
    }

    /**
     * The notification.
     *
     * Says how far and how long. **Never says where** — a notification is
     * visible on a lock screen, and a coordinate on a lock screen is a
     * location leak to anyone who picks the phone up.
     */
    private fun build(snapshot: RecordingSnapshot): Notification {
        val paused = snapshot.state == RecordingState.Paused
        val title = getString(
            if (paused) R.string.recording_title_paused else R.string.recording_title,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_mapme_recording)
            .setContentTitle(title)
            .setContentText(summary(snapshot))
            .setOngoing(!paused)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            // Distance and duration only, but a lock screen is a public
            // surface and this is a journey. Keep it off it.
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)

        if (paused) {
            builder.addAction(0, getString(R.string.recording_resume), action(ACTION_RESUME))
        } else {
            builder.addAction(0, getString(R.string.recording_pause), action(ACTION_PAUSE))
        }
        builder.addAction(0, getString(R.string.recording_stop), action(ACTION_STOP))

        return builder.build()
    }

    private fun summary(snapshot: RecordingSnapshot): String {
        val minutes = snapshot.recordedMillis / 60_000
        val metres = snapshot.distanceMetres
        val distance = if (metres >= 1_000) {
            "%.2f km".format(metres / 1_000)
        } else {
            "${metres.toInt()} m"
        }
        return "$distance · $minutes min"
    }

    private fun action(name: String): PendingIntent = PendingIntent.getService(
        this,
        name.hashCode(),
        Intent(this, RecordingService::class.java).setAction(name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL,
            getString(R.string.recording_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.recording_channel_description)
            setShowBadge(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL = "mapme.recording"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.mapme.recording.START"
        const val ACTION_PAUSE = "com.mapme.recording.PAUSE"
        const val ACTION_RESUME = "com.mapme.recording.RESUME"
        const val ACTION_STOP = "com.mapme.recording.STOP"

        private fun send(context: Context, action: String) {
            val intent = Intent(context, RecordingService::class.java).setAction(action)
            runCatching {
                if (action == ACTION_START) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun start(context: Context) = send(context, ACTION_START)
        fun pause(context: Context) = send(context, ACTION_PAUSE)
        fun resume(context: Context) = send(context, ACTION_RESUME)
        fun stop(context: Context) = send(context, ACTION_STOP)
    }
}
