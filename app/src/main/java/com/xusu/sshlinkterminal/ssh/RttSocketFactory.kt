package com.xusu.sshlinkterminal.ssh

import com.jcraft.jsch.SocketFactory
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * JSch socket factory that stamps the arrival time of every inbound chunk.
 *
 * `Session.sendKeepAliveMsg()` only enqueues the request and returns immediately, so timing the call
 * measures local socket write latency (~1 ms) rather than network round trip. By recording when the
 * server's reply (or any other inbound traffic) lands on the socket we can measure a real RTT.
 */
class RttSocketFactory(
    private val connectTimeoutMs: Int = 20_000,
    private val clock: () -> Long = System::nanoTime
) : SocketFactory {

    @Volatile
    var lastInboundNanos: Long = 0L
        private set

    override fun createSocket(host: String, port: Int): Socket {
        val socket = Socket()
        socket.tcpNoDelay = true
        socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
        return socket
    }

    override fun getInputStream(socket: Socket): InputStream =
        object : FilterInputStream(socket.getInputStream()) {
            override fun read(): Int {
                val b = super.read()
                if (b >= 0) lastInboundNanos = clock()
                return b
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = super.read(b, off, len)
                if (n > 0) lastInboundNanos = clock()
                return n
            }
        }

    override fun getOutputStream(socket: Socket): OutputStream = socket.getOutputStream()

    /** Milliseconds between [sentAtNanos] and the first inbound traffic after it, or null if none yet. */
    fun inboundDelayMsSince(sentAtNanos: Long): Long? {
        val inbound = lastInboundNanos
        if (inbound <= sentAtNanos) return null
        return ((inbound - sentAtNanos) / 1_000_000L).coerceAtLeast(1L)
    }
}
