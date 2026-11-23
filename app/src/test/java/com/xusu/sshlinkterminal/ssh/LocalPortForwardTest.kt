package com.xusu.sshlinkterminal.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalPortForwardTest {
    @Test
    fun `parses the forwarding argument with or without an explicit loopback address`() {
        val expected = LocalPortForward(18765, "127.0.0.1", 8765)

        assertEquals(expected, LocalPortForward.parse("18765:127.0.0.1:8765"))
        assertEquals(expected, LocalPortForward.parse("127.0.0.1:18765:127.0.0.1:8765"))
        assertEquals("127.0.0.1:18765", expected.localAddress)
        assertEquals(LocalPortForward(18765, "::1", 8765), LocalPortForward.parse("18765:[::1]:8765"))
    }

    @Test
    fun `rejects invalid ports and non-loopback listeners`() {
        assertNull(LocalPortForward.parse("80:127.0.0.1:8765"))
        assertNull(LocalPortForward.parse("18765:127.0.0.1:65536"))
        assertNull(LocalPortForward.parse("0.0.0.0:18765:127.0.0.1:8765"))
        assertNull(LocalPortForward.parse("18765::8765"))
        assertNull(LocalPortForward.parse(""))
    }
}
