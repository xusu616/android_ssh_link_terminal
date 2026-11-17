package com.xusu.sshlinkterminal.ssh

import com.xusu.sshlinkterminal.data.HostProfile
import com.xusu.sshlinkterminal.data.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SshSessionManagerPolicyTest {

    private fun host(id: String): HostProfile = HostProfile(
        id = id,
        name = "Host $id",
        host = "10.0.0.1",
        username = "root"
    )

    @Test
    fun `reconnectAll policy protects connected and authenticating sessions`() {
        assertFalse(SshSessionManager.shouldReconnectOnAction(SessionState.CONNECTED, pinInDaemon = true))
        assertFalse(SshSessionManager.shouldReconnectOnAction(SessionState.CONNECTING, pinInDaemon = true))
        assertFalse(SshSessionManager.shouldReconnectOnAction(SessionState.AWAITING_INPUT, pinInDaemon = true))
        assertFalse(SshSessionManager.shouldReconnectOnAction(SessionState.AWAITING_OAUTH, pinInDaemon = true))

        assertTrue(SshSessionManager.shouldReconnectOnAction(SessionState.RECONNECTING, pinInDaemon = false))
        assertTrue(SshSessionManager.shouldReconnectOnAction(SessionState.DISCONNECTED, pinInDaemon = false))
        assertTrue(SshSessionManager.shouldReconnectOnAction(SessionState.ERROR, pinInDaemon = false))
        assertTrue(SshSessionManager.shouldReconnectOnAction(SessionState.IDLE, pinInDaemon = true))
        assertFalse(SshSessionManager.shouldReconnectOnAction(SessionState.IDLE, pinInDaemon = false))
    }

    @Test
    fun `resolveHostSelection preserves active session host instead of resetting to first host`() {
        val hosts = listOf(host("h1"), host("h2"), host("h3"))

        assertEquals(
            "h2",
            SshSessionManager.resolveHostSelection(
                hosts = hosts,
                requestedHostId = null,
                activeSessionHostId = "h2",
                activeHostId = "h3",
                liveSessionHostIds = setOf("h1", "h2")
            )?.id
        )

        assertEquals(
            "h3",
            SshSessionManager.resolveHostSelection(
                hosts = hosts,
                requestedHostId = null,
                activeSessionHostId = "deleted-host",
                activeHostId = "h3",
                liveSessionHostIds = setOf("h1")
            )?.id
        )

        assertEquals(
            "h2",
            SshSessionManager.resolveHostSelection(
                hosts = hosts,
                requestedHostId = null,
                activeSessionHostId = null,
                activeHostId = null,
                liveSessionHostIds = setOf("h2")
            )?.id
        )

        assertEquals(
            "h3",
            SshSessionManager.resolveHostSelection(
                hosts = hosts,
                requestedHostId = "h3",
                activeSessionHostId = "h2",
                activeHostId = "h2",
                liveSessionHostIds = setOf("h1")
            )?.id
        )

        assertNull(
            SshSessionManager.resolveHostSelection(
                hosts = emptyList(),
                requestedHostId = null,
                activeSessionHostId = "h1",
                activeHostId = "h1",
                liveSessionHostIds = setOf("h1")
            )
        )
    }
}
