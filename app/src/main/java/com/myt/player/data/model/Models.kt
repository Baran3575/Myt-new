package com.myt.player.data.model

import kotlinx.serialization.Serializable

/** Where a track comes from. */
enum class TrackSource {
    LOCAL,   // scanned from the device MediaStore
    ONLINE,  // streamed/fetched from Jamendo
    DOWNLOAD // previously downloaded file on disk
}

/** A playable track, regardless of source. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String?,          // content:// for local albums, https:// for online
    val uri: String,                  // playable uri (content://, https://, file://)
    val source: TrackSource,
    val downloadUrl: String? = null,  // only for online tracks
    val isDownloaded: Boolean = false
)

/** Serializable form of a track, used to persist favorites / recents. */
@Serializable
data class TrackDto(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String?,
    val uri: String,
    val source: String,
    val downloadUrl: String? = null
)

fun Track.toDto() = TrackDto(
    id = id, title = title, artist = artist, album = album,
    durationMs = durationMs, artworkUri = artworkUri, uri = uri,
    source = source.name, downloadUrl = downloadUrl
)

fun TrackDto.toTrack() = Track(
    id = id, title = title, artist = artist, album = album,
    durationMs = durationMs, artworkUri = artworkUri, uri = uri,
    source = TrackSource.valueOf(source), downloadUrl = downloadUrl
)

/** An album grouping from the device library. */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val trackCount: Int
)