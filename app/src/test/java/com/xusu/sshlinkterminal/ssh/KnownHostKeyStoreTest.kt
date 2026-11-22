package com.xusu.sshlinkterminal.ssh

import com.jcraft.jsch.HostKeyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class KnownHostKeyStoreTest {
    private val entries = mutableMapOf<String, String>()

    private fun store() = KnownHostKeyStore(
        readEntry = entries::get,
        writeEntry = { endpoint, value ->
            if (value == null) entries.remove(endpoint) else entries[endpoint] = value
            true
        }
    )

    private fun key(seed: Int): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use {
            val algorithm = "ssh-ed25519".toByteArray(Charsets.US_ASCII)
            it.writeInt(algorithm.size)
            it.write(algorithm)
            it.writeInt(32)
            it.write(ByteArray(32) { seed.toByte() })
        }
        return output.toByteArray()
    }

    private fun expectBlocked(action: () -> Unit): HostKeyVerificationException {
        try {
            action()
            fail("Connection must be blocked")
        } catch (expected: HostKeyVerificationException) {
            return expected
        }
        error("unreachable")
    }

    @Test
    fun `first connection requires confirmation and persists across store recreation`() {
        var prompts = 0
        val saved = store().verify(" Server.Example ", 22, key(1)) {
            prompts++
            assertEquals("[server.example]:22", it.endpoint)
            assertEquals("ssh-ed25519", it.algorithm)
            assertTrue(it.fingerprintSha256.startsWith("SHA256:"))
            assertFalse(it.fingerprintSha256.endsWith("="))
            assertTrue(entries.isEmpty())
            true
        }
        val reloaded = store().verify("server.example", 22, key(1)) {
            fail("Known key must not prompt again")
            false
        }
        assertEquals(1, prompts)
        assertEquals(saved, reloaded)
    }

    @Test
    fun `declining confirmation neither saves nor accepts the key`() {
        expectBlocked { store().verify("server", 22, key(1)) { false } }
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `changed key is rejected without offering acceptance or replacing the saved key`() {
        val store = store()
        val original = store.verify("server", 22, key(1)) { true }
        val error = expectBlocked {
            store.verify("server", 22, key(2)) {
                fail("Changed key must not be offered as first connection")
                true
            }
        }
        assertTrue(error.message!!.contains(original.fingerprintSha256))
        assertTrue(error.message!!.contains("编辑主机"))
        assertEquals(original, store.read("server", 22))
    }

    @Test
    fun `same address on different ports has independent trust`() {
        val store = store()
        val first = store.verify("server", 22, key(1)) { true }
        val second = store.verify("server", 2222, key(2)) { true }
        assertEquals(first, store.read("server", 22))
        assertEquals(second, store.read("server", 2222))
    }

    @Test
    fun `reset permits a new explicit confirmation and only clears its endpoint`() {
        val store = store()
        store.verify("server", 22, key(1)) { true }
        store.verify("server", 2222, key(1)) { true }
        store.forget("server", 22)
        assertNull(store.read("server", 22))
        assertTrue(store.read("server", 2222) != null)
        var confirmed = false
        store.verify("server", 22, key(2)) { confirmed = true; true }
        assertTrue(confirmed)
    }

    @Test
    fun `a confirmation shown before reset cannot restore the old key`() {
        val store = store()
        expectBlocked {
            store.verify("server", 22, key(1)) {
                store.forget("server", 22)
                true
            }
        }
        assertNull(store.read("server", 22))
    }

    @Test
    fun `concurrent confirmations never replace a different key`() {
        val store = store()
        var winner: KnownHostKey? = null
        expectBlocked {
            store.verify("server", 22, key(1)) {
                winner = store.verify("server", 22, key(2)) { true }
                true
            }
        }
        assertEquals(winner, store.read("server", 22))
    }

    @Test
    fun `corrupt saved data blocks instead of prompting to trust again`() {
        entries[KnownHostKeyStore.endpoint("server", 22)] = "broken-json"
        expectBlocked {
            store().verify("server", 22, key(1)) {
                fail("Corrupt trust data must not be treated as a new host")
                true
            }
        }
        assertEquals("broken-json", entries.values.single())
    }

    @Test
    fun `failed persistence prevents accepting a confirmed key`() {
        val store = KnownHostKeyStore(readEntry = { null }, writeEntry = { _, _ -> false })
        expectBlocked { store.verify("server", 22, key(1)) { true } }
    }

    @Test
    fun `failed disk write cannot turn an in-memory key into a trusted key on retry`() {
        var persistenceSucceeds = false
        val store = KnownHostKeyStore(readEntry = entries::get, writeEntry = { endpoint, value ->
            if (value == null) entries.remove(endpoint) else entries[endpoint] = value
            persistenceSucceeds
        })
        expectBlocked { store.verify("server", 22, key(1)) { true } }
        assertTrue(entries.isNotEmpty())
        expectBlocked {
            store.verify("server", 22, key(1)) {
                fail("Failed persistence must be resolved before trusting a key")
                true
            }
        }
        persistenceSucceeds = true
        store.forget("server", 22)
        store.verify("server", 22, key(1)) { true }
        assertTrue(store.read("server", 22) != null)
    }

    @Test
    fun `JSch repository accepts only verified endpoint and exposes its matching public key`() {
        val repository = PinnedHostKeyRepository(store(), "server", 2222) { true }
        expectBlocked { repository.check("server", key(1)) }
        assertEquals(HostKeyRepository.OK, repository.check("[server]:2222", key(1)))
        assertEquals(1, repository.getHostKey("[server]:2222", "ssh-ed25519").size)
        assertEquals(0, repository.getHostKey("[server]:22", "ssh-ed25519").size)
        expectBlocked { repository.check("[server]:2222", key(2)) }
    }
}
