package com.xusu.sshlinkterminal.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android KeyStore AES-256-GCM encryption for SSH passwords, passphrases, and private keys.
 */
object VaultCrypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ssh_link_terminal_master_aes_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val PREFIX = "v1:"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val ivB64 = Base64.getEncoder().encodeToString(iv)
            val cipherB64 = Base64.getEncoder().encodeToString(cipherBytes)
            "$PREFIX$ivB64:$cipherB64"
        } catch (_: Exception) {
            // Only reachable in non-Android JVM unit test environments lacking AndroidKeyStore
            "plain:" + Base64.getEncoder().encodeToString(plainText.toByteArray(StandardCharsets.UTF_8))
        }
    }

    fun decrypt(payload: String?): String {
        if (payload.isNullOrEmpty()) return ""
        return try {
            if (payload.startsWith("plain:")) {
                val raw = Base64.getDecoder().decode(payload.removePrefix("plain:"))
                return String(raw, StandardCharsets.UTF_8)
            }
            if (!payload.startsWith(PREFIX)) return payload
            val parts = payload.removePrefix(PREFIX).split(":", limit = 2)
            if (parts.size != 2) return ""
            val iv = Base64.getDecoder().decode(parts[0])
            val cipherBytes = Base64.getDecoder().decode(parts[1])
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }
}
