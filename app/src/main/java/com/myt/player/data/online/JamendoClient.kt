package com.myt.player.data.online

import com.myt.player.BuildConfig
import com.myt.player.data.model.Track
import com.myt.player.data.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client for the Jamendo API - thousands of Creative-Commons / royalty-free tracks.
 * Get a free client_id at https://devs.jamendo.com and set it via
 * MYT_JAMENDO_CLIENT_ID (gradle property or GitHub Actions secret).
 */
object JamendoClient {

    private const val BASE = "https://api.jamendo.com/v3.0"
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Serializable
    data class JamendoResponse(val results: List<JamendoTrack> = emptyList())

    @Serializable
    data class JamendoTrack(
        val id: String,
        val name: String,
        val artist_name: String,
        val album_name: String? = null,
        val duration: Int = 0,
        val audio: String? = null,
        val audio_download: String? = null,
        val image: String? = null,
        val album_image: String? = null
    )

    val isConfigured: Boolean get() = BuildConfig.JAMENDO_CLIENT_ID.isNotBlank()

    private fun getJson(path: String, params: Map<String, String>): String? {
        if (!isConfigured) return null
        val url = StringBuilder(BASE).append(path).append("?client_id=")
            .append(BuildConfig.JAMENDO_CLIENT_ID)
            .append("&format=json")
        params.forEach { (k, v) -> url.append("&").append(k).append("=").append(v) }
        val request = Request.Builder().url(url.toString()).build()
        return try {
            http.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: IOException) {
            null
        }
    }

    /** Search tracks by query text. */
    suspend fun search(query: String, limit: Int = 30): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val body = getJson("/tracks/", mapOf(
            "search" to query,
            "limit" to limit.toString(),
            "include" to "musicinfo"
        )) ?: return@withContext emptyList()
        parseTracks(body)
    }

    /** Popular / featured tracks for the home screen. */
    suspend fun featured(limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        val body = getJson("/tracks/", mapOf(
            "limit" to limit.toString(),
            "order" to "popular_month",
            "include" to "musicinfo"
        )) ?: return@withContext emptyList()
        parseTracks(body)
    }

    private fun parseTracks(body: String): List<Track> {
        return runCatching {
            json.decodeFromString(JamendoResponse.serializer(), body).results.mapNotNull { t ->
                val audio = t.audio ?: return@mapNotNull null
                Track(
                    id = "jamendo:${t.id}",
                    title = t.name,
                    artist = t.artist_name,
                    album = t.album_name ?: "Unknown album",
                    durationMs = t.duration * 1000L,
                    artworkUri = t.album_image ?: t.image,
                    uri = audio,
                    source = TrackSource.ONLINE,
                    downloadUrl = t.audio_download ?: audio
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Downloads the bytes of a remote file (used for artwork in the media notification). */
    suspend fun fetchBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            http.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.bytes() else null
            }
        }.getOrNull()
    }

    /** Streams a remote file to disk (used by the downloader). */
    suspend fun downloadToFile(url: String, target: java.io.File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).build()
                http.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching false
                    val body = resp.body ?: return@runCatching false
                    target.outputStream().use { out -> body.byteStream().copyTo(out) }
                    target.length() > 10_000
                }
            }.getOrDefault(false)
        }
}