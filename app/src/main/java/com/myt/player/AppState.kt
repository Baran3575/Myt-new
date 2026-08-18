package com.myt.player

import android.content.Context
import com.myt.player.data.local.LibraryStore
import com.myt.player.data.local.LocalMusicRepository
import com.myt.player.data.model.Album
import com.myt.player.data.model.Track
import com.myt.player.data.online.Downloader
import com.myt.player.data.online.JamendoClient
import com.myt.player.playback.PlayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single shared state holder for the whole app (no DI framework needed
 * for a personal player). Initialized once in [MytApp].
 */
object AppState {

    lateinit var context: Context
        private set

    val localRepo: LocalMusicRepository by lazy { LocalMusicRepository(context) }
    val store: LibraryStore by lazy { LibraryStore(context) }
    val downloader: Downloader by lazy { Downloader(context, store) }
    val player: PlayerRepository by lazy { PlayerRepository(context) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _recents = MutableStateFlow<List<Track>>(emptyList())
    val recents: StateFlow<List<Track>> = _recents.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _downloads = MutableStateFlow<List<Track>>(emptyList())
    val downloads: StateFlow<List<Track>> = _downloads.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()

    private val _featured = MutableStateFlow<List<Track>>(emptyList())
    val featured: StateFlow<List<Track>> = _featured.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    fun init(appContext: Context) {
        if (::context.isInitialized) return
        context = appContext.applicationContext
        _favorites.value = store.favorites()
        _recents.value = store.recents()
        _downloads.value = store.downloads()
        scanLibrary()
        loadFeatured()
    }

    fun hasMediaPermission(): Boolean = localRepo.hasPermission()

    /** Re-scans the device MediaStore. Does nothing without permission. */
    fun scanLibrary() {
        scope.launch {
            if (!localRepo.hasPermission()) return@launch
            _localTracks.value = localRepo.scanTracks()
            _albums.value = localRepo.scanAlbums()
        }
    }

    fun loadFeatured() {
        if (!JamendoClient.isConfigured) return
        scope.launch {
            _featured.value = JamendoClient.featured(limit = 24)
        }
    }

    fun isFavorite(trackId: String): Boolean = _favorites.value.contains(trackId)

    fun toggleFavorite(track: Track) {
        val newValue = !isFavorite(track.id)
        val updated = _favorites.value.toMutableSet()
        if (newValue) updated.add(track.id) else updated.remove(track.id)
        _favorites.value = updated
        scope.launch { store.setFavorite(track, newValue) }
    }

    fun favoriteTracks(): List<Track> {
        val fav = _favorites.value
        val byId = HashMap<String, Track>()
        _localTracks.value.forEach { byId[it.id] = it }
        _downloads.value.forEach { byId[it.id] = it }
        if (JamendoClient.isConfigured) {
            _searchResults.value.forEach { byId[it.id] = it }
        }
        return fav.mapNotNull { byId[it] }
    }

    fun onTrackPlayed(track: Track) {
        _recents.value = listOf(track) + _recents.value.filterNot { it.id == track.id }.take(49)
        scope.launch { store.addRecent(track) }
    }

    fun startDownload(track: Track) {
        if (downloader.start(track)) {
            _downloadingIds.value = _downloadingIds.value + track.id
            scope.launch {
                // Watch until the active job is done, then refresh the downloads list.
                while (downloader.isDownloading) {
                    kotlinx.coroutines.delay(500)
                }
                _downloadingIds.value = _downloadingIds.value - track.id
                _downloads.value = store.downloads()
            }
        }
    }

    fun removeDownload(track: Track) {
        scope.launch {
            store.removeDownload(track)
            _downloads.value = store.downloads()
        }
    }

    fun searchOnline(query: String) {
        if (!JamendoClient.isConfigured || query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        scope.launch {
            _searchResults.value = JamendoClient.search(query.trim(), limit = 40)
        }
    }
}