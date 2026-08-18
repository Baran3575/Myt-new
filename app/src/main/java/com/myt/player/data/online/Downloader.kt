package com.myt.player.data.online

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.app.Notification
import com.myt.player.data.local.LibraryStore
import com.myt.player.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads online (Jamendo) tracks to the app's Music folder, updates
 * the LibraryStore index and shows a progress notification.
 */
class Downloader(
    private val context: Context,
    private val store: LibraryStore
) {
    companion object {
        const val CHANNEL_ID = "downloads"
        const val NOTIFICATION_ID = 42
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    var currentProgress: Float? = null
        private set

    private var activeJob: Job? = null
    val isDownloading: Boolean get() = activeJob?.isActive == true

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        createChannel()
    }

    private fun createChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
        )
    }

    fun cancel() {
        activeJob?.cancel()
    }

    /** Returns true if a download was started. */
    fun start(track: Track): Boolean {
        if (activeJob?.isActive == true || track.source != com.myt.player.data.model.TrackSource.ONLINE) return false
        activeJob = scope.launch {
            runDownload(track)
        }
        return true
    }

    private suspend fun runDownload(track: Track) = withContext(Dispatchers.IO) {
        val dir = store.downloadsDir().apply { mkdirs() }
        val safeName = "${track.id.replace(':', '_')}.mp3"
        val target = File(dir, safeName)

        val request = Request.Builder().url(track.downloadUrl ?: track.uri).build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) { finishNotification(false); return@withContext }
            val body = resp.body ?: return@withContext
            val total = body.contentLength()
            var written = 0L
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) {
                            val fraction = written.toFloat() / total.toFloat()
                            currentProgress = fraction
                            publishNotification(track.title, fraction)
                        }
                    }
                }
            }
        }

        if (target.length() > 10_000) {
            val saved = track.copy(
                uri = "file://" + target.absolutePath,
                artworkUri = track.artworkUri,
                source = com.myt.player.data.model.TrackSource.DOWNLOAD,
                isDownloaded = true
            )
            store.saveDownload(saved)
            finishNotification(true)
        } else {
            target.delete()
            finishNotification(false)
        }
    }

    private fun publishNotification(title: String, fraction: Float) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            NOTIFICATION_ID,
            buildNotification(title, (fraction * 100).toInt(), true)
        )
    }

    private fun finishNotification(success: Boolean) {
        currentProgress = null
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(
            if (success) "Download complete" else "Download failed",
            -1, false
        ))
        // Dismiss after a moment
        scope.launch {
            kotlinx.coroutines.delay(2500)
            manager.cancel(NOTIFICATION_ID)
        }
    }

    private fun buildNotification(title: String, progress: Int, indeterminate: Boolean): Notification {
        val intent = android.content.Intent(context, com.myt.player.MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Myt — $title")
            .setContentIntent(pi)
            .setOngoing(true)
            .apply {
                if (progress >= 0) setProgress(100, progress, false)
            }
            .build()
    }
}