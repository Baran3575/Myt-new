package com.myt.player.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.myt.player.data.online.VideoClip

/**
 * Muted, looping mp4 clip used as the visual background of the
 * now-playing screen. Pauses along with the music.
 */
@Composable
fun VideoBackground(clip: VideoClip?, isPlaying: Boolean) {
    val context = LocalContext.current
    val clipPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    DisposableEffect(Unit) {
        onDispose { clipPlayer.release() }
    }

    // Track the url we are currently showing to avoid re-preparing constantly.
    var loadedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(clip?.url, isPlaying) {
        val url = clip?.url
        when {
            url == null -> {
                loadedUrl = null
                clipPlayer.pause()
                clipPlayer.clearMediaItems()
            }
            url != loadedUrl -> {
                loadedUrl = url
                clipPlayer.setMediaItem(MediaItem.fromUri(url))
                clipPlayer.prepare()
                if (isPlaying) clipPlayer.play()
            }
            else -> {
                if (isPlaying) clipPlayer.play() else clipPlayer.pause()
            }
        }
    }

    if (clip != null) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = clipPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}