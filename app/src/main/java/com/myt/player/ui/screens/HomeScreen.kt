package com.myt.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.myt.player.data.online.JamendoClient
import com.myt.player.ui.components.CardCarousel
import com.myt.player.ui.components.SectionHeader
import com.myt.player.ui.theme.MytGreen
import java.time.LocalTime

@Composable
fun HomeScreen(
    onPlay: (List<com.myt.player.data.model.Track>, Int) -> Unit
) {
    val recents by AppState.recents.collectAsStateWithLifecycle()
    val localTracks by AppState.localTracks.collectAsStateWithLifecycle()
    val downloads by AppState.downloads.collectAsStateWithLifecycle()
    val featured by AppState.featured.collectAsStateWithLifecycle()
    val favoriteIds by AppState.favorites.collectAsStateWithLifecycle()

    val favorites = remember(favoriteIds, localTracks, downloads, featured) {
        (localTracks + downloads + featured).filter { favoriteIds.contains(it.id) }
    }
    val mixSource = remember(favorites, recents) {
        favorites.ifEmpty { recents }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
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
            HeroCard(mixSource = mixSource, onPlay = onPlay)
            Spacer(Modifier.height(8.dp))
        }

        if (recents.isNotEmpty()) {
            item { SectionHeader("Recently played") }
            item {
                CardCarousel(
                    tracks = recents,
                    onPlay = onPlay,
                    emptyText = ""
                )
            }
        }

        if (favorites.isNotEmpty()) {
            item { SectionHeader("Your favorites") }
            item {
                CardCarousel(
                    tracks = favorites,
                    onPlay = onPlay,
                    emptyText = ""
                )
            }
        }

        if (downloads.isNotEmpty()) {
            item { SectionHeader("Downloads") }
            item {
                CardCarousel(
                    tracks = downloads,
                    onPlay = onPlay,
                    emptyText = ""
                )
            }
        }

        item { SectionHeader("Featured online") }
        item {
            if (JamendoClient.isConfigured) {
                CardCarousel(
                    tracks = featured,
                    onPlay = onPlay,
                    emptyText = "Could not load featured tracks right now."
                )
            } else {
                JamendoHintCard()
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
private fun HeroCard(
    mixSource: List<com.myt.player.data.model.Track>,
    onPlay: (List<com.myt.player.data.model.Track>, Int) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.linearGradient(listOf(MytGreen, Color(0xFF0A3D20))),
                RoundedCornerShape(12.dp)
            )
            .clickable {
                if (mixSource.isNotEmpty()) onPlay(mixSource, 0)
            },
        contentAlignment = Alignment.BottomStart
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.height(24.dp)
            )
            Text(
                text = "Your Mix",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF04180C)
            )
            Text(
                text = if (mixSource.isNotEmpty()) "${mixSource.size} songs • tap to play" else "No favorites yet",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF04180C).copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun JamendoHintCard() {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MytGreen
        )
        Column {
            Text(
                text = "Online music is inactive",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Get a free Jamendo API key at devs.jamendo.com and set it as the " +
                    "MYT_JAMENDO_CLIENT_ID Gradle property (or GitHub secret). Local music works already.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}