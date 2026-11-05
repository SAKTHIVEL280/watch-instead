package com.watchinstead.app.data

import com.watchinstead.app.data.metadata.OpenGraphParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenGraphParserTest {
    @Test fun `reads tags in either attribute order and unescapes`() {
        val html = """
            <meta property="og:title" content="Tom &amp; Jerry's talk">
            <meta content='https://cdn.example.com/a.jpg' property='og:image' />
        """
        val result = OpenGraphParser.parse(html)
        assertEquals("Tom & Jerry's talk", result.title)
        assertEquals("https://cdn.example.com/a.jpg", result.thumbnailUrl)
    }

    @Test fun `insecure images and empty pages give nothing`() {
        assertNull(OpenGraphParser.parse("""<meta property="og:image" content="http://a/b.jpg">""").thumbnailUrl)
        assertNull(OpenGraphParser.parse("<html></html>").title)
    }
}
