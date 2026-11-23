package com.xusu.sshlinkterminal.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRepositoryPersistenceTest {

    @Test
    fun `round-trips RTT samples and filters invalid entries`() {
        val encoded = AppRepository.encodeRttSamplesJson(
            listOf(
                RttSample(timestampSec = 1_700_000_001L, rttMs = 12L, activeSessions = 1),
                RttSample(timestampSec = 1_700_000_031L, rttMs = 45L, activeSessions = 2)
            )
        )
        val decoded = AppRepository.decodeRttSamplesJson(encoded)
        assertEquals(2, decoded.size)
        assertEquals(12L, decoded[0].rttMs)
        assertEquals(45L, decoded[1].rttMs)
        assertEquals(emptyList<RttSample>(), AppRepository.decodeRttSamplesJson("invalid-json"))
    }

    @Test
    fun `round-trips persisted logs cleanly`() {
        val entries = listOf(
            AppLogEntry(timestampMillis = 123456L, level = "WARN", tag = "SSH", message = "连接中断 - timeout"),
            AppLogEntry(timestampMillis = 123999L, level = "INFO", tag = "Daemon", message = "系统重启恢复")
        )
        val decoded = AppRepository.decodeLogsJson(AppRepository.encodeLogsJson(entries))
        assertEquals(2, decoded.size)
        assertEquals("SSH", decoded[0].tag)
        assertEquals("系统重启恢复", decoded[1].message)
    }
}
