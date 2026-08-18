package com.myt.player.data.online

import com.myt.player.BuildConfig
import com.myt.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** A video clip (mp4) + preview thumbnail, matched to a playing track. */
data class VideoClip(val url: String, val thumbnail: String?)

/**
 * Fetches short, CC-licensed stock video clips from the Pixabay Video API
 * (same free key as the music API). Used as a muted, looping visual
 * background in the now-playing screen while the music plays.
 * Defensive parsing: any unexpected response yields null, never a crash.
 */
object PixabayVideoClient {

    private const val BASE = "https://pixabay.com/api/videos/"
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean get() = BuildConfig.PIXABAY_API_KEY.isNotBlank()

    @Serializable
    data class VideoResponse(val hits: List<VideoHit> = emptyList())

    @Serializable
    data class VideoHit(
        val id: Long = 0,
        val picture: String? = null,
        val tags: String? = null,
        val videos: Map<String, VideoFile> = emptyMap()
    )

    @Serializable
    data class VideoFile(val url: String? = null)

    /**
     * Picks the best clip for a track: keyword from the title, fallback to
     * generic "music" visuals. Returns a preview URL or null.
     */
    suspend fun fetchForTrack(track: Track): VideoClip? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val words = track.title.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 3 }
            .distinct()
            .take(3)
        val query = (words + "music").take(3).joinToString(" ")
        fetch(query)?.firstOrNull() ?: fetch("music abstract")?.firstOrNull()
    }

    private suspend fun fetch(query: String): List<VideoClip> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()
        val url = "$BASE?key=${BuildConfig.PIXABAY_API_KEY}" +
            "&q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
            "&per_page=3&video_type=film&min_width=480"
        val request = Request.Builder().url(url).build()
        val body = try {
            http.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            null
        } ?: return@withContext emptyList()

        runCatching {
            json.decodeFromString(VideoResponse.serializer(), body).hits.mapNotNull { hit ->
                // Prefer larger mp4-format files.
                val file = hit.videos["large"] ?: hit.videos["medium"] ?: hit.videos["small"]
                val fileUrl = file?.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                VideoClip(url = fileUrl, thumbnail = hit.picture)
            }
        }.getOrDefault(emptyList())
    }
}