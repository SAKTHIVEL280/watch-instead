package com.watchinstead.app.data.metadata

data class LinkMetadata(val title: String?, val thumbnailUrl: String?)

/** Reads Open Graph tags from an HTML head, tolerating either attribute order. */
object OpenGraphParser {
    private val META_TAG = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val PROPERTY = Regex("""(?:property|name)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val CONTENT = Regex("""content\s*=\s*"([^"]*)"|content\s*=\s*'([^']*)'""", RegexOption.IGNORE_CASE)

    fun parse(html: String): LinkMetadata {
        val tags = META_TAG.findAll(html).mapNotNull { match ->
            val property = PROPERTY.find(match.value)?.groupValues?.get(1)?.lowercase()
            val content = CONTENT.find(match.value)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
            if (property != null && content != null) property to unescape(content) else null
        }.toMap()
        return LinkMetadata(
            title = tags["og:title"]?.takeIf { it.isNotBlank() },
            thumbnailUrl = tags["og:image"]?.takeIf { it.startsWith("https://") },
        )
    }

    private fun unescape(value: String) = value
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
}
