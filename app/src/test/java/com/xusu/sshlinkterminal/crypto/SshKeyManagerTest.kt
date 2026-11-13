package com.xusu.sshlinkterminal.crypto

import com.xusu.sshlinkterminal.crypto.VaultCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SshKeyManagerTest {

    @Test
    fun `generates valid Ed25519 key material and round-trips via import`() {
        val generated = SshKeyManager.generateEd25519Key("test-key", "linkterm@test")
        assertTrue(generated.publicKeyLine.startsWith("ssh-ed25519 "))
        assertTrue(generated.record.fingerprintSha256.startsWith("SHA256:"))
        assertEquals("Ed25519", generated.record.algorithm)

        val decryptedPem = VaultCrypto.decrypt(generated.record.encryptedPrivateKey)
        assertTrue(decryptedPem.contains("BEGIN OPENSSH PRIVATE KEY"))

        val reimported = SshKeyManager.importPrivateKey("reimported", decryptedPem)
        assertEquals(generated.record.fingerprintSha256, reimported.record.fingerprintSha256)
    }

    @Test
    fun `escapes single quotes in authorized_keys deploy command`() {
        val cmd = SshKeyManager.buildInstallPublicKeyCommand("ssh-ed25519 AAAAC3 user's@phone")
        assertTrue(cmd.contains("~/.ssh/authorized_keys"))
        assertTrue(cmd.contains("'\"'\"'"))
    }
}
