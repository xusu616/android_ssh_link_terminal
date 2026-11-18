package com.xusu.sshlinkterminal.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicLong

class RttSocketFactoryTest {

    @Test
    fun `stamps inbound arrival time and ignores traffic older than the probe`() {
        val fakeNow = AtomicLong(1_000_000_000L)
        val factory = RttSocketFactory(connectTimeoutMs = 2_000, clock = { fakeNow.get() })
        ServerSocket(0).use { server ->
            val socket = factory.createSocket("127.0.0.1", server.localPort)
            val accepted = server.accept()
            try {
                val input = factory.getInputStream(socket)

                // Nothing received yet -> no RTT sample available.
                assertEquals(0L, factory.lastInboundNanos)
                assertNull(factory.inboundDelayMsSince(500_000_000L))

                // Probe is sent at t=1.000s, server reply lands at t=1.037s.
                val sentAt = 1_000_000_000L
                fakeNow.set(1_037_000_000L)
                accepted.getOutputStream().apply { write(byteArrayOf(1, 2, 3)); flush() }
                val buf = ByteArray(8)
                val n = input.read(buf, 0, buf.size)
                assertEquals(3, n)

                assertEquals(1_037_000_000L, factory.lastInboundNanos)
                assertEquals(37L, factory.inboundDelayMsSince(sentAt))

                // Traffic that arrived before a later probe must not be counted for it.
                assertNull(factory.inboundDelayMsSince(2_000_000_000L))

                // Single-byte read path also stamps.
                fakeNow.set(2_500_000_000L)
                accepted.getOutputStream().apply { write(7); flush() }
                assertEquals(7, input.read())
                assertEquals(500L, factory.inboundDelayMsSince(2_000_000_000L))
                assertNotNull(factory.getOutputStream(socket))
                assertTrue(socket.tcpNoDelay)
            } finally {
                accepted.close()
                socket.close()
            }
        }
    }
}
