package com.myt.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myt.player.AppState
import com.myt.player.data.model.Track
import com.myt.player.ui.components.GreenPlayButton
import com.myt.player.ui.components.MixCard
import com.myt.player.ui.components.SectionHeader
import com.myt.player.ui.components.SquareCard
import com.myt.player.ui.components.TwoColumnGrid
import com.myt.player.ui.theme.MytGreen
import java.time.LocalTime

@Composable
fun HomeScreen(
    onPlay: (List<Track>, Int) -> Unit
) {
    val recents by AppState.recents.collectAsStateWithLifecycle()
    val localTracks by AppState.localTracks.collectAsStateWithLifecycle()
    val downloads by AppState.downloads.collectAsStateWithLifecycle()
    val featured by AppState.featured.collectAsStateWithLifecycle()
    val favoriteIds by AppState.favorites.collectAsStateWithLifecycle()

    val favorites = remember(favoriteIds, localTracks, downloads, featured) {
        (localTracks + downloads + featured).filter { favoriteIds.contains(it.id) }
    }
    val recentsForGrid = recents.take(6)
    val favoritesForGrid = favorites.take(6)
    val downloadsForGrid = downloads.take(6)
    val onlineForGrid = featured.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = greeting(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Myt",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        item {
            HeroMixCard(mixSource = favorites.ifEmpty { recents }, onPlay = onPlay)
            Spacer(Modifier.height(10.dp))
        }

        if (recentsForGrid.isNotEmpty()) {
            item { SectionHeader("Recently played") }
            item {
                TwoColumnGrid(recentsForGrid) { track, modifier ->
                    MixCard(
                        title = track.title,
                        subtitle = track.artist,
                        artworkUri = track.artworkUri,
                        onClick = {
                            val list = recents
                            onPlay(list, list.indexOf(track).takeIf { it >= 0 } ?: 0)
                        },
                        modifier = modifier
                    )
                }
            }
        }

        if (favoritesForGrid.isNotEmpty()) {
            item { SectionHeader("Your favorites") }
            item {
                TwoColumnGrid(favoritesForGrid) { track, modifier ->
                    MixCard(
                        title = track.title,
                        subtitle = track.artist,
                        artworkUri = track.artworkUri,
                        onClick = {
                            val list = favorites
                            onPlay(list, list.indexOf(track).takeIf { it >= 0 } ?: 0)
                        },
                        modifier = modifier
                    )
                }
            }
        }

        if (downloadsForGrid.isNotEmpty()) {
            item { SectionHeader("Downloads") }
            item {
                TwoColumnGrid(downloadsForGrid) { track, modifier ->
                    MixCard(
                        title = track.title,
                        subtitle = track.artist,
                        artworkUri = track.artworkUri,
                        onClick = {
                            val list = downloads
                            onPlay(list, list.indexOf(track).takeIf { it >= 0 } ?: 0)
                        },
                        modifier = modifier
                    )
                }
            }
        }

        item { SectionHeader("Featured online") }
        item {
            if (AppState.hasOnlineMusic && onlineForGrid.isNotEmpty()) {
                TwoColumnGrid(onlineForGrid) { track, modifier ->
                    SquareCard(
                        track = track,
                        onClick = {
                            val list = onlineForGrid
                            onPlay(list, list.indexOf(track).takeIf { it >= 0 } ?: 0)
                        },
                        modifier = modifier
                    )
                }
            } else if (!AppState.hasOnlineMusic) {
                OnlineSetupHint()
            } else {
                Text(
                    text = "Could not load featured tracks right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

private fun greeting(): String {
    val hour = runCatching { LocalTime.now().hour }.getOrDefault(12)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun HeroMixCard(
    mixSource: List<Track>,
    onPlay: (List<Track>, Int) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(150.dp)
            .background(
                Brush.linearGradient(
                    listOf(MytGreen, Color(0xFF0A3D20))
                ),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color(0xFF04180C).copy(alpha = 0.65f),
                modifier = Modifier.height(22.dp)
            )
            Text(
                text = "Your Mix",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF04180C)
            )
            Text(
                text = if (mixSource.isNotEmpty())
                    "${mixSource.size} songs • favorites + recents"
                else "Tap to explore",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF04180C).copy(alpha = 0.8f)
            )
        }
        GreenPlayButton(
            onClick = { if (mixSource.isNotEmpty()) onPlay(mixSource, 0) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 15.dp),
            size = 56
        )
    }
}

@Composable
private fun OnlineSetupHint() {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MytGreen
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "Online music is inactive",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Free keys are needed (add both to get the best catalog):\n" +
                    "• Jamendo: devs.jamendo.com → JAMENDO_CLIENT_ID secret\n" +
                    "• Pixabay Music: pixabay.com/api/docs → PIXABAY_API_KEY secret\n" +
                    "Rebuild via Actions after adding. Local music already works.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}