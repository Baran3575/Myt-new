package com.myt.player.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.myt.player.data.model.Album
import com.myt.player.data.model.Track
import com.myt.player.data.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the on-device music library from MediaStore.
 */
class LocalMusicRepository(private val context: Context) {

    fun hasPermission(): Boolean {
        val hasRead = if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return hasRead
    }

    /** Scans MediaStore for all audio tracks. Returns empty list if permission missing. */
    suspend fun scanTracks(): List<Track> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown"
                val artist = cursor.getString(artistCol) ?: "Unknown artist"
                val album = cursor.getString(albumCol) ?: "Unknown album"
                val albumId = cursor.getLong(albumIdCol)
                val durationMs = cursor.getLong(durCol)
                val dataPath = cursor.getString(dataCol)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val artworkUri = if (albumId > 0) {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId
                    ).toString()
                } else null

                // Skip tiny/empty files and system sounds
                if (dataPath.isNullOrBlank()) continue
                val size = java.io.File(dataPath).length()
                if (size < 50_000) continue

                tracks += Track(
                    id = "local:$id:$albumId",
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = durationMs,
                    artworkUri = artworkUri,
                    uri = contentUri.toString(),
                    source = TrackSource.LOCAL
                )
            }
        }
        tracks
    }

    /** Scans MediaStore and groups tracks into albums. */
    suspend fun scanAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val tracks = scanTracks()
        tracks.groupBy { it.album + "|" + it.artist }
            .map { (key, group) ->
                val first = group.first()
                Album(
                    id = key.hashCode().toLong(),
                    title = first.album,
                    artist = first.artist,
                    artworkUri = first.artworkUri,
                    trackCount = group.size
                )
            }
            .sortedBy { it.title.lowercase() }
    }
}