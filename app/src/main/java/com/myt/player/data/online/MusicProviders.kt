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
import java.util.concurrent.TimeUnit

/** A source of royalty-free music that can be streamed and downloaded. */
interface MusicProvider {
    val id: String
    val name: String
    val isConfigured: Boolean
    suspend fun search(query: String, limit: Int): List<Track>
    suspend fun featured(limit: Int): List<Track>
}

/** Jamendo - thousands of CC-licensed tracks from amateur musicians. */
object JamendoProvider : MusicProvider {
    override val id = "jamendo"
    override val name = "Jamendo"
    override val isConfigured: Boolean get() = JamendoClient.isConfigured
    override suspend fun search(query: String, limit: Int): List<Track> =
        JamendoClient.search(query, limit)

    override suspend fun featured(limit: Int): List<Track> =
        JamendoClient.featured(limit)
}

/**
 * Pixabay Music - professional stock music, royalty-free (Pixabay License).
 * Uses the api_music endpoint. Free key: https://pixabay.com/api/docs/
 * The endpoint is lightly documented, so parsing is defensive:
 * any unexpected shape simply yields an empty result (Jamendo stays as fallback).
 */
object PixabayMusicProvider : MusicProvider {

    override val id = "pixabay"
    override val name = "Pixabay Music"
    override val isConfigured: Boolean get() = BuildConfig.PIXABAY_API_KEY.isNotBlank()

    private const val BASE = "https://pixabay.com/api_music/"
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Serializable
    data class PixabayResponse(val hits: List<PixabayHit> = emptyList())

    @Serializable
    data class PixabayHit(
        val id: Long = 0,
        val title: String? = null,
        val tags: String? = null,
        val duration: Int? = null,
        val audio: String? = null,          // short preview (mp3)
        val audio_download: String? = null, // full track download (mp3)
        val images: List<String> = emptyList(),
        val user: String? = null
    )

    override suspend fun search(query: String, limit: Int): List<Track> =
        fetch(query = query, order = null, limit = limit)

    override suspend fun featured(limit: Int): List<Track> =
        fetch(query = null, order = "popular", limit = limit)

    private suspend fun fetch(query: String?, order: String?, limit: Int): List<Track> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext emptyList()
            val url = StringBuilder(BASE)
                .append("?key=").append(BuildConfig.PIXABAY_API_KEY)
                .append("&per_page=").append(limit.coerceIn(1, 200))
                .append("&lang=en")
            if (!query.isNullOrBlank()) {
                url.append("&q=").append(query.trim().replace(' ', '+'))
            }
            if (!order.isNullOrBlank()) url.append("&order=").append(order)

            val request = Request.Builder()
                .url(url.toString())
                .header("User-Agent", "MytPlayer/1.0 (personal music app)")
                .build()

            val body = try {
                http.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
            } catch (e: Exception) {
                null
            } ?: return@withContext emptyList()

            runCatching {
                val decoded = json.decodeFromString(PixabayResponse.serializer(), body)
                decoded.hits.mapNotNull { hit ->
                    val stream = hit.audio_download ?: hit.audio ?: return@mapNotNull null
                    val title = hit.title?.takeIf { it.isNotBlank() }
                        ?: hit.tags?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    Track(
                        id = "pixabay:${hit.id}",
                        title = title.trim(),
                        artist = hit.user ?: "Pixabay",
                        album = "Pixabay Music",
                        durationMs = (hit.duration ?: 0) * 1000L,
                        artworkUri = hit.images.firstOrNull(),
                        uri = stream,
                        source = TrackSource.ONLINE,
                        downloadUrl = hit.audio_download ?: stream
                    )
                }
            }.getOrDefault(emptyList())
        }
}

/** All providers that are currently configured, Jamendo first. */
fun configuredProviders(): List<MusicProvider> =
    listOf(JamendoProvider, PixabayMusicProvider).filter { it.isConfigured }