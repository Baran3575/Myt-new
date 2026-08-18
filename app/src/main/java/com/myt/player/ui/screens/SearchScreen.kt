package com.myt.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myt.player.AppState
import com.myt.player.data.model.Track
import com.myt.player.data.model.TrackSource
import com.myt.player.data.online.JamendoClient
import com.myt.player.ui.components.EmptyState
import com.myt.player.ui.components.TrackRow
import com.myt.player.ui.theme.MytGreen
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    onPlay: (List<Track>, Int) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }

    val localTracks by AppState.localTracks.collectAsStateWithLifecycle()
    val onlineResults by AppState.searchResults.collectAsStateWithLifecycle()
    val downloadingIds by AppState.downloadingIds.collectAsStateWithLifecycle()

    val localMatches = remember(localTracks, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else localTracks.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }

    LaunchedEffect(query) {
        if (tab == 1 && query.isNotBlank()) {
            delay(350)
            AppState.searchOnline(query)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Search field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("What do you want to listen to?") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (tab == 1) AppState.searchOnline(query) }),
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Device / Online tabs
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("On device") }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("Online") }
            )
        }

        Spacer(Modifier.size(0.dp))

        when (tab) {
            0 -> {
                if (!AppState.hasMediaPermission() || localTracks.isEmpty()) {
                    EmptyState(
                        if (AppState.hasMediaPermission())
                            "Nothing found. Allow music access in Library if songs are missing."
                        else "Music permission needed — grant it in the Library tab."
                    )
                } else if (query.isBlank()) {
                    EmptyState("Type to search your device library.")
                } else if (localMatches.isEmpty()) {
                    EmptyState("No local matches for \"$query\"")
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(localMatches.size) { i ->
                            val track = localMatches[i]
                            TrackRow(
                                track = track,
                                onClick = { onPlay(localMatches, i) },
                                trailing = { FavoriteButton(track) }
                            )
                        }
                    }
                }
            }

            1 -> {
                if (!JamendoClient.isConfigured) {
                    EmptyState(
                        "Online search needs a free Jamendo API key.\n" +
                            "Get one at devs.jamendo.com and rebuild with MYT_JAMENDO_CLIENT_ID."
                    )
                } else if (query.isBlank()) {
                    EmptyState("Type to search royalty-free music online.")
                } else if (onlineResults.isEmpty()) {
                    EmptyState("Searching… or no results.")
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(onlineResults.size) { i ->
                            val track = onlineResults[i]
                            TrackRow(
                                track = track,
                                onClick = { onPlay(onlineResults, i) },
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FavoriteButton(track)
                                        DownloadButton(
                                            track = track,
                                            downloading = downloadingIds.contains(track.id)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteButton(track: Track) {
    val favIds by AppState.favorites.collectAsStateWithLifecycle()
    val favorite = favIds.contains(track.id)
    IconButton(onClick = { AppState.toggleFavorite(track) }) {
        Icon(
            imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (favorite) "Remove favorite" else "Add favorite",
            tint = if (favorite) MytGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DownloadButton(track: Track, downloading: Boolean) {
    val canDownload = track.source == TrackSource.ONLINE && !downloading
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(enabled = canDownload) { AppState.startDownload(track) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (downloading) Icons.Rounded.Check else Icons.Rounded.Download,
            contentDescription = if (downloading) "Downloading" else "Download",
            tint = if (downloading) MytGreen else if (canDownload) MytGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}