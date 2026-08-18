package com.myt.player.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.myt.player.data.model.Track
import com.myt.player.data.model.TrackSource
import com.myt.player.data.online.JamendoClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/** Snapshot of what the player is doing right now. */
data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleOn: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

/**
 * Talks to [PlaybackService] through a MediaController and exposes
 * reactive playback state to the UI. All player calls happen on the
 * main thread (required by Media3).
 */
class PlayerRepository(private val context: Context) {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var controllerConnected = false
    private var ticker: Job? = null

    // Remembers the Track behind each enqueued mediaId.
    private val mediaIdToTrack = HashMap<String, Track>()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            controller = MediaController.Builder(context, token).buildAsync().also { future ->
                future.addListener({
                    val c = future.get()
                    // Attach on the main thread so all controller calls stay consistent.
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        controller = c
                        controllerConnected = true
                        attachListener()
                        syncStateFromController()
                    }
                }, MoreExecutors.directExecutor())
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            controllerConnected = false
        }
    }

    init {
        context.bindService(
            Intent(context, PlaybackService::class.java),
            connection, Context.BIND_AUTO_CREATE
        )
    }

    private fun attachListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
                manageTicker(isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateState()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                updateState()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateState()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                updateState()
            }
        })
    }

    private fun updateState() {
        val c = controller ?: return
        val item = c.currentMediaItem
        val track = item?.mediaId?.let { mediaIdToTrack[it] }
        _state.value = _state.value.copy(
            currentTrack = track,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.takeIf { it > 0 } ?: track?.durationMs ?: 0L,
            shuffleOn = c.shuffleModeEnabled,
            repeatMode = c.repeatMode
        )
    }

    private fun syncStateFromController() {
        val c = controller ?: return
        c.mediaMetadata // touch to load metadata
        updateState()
        manageTicker(c.isPlaying)
    }

    private fun manageTicker(playing: Boolean) {
        if (playing) {
            if (ticker == null) {
                ticker = scope.launch {
                    while (true) {
                        delay(500)
                        updateState()
                    }
                }
            }
        } else {
            ticker?.cancel()
            ticker = null
        }
    }

    // ---------------- Public API ----------------

    /** Loads a queue of tracks and starts playing from [startIndex]. */
    fun playQueue(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: return
        if (tracks.isEmpty()) return

        val items = tracks.map { track ->
            mediaIdToTrack[track.id] = track
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(Uri.parse(track.uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkData(loadArtworkBytes(track), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()
                )
                .build()
        }
        val start = startIndex.coerceIn(0, items.lastIndex)
        c.setMediaItems(items, start, 0L)
        c.prepare()
        c.play()
    }

    fun playInstant(single: Track) = playQueue(listOf(single), 0)

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.playbackState == Player.STATE_IDLE || c.playlist.isEmpty()) return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNext()
    fun previous() = controller?.seekToPrevious()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0))
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /**
     * Fetches artwork bytes for the media notification:
     * content:// for local albums, https for Jamendo images.
     * Runs on IO; safe to call per-enqueue.
     */
    private fun loadArtworkBytes(track: Track): ByteArray? {
        return try {
            when {
                track.artworkUri == null -> null
                track.artworkUri.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(track.artworkUri))?.use { input ->
                        ByteArrayOutputStream().use { out -> input.copyTo(out); out.toByteArray() }
                    }
                }
                track.artworkUri.startsWith("https://") -> {
                    kotlinx.coroutines.runBlocking { JamendoClient.fetchBytes(track.artworkUri) }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}