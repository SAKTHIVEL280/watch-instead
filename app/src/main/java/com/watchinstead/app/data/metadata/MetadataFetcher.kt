package com.lockin.app.data.metadata

import com.lockin.app.domain.Platform
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

interface MetadataFetcher {
    /** Best effort: returns null when the site gives us nothing usable. */
    suspend fun fetch(url: String, platform: Platform): LinkMetadata?
}

@Serializable
private data class OEmbedResponse(
    val title: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
)

@Singleton
class HttpMetadataFetcher @Inject constructor(private val client: OkHttpClient) : MetadataFetcher {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetch(url: String, platform: Platform): LinkMetadata? =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = when (platform) {
                    Platform.YOUTUBE -> "https://www.youtube.com/oembed"
                    Platform.TIKTOK -> "https://www.tiktok.com/oembed"
                    else -> null
                }
                if (endpoint != null) {
                    val encoded = URLEncoder.encode(url, "UTF-8")
                    val body = read("$endpoint?format=json&url=$encoded")
                    json.decodeFromString<OEmbedResponse>(body).let { LinkMetadata(it.title, it.thumbnailUrl) }
                } else {
                    OpenGraphParser.parse(read(url))
                }
            }.getOrNull()
        }

    private fun read(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", "Lockin").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            return response.peekBody(MAX_BODY_BYTES).string()
        }
    }

    private companion object {
        const val MAX_BODY_BYTES = 512L * 1024
    }
}
