package com.xusu.sshlinkterminal.shizuku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuDaemonServiceTest {

    @Test
    fun `isPackageInWhitelistOutput matches Android 15 comma-uid format`() {
        val sampleOutput = """
            system-excidle,com.android.providers.calendar,10226
            user,com.xusu.sshlinkterminal,10544
        """.trimIndent()

        assertTrue(
            ShizukuDaemonService.isPackageInWhitelistOutput(
                sampleOutput,
                "com.xusu.sshlinkterminal"
            )
        )
        assertFalse(
            ShizukuDaemonService.isPackageInWhitelistOutput(
                sampleOutput,
                "com.xusu.other"
            )
        )
    }

    @Test
    fun `isPackageInWhitelistOutput handles legacy formats without uid suffix`() {
        val legacyOutput = """
            system,com.android.shell
            whitelist:com.xusu.sshlinkterminal
        """.trimIndent()

        assertTrue(
            ShizukuDaemonService.isPackageInWhitelistOutput(
                legacyOutput,
                "com.xusu.sshlinkterminal"
            )
        )
    }
}
