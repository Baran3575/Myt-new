package com.myt.player.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myt.player.AppState
import com.myt.player.data.model.Album
import com.myt.player.data.model.Track
import com.myt.player.ui.components.Artwork
import com.myt.player.ui.components.EmptyState
import com.myt.player.ui.components.GreenPlayButton
import com.myt.player.ui.components.SectionHeader
import com.myt.player.ui.components.TrackRow
import com.myt.player.ui.theme.MytGreen

@Composable
fun LibraryScreen(
    onPlay: (List<Track>, Int) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }

    val localTracks by AppState.localTracks.collectAsStateWithLifecycle()
    val albums by AppState.albums.collectAsStateWithLifecycle()
    val downloads by AppState.downloads.collectAsStateWithLifecycle()
    val favoriteIds by AppState.favorites.collectAsStateWithLifecycle()

    val favorites = remember(favoriteIds, localTracks, downloads) {
        (localTracks + downloads).filter { favoriteIds.contains(it.id) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) AppState.scanLibrary()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { AppState.scanLibrary() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Rescan device music")
            }
        }

        if (!AppState.hasMediaPermission()) {
            PermissionBanner { permissionLauncher.launch(mediaPermission()) }
        }

        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tracks") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Albums") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Favorites") })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Downloads") })
        }

        when (tab) {
            0 -> TrackList(
                tracks = localTracks,
                onPlay = onPlay,
                emptyMessage = if (AppState.hasMediaPermission())
                    "No music found on this device yet."
                else "Allow music permission to see your songs."
            )
            1 -> AlbumGrid(albums = albums, localTracks = localTracks, onPlay = onPlay)
            2 -> TrackList(
                tracks = favorites,
                onPlay = onPlay,
                emptyMessage = "No favorites yet — tap the ♥ on any track."
            )
            3 -> TrackList(
                tracks = downloads,
                onPlay = onPlay,
                emptyMessage = "Downloads appear here after you download online tracks.",
                showDelete = true
            )
        }
    }
}

private fun mediaPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
private fun PermissionBanner(onAllow: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(10.dp))
            .clickable(onClick = onAllow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MytGreen
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Music access needed", fontWeight = FontWeight.SemiBold)
            Text(
                "Tap to allow reading your on-device songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    onPlay: (List<Track>, Int) -> Unit,
    emptyMessage: String,
    showDelete: Boolean = false
) {
    if (tracks.isEmpty()) {
        EmptyState(emptyMessage)
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
    ) {
        items(tracks.size) { i ->
            val track = tracks[i]
            TrackRow(
                track = track,
                onClick = { onPlay(tracks, i) },
                trailing = {
                    if (showDelete) {
                        IconButton(onClick = { AppState.removeDownload(track) }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete download",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    localTracks: List<Track>,
    onPlay: (List<Track>, Int) -> Unit
) {
    if (!AppState.hasMediaPermission() || localTracks.isEmpty()) {
        EmptyState("Albums appear here once music access is granted.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, bottom = 120.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(albums) { index, album ->
            AlbumCard(
                album = album,
                onClick = {
                    val songs = localTracks.filter {
                        it.album == album.title && it.artist == album.artist
                    }
                    if (songs.isNotEmpty()) onPlay(songs, 0)
                }
            )
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            Artwork(
                uri = album.artworkUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            GreenPlayButton(
                onClick = onClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                size = 36
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.artist} • ${album.trackCount} " +
                if (album.trackCount == 1) "song" else "songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}