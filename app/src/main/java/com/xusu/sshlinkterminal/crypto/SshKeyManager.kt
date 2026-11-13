package com.xusu.sshlinkterminal.crypto

import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.xusu.sshlinkterminal.data.SshKeyRecord
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

object SshKeyManager {

    data class GeneratedKeyMaterial(
        val record: SshKeyRecord,
        val publicKeyLine: String
    )

    fun generateEd25519Key(name: String, comment: String = "linkterm@android"): GeneratedKeyMaterial {
        val jsch = JSch()
        val keyPair = KeyPair.genKeyPair(jsch, KeyPair.ED25519)
        return try {
            val privOut = ByteArrayOutputStream()
            val pubOut = ByteArrayOutputStream()
            keyPair.writeOpenSSHv1PrivateKey(privOut, null)
            keyPair.writePublicKey(pubOut, comment)
            val privateKeyPem = privOut.toString(StandardCharsets.UTF_8.name())
            val publicKeyOpenSsh = pubOut.toString(StandardCharsets.UTF_8.name()).trim()
            val fingerprint = computeSha256Fingerprint(keyPair.publicKeyBlob)
            val record = SshKeyRecord(
                name = name,
                algorithm = "Ed25519",
                fingerprintSha256 = fingerprint,
                publicKeyOpenSsh = publicKeyOpenSsh,
                encryptedPrivateKey = VaultCrypto.encrypt(privateKeyPem),
                hasPassphrase = false
            )
            GeneratedKeyMaterial(record = record, publicKeyLine = publicKeyOpenSsh)
        } finally {
            keyPair.dispose()
        }
    }

    fun importPrivateKey(name: String, privateKeyText: String, passphrase: String? = null): GeneratedKeyMaterial {
        val jsch = JSch()
        val keyBytes = privateKeyText.trim().toByteArray(StandardCharsets.UTF_8)
        val passBytes = passphrase?.takeIf { it.isNotEmpty() }?.toByteArray(StandardCharsets.UTF_8)
        val keyPair = KeyPair.load(jsch, keyBytes, null)
        return try {
            if (keyPair.isEncrypted) {
                require(passBytes != null && keyPair.decrypt(passBytes)) {
                    "私钥已加密，请输入正确的口令（Passphrase）"
                }
            }
            val pubOut = ByteArrayOutputStream()
            keyPair.writePublicKey(pubOut, name)
            val publicKeyOpenSsh = pubOut.toString(StandardCharsets.UTF_8.name()).trim()
            val algo = when (keyPair.keyType) {
                KeyPair.ED25519 -> "Ed25519"
                KeyPair.ED448 -> "Ed448"
                KeyPair.ECDSA -> "ECDSA"
                KeyPair.RSA -> "RSA"
                else -> "SSH Key"
            }
            val fingerprint = computeSha256Fingerprint(keyPair.publicKeyBlob)
            val record = SshKeyRecord(
                name = name,
                algorithm = algo,
                fingerprintSha256 = fingerprint,
                publicKeyOpenSsh = publicKeyOpenSsh,
                encryptedPrivateKey = VaultCrypto.encrypt(privateKeyText.trim()),
                hasPassphrase = !passphrase.isNullOrEmpty()
            )
            GeneratedKeyMaterial(record = record, publicKeyLine = publicKeyOpenSsh)
        } finally {
            keyPair.dispose()
        }
    }

    fun buildInstallPublicKeyCommand(publicKeyLine: String): String {
        val sanitized = publicKeyLine.trim().replace("'", "'\"'\"'")
        return "mkdir -p ~/.ssh && chmod 700 ~/.ssh && grep -qxF '$sanitized' ~/.ssh/authorized_keys 2>/dev/null || echo '$sanitized' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
    }

    private fun computeSha256Fingerprint(blob: ByteArray?): String {
        if (blob == null || blob.isEmpty()) return "SHA256:unknown"
        val digest = MessageDigest.getInstance("SHA-256").digest(blob)
        val b64 = Base64.getEncoder().withoutPadding().encodeToString(digest)
        return "SHA256:$b64"
    }
}
