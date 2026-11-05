package com.watchinstead.app.domain

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** Extracts and canonicalises links from the free-form text that apps put in share intents. */
object LinkParser {
    private val URL_PATTERN = Regex("""(?:https?://|www\.)[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private const val TRAILING_PUNCTUATION = ".,;:!?)]}"

    private val UNIVERSAL_TRACKING_PARAMS = setOf("fbclid", "gclid")

    // Only stripped on known platforms: on other sites a parameter like "s" or "ref" can be meaningful.
    private val PLATFORM_TRACKING_PARAMS = setOf(
        "igsh", "igshid", "si", "feature", "pp", "ref", "ref_src", "ref_url",
        "is_from_webapp", "sender_device", "sender_web_id", "_t", "_r", "share_id", "share_app_id",
        "share_item_id", "tt_from", "u_code", "web_id", "s", "t_medium",
    )

    fun parse(text: String): ParsedLink? {
        val raw = URL_PATTERN.find(text)?.value?.trimEnd { it in TRAILING_PUNCTUATION } ?: return null
        val withScheme = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
        val uri = try {
            URI(withScheme)
        } catch (_: URISyntaxException) {
            return null
        }
        val host = uri.host?.lowercase(Locale.ROOT)?.takeIf { '.' in it } ?: return null
        val scheme = uri.scheme.lowercase(Locale.ROOT)
        val platform = platformOf(host)
        return ParsedLink(canonicalise(scheme, host, uri, platform), platform)
    }

    fun platformOf(host: String): Platform = when {
        host.isOrSubdomainOf("youtube.com") || host == "youtu.be" -> Platform.YOUTUBE
        host.isOrSubdomainOf("instagram.com") -> Platform.INSTAGRAM
        host.isOrSubdomainOf("tiktok.com") -> Platform.TIKTOK
        host.isOrSubdomainOf("x.com") || host.isOrSubdomainOf("twitter.com") -> Platform.X
        else -> Platform.OTHER
    }

    private fun canonicalise(scheme: String, host: String, uri: URI, platform: Platform): String {
        var path = uri.rawPath.orEmpty()
        val params = uri.rawQuery.orEmpty().split('&').filter { param ->
            val key = param.substringBefore('=').lowercase(Locale.ROOT)
            val tracked = key.startsWith("utm_") || key in UNIVERSAL_TRACKING_PARAMS ||
                (platform != Platform.OTHER && key in PLATFORM_TRACKING_PARAMS)
            param.isNotEmpty() && !tracked
        }.toMutableList()

        var canonicalHost = host
        if (platform == Platform.YOUTUBE) {
            if (host == "youtu.be" && path.length > 1) {
                params.add(0, "v=${path.drop(1).trimEnd('/')}")
                path = "/watch"
            }
            canonicalHost = "www.youtube.com"
        }
        if (platform == Platform.INSTAGRAM) canonicalHost = "www.instagram.com"
        if (path.length > 1) path = path.trimEnd('/')

        val port = if (uri.port != -1) ":${uri.port}" else ""
        val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        return "$scheme://$canonicalHost$port$path$query"
    }

    private fun String.isOrSubdomainOf(domain: String) = this == domain || endsWith(".$domain")
}
