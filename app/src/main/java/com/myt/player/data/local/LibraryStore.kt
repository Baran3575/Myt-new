package com.myt.player.data.local

import android.content.Context
import com.myt.player.data.model.Track
import com.myt.player.data.model.TrackDto
import com.myt.player.data.model.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Tiny JSON-file based store for favorites, recently played and the download index.
 * No database needed for a personal player.
 */
class LibraryStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dir: File get() = context.filesDir

    private fun file(name: String) = File(dir, name)

    // ---------- Favorites ----------
    fun favorites(): Set<String> = readStrings("favorites.json").toSet()

    suspend fun setFavorite(track: Track, favorite: Boolean) = withContext(Dispatchers.IO) {
        val current = favorites().toMutableSet()
        if (favorite) current.add(track.id) else current.remove(track.id)
        writeStrings("favorites.json", current.toList())
    }

    // ---------- Recently played ----------
    fun recents(): List<Track> = readTracks("recent.json")

    suspend fun addRecent(track: Track) = withContext(Dispatchers.IO) {
        val list = recents().toMutableList().filterNot { it.id == track.id }
        list.add(0, track)
        writeTracks("recent.json", list.take(50))
    }

    // ---------- Downloads index ----------
    fun downloadsDir(): File = File(context.getExternalFilesDir(null) ?: dir, "Music")

    fun downloads(): List<Track> = readTracks("downloads.json")

    suspend fun saveDownload(track: Track) = withContext(Dispatchers.IO) {
        val list = downloads().toMutableList().filterNot { it.id == track.id }
        list.add(0, track.copy(isDownloaded = true))
        writeTracks("downloads.json", list)
    }

    suspend fun removeDownload(track: Track) = withContext(Dispatchers.IO) {
        val list = downloads().filterNot { it.id == track.id }
        writeTracks("downloads.json", list)
        runCatching { File(track.uri.removePrefix("file://")).delete() }
    }

    // ---------- IO helpers ----------
    private fun readStrings(name: String): List<String> {
        val f = file(name)
        if (!f.exists()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(String.serializer()), f.readText()) }
            .getOrDefault(emptyList())
    }

    private fun writeStrings(name: String, list: List<String>) {
        file(name).writeText(json.encodeToString(ListSerializer(String.serializer()), list))
    }

    private fun readTracks(name: String): List<Track> {
        val f = file(name)
        if (!f.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(TrackDto.serializer()), f.readText()).map { it.toTrack() }
        }.getOrDefault(emptyList())
    }

    private fun writeTracks(name: String, list: List<Track>) {
        file(name).writeText(
            json.encodeToString(ListSerializer(TrackDto.serializer()), list.map { it.toDto() })
        )
    }
}