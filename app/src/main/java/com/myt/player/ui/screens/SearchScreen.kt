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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.myt.player.ui.components.CategoryCard
import com.myt.player.ui.components.EmptyState
import com.myt.player.ui.components.SectionHeader
import com.myt.player.ui.components.TrackRow
import com.myt.player.ui.theme.MytGreen
import kotlinx.coroutines.delay

/** Spotlight-style category tiles: tapping one starts an online search. */
private data class Category(val label: String, val colors: List<Color>)

private val categories = listOf(
    Category("Chill", listOf(Color(0xFF27856A), Color(0xFF0E3E33))),
    Category("Focus", listOf(Color(0xFF8E66AC), Color(0xFF543680))),
    Category("Workout", listOf(Color(0xFFE13300), Color(0xFF7A1B00))),
    Category("Party", listOf(Color(0xFFD84000), Color(0xFF6E2600))),
    Category("Sleep", listOf(Color(0xFF2E5771), Color(0xFF172F3D))),
    Category("Travel", listOf(Color(0xFF0D73EC), Color(0xFF053C7A))),
    Category("Gaming", listOf(Color(0xFFE91429), Color(0xFF6E0913))),
    Category("Lofi", listOf(Color(0xFF8D67AB), Color(0xFF3E2A4E))),
    Category("Rock", listOf(Color(0xFF283891), Color(0xFF120F2E))),
    Category("Pop", listOf(Color(0xFF1E3264), Color(0xFF0D1B33))),
    Category("Jazz", listOf(Color(0xFFBA5D07), Color(0xFF4A2603))),
    Category("Hip Hop", listOf(Color(0xFF503750), Color(0xFF251425)))
)

@Composable
fun SearchScreen(
    onPlay: (List<Track>, Int) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) } // 0 = device, 1 = online

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

    LaunchedEffect(query, tab) {
        if (tab == 1 && query.isNotBlank()) {
            delay(350)
            AppState.searchOnline(query)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
            keyboardActions = KeyboardActions(onSearch = {
                if (tab == 1) AppState.searchOnline(query)
            }),
            shape = RoundedCornerShape(24.dp)
        )

        // Scope chips
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = tab == 0,
                onClick = { tab = 0 },
                label = { Text("On device") }
            )
            FilterChip(
                selected = tab == 1,
                onClick = { tab = 1 },
                label = { Text("Online") }
            )
        }

        Spacer(Modifier.size(6.dp))

        when {
            query.isBlank() && tab == 0 -> {
                SectionHeader("Browse all")
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp)
                ) {
                    items(categories.size) { index ->
                        val inner = categories[index]
                        val next = if (index + 1 < categories.size) categories[index + 1] else null
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            CategoryCard(
                                label = inner.label,
                                colors = inner.colors,
                                onClick = {
                                    tab = 1
                                    query = inner.label
                                    AppState.searchOnline(inner.label)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (next != null) {
                                Spacer(Modifier.width(12.dp))
                                CategoryCard(
                                    label = next.label,
                                    colors = next.colors,
                                    onClick = {
                                        tab = 1
                                        query = next.label
                                        AppState.searchOnline(next.label)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            tab == 0 -> {
                when {
                    localMatches.isEmpty() && query.isNotBlank() ->
                        EmptyState("No local matches for \"$query\"")
                    !AppState.hasMediaPermission() || localTracks.isEmpty() ->
                        EmptyState("Nothing found. Allow music access in the Library tab.")
                    else -> LazyColumn(Modifier.fillMaxSize()) {
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

            else -> {
                when {
                    !AppState.hasOnlineMusic ->
                        EmptyState(
                            "Online music needs free API keys.\n" +
                                "Add JAMENDO_CLIENT_ID (devs.jamendo.com) and/or\n" +
                                "PIXABAY_API_KEY (pixabay.com/api/docs) as GitHub secrets."
                        )
                    query.isBlank() -> EmptyState("Type to search royalty-free music online.")
                    onlineResults.isEmpty() -> EmptyState("Searching… or no results.")
                    else -> LazyColumn(Modifier.fillMaxSize()) {
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