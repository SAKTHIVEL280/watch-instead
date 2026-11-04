package com.lockin.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkParserTest {
    @Test fun `instagram reel loses share token and trailing slash`() {
        val link = LinkParser.parse("https://www.instagram.com/reel/C8abc123/?igsh=MWx4eHg=")!!
        assertEquals("https://www.instagram.com/reel/C8abc123", link.url)
        assertEquals(Platform.INSTAGRAM, link.platform)
    }

    @Test fun `tiktok link is pulled out of surrounding share text`() {
        val text = "Check out this video on TikTok! https://vm.tiktok.com/ZMabc123/ Download the app."
        val link = LinkParser.parse(text)!!
        assertEquals("https://vm.tiktok.com/ZMabc123", link.url)
        assertEquals(Platform.TIKTOK, link.platform)
    }

    @Test fun `youtube short link matches the full watch link`() {
        val short = LinkParser.parse("https://youtu.be/dQw4w9WgXcQ?si=AbCdEf")!!
        val full = LinkParser.parse("https://m.youtube.com/watch?v=dQw4w9WgXcQ&feature=shared")!!
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", short.url)
        assertEquals(short.url, full.url)
    }

    @Test fun `meaningful query parameters survive`() {
        val link = LinkParser.parse("https://youtube.com/watch?v=abc&t=42&utm_source=x")!!
        assertEquals("https://www.youtube.com/watch?v=abc&t=42", link.url)
    }

    @Test fun `ordinary sites keep parameters that are only tracking on social platforms`() {
        assertEquals("https://example.com/find?s=focus", LinkParser.parse("https://example.com/find?s=focus&utm_source=a")!!.url)
    }

    @Test fun `bare www link gets https and trailing punctuation is dropped`() {
        assertEquals("https://www.example.com/talk", LinkParser.parse("see www.example.com/talk.")!!.url)
    }

    @Test fun `lookalike domains are not treated as a platform`() {
        assertEquals(Platform.OTHER, LinkParser.parse("https://notyoutube.com/a")!!.platform)
    }

    @Test fun `text without a usable link returns null`() {
        assertNull(LinkParser.parse("no link here"))
        assertNull(LinkParser.parse("ftp://example.com/file"))
        assertNull(LinkParser.parse("https://localhost/x"))
        assertNull(LinkParser.parse("https://exa mple.com"))
    }
}
