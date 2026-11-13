package com.xusu.sshlinkterminal.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LinkDetectorTest {

    @Test
    fun `detects Tailscale SSH check URL and trims trailing punctuation`() {
        val banner = """
            # Tailscale SSH requires an additional check.
            # To authenticate, visit: https://login.tailscale.com/a/check_abc123XYZ.
        """.trimIndent()

        val detected = LinkDetector.detectAuthLink(banner)
        assertNotNull(detected)
        assertEquals("https://login.tailscale.com/a/check_abc123XYZ", detected!!.url)
        assertEquals("Tailscale SSH / 节点验证", detected.providerLabel)
    }

    @Test
    fun `detects Google or Antigravity OAuth URL in terminal output`() {
        val output = "Open this URL to authenticate Antigravity: (https://accounts.google.com/o/oauth2/v2/auth?client_id=123&scope=email)"
        val detected = LinkDetector.detectAuthLink(output)
        assertNotNull(detected)
        assertEquals(
            "https://accounts.google.com/o/oauth2/v2/auth?client_id=123&scope=email",
            detected!!.url
        )
        assertEquals("Google / Antigravity OAuth", detected.providerLabel)
    }

    @Test
    fun `ignores normal documentation links without auth intent`() {
        val text = "Documentation available at https://example.com/docs/getting-started"
        val detected = LinkDetector.detectAuthLink(text)
        assertNull(detected)

        val urls = LinkDetector.extractUrls(text)
        assertEquals(1, urls.size)
        assertEquals("https://example.com/docs/getting-started", urls[0].url)
    }
}
