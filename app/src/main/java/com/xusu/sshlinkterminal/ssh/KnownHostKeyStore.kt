package com.xusu.sshlinkterminal.ssh

import com.jcraft.jsch.HostKey
import com.jcraft.jsch.JSchException
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

data class KnownHostKey(
    val endpoint: String,
    val algorithm: String,
    val publicKeyBase64: String
) {
    val fingerprintSha256: String
        get() = "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(Base64.getDecoder().decode(publicKeyBase64))
        )
}

class HostKeyVerificationException(message: String, cause: Throwable? = null) : JSchException(message, cause)

/** One public host key per endpoint. Persistence must succeed before authentication is allowed. */
class KnownHostKeyStore(
    private val readEntry: (String) -> String?,
    private val writeEntry: (String, String?) -> Boolean
) {
    private val lock = Any()
    private val resetGenerations = mutableMapOf<String, Long>()
    private val failedPersistence = mutableSetOf<String>()

    fun read(host: String, port: Int): KnownHostKey? = synchronized(lock) {
        readEndpoint(endpoint(host, port))
    }

    fun forget(host: String, port: Int) = synchronized(lock) {
        val endpoint = endpoint(host, port)
        if (!writeEntry(endpoint, null)) {
            failedPersistence.add(endpoint)
            throw HostKeyVerificationException("无法清除已保存的主机密钥，请检查设备存储后重试")
        }
        failedPersistence.remove(endpoint)
        // Invalidate confirmations that were displayed before the user's reset.
        resetGenerations[endpoint] = (resetGenerations[endpoint] ?: 0L) + 1L
    }

    fun verify(
        host: String,
        port: Int,
        publicKey: ByteArray,
        confirmFirstConnection: (KnownHostKey) -> Boolean
    ): KnownHostKey {
        val endpoint = endpoint(host, port)
        val candidate = KnownHostKey(
            endpoint = endpoint,
            algorithm = HostKey(endpoint, publicKey).type,
            publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey)
        )
        val (saved, resetGeneration) = synchronized(lock) {
            readEndpoint(endpoint) to (resetGenerations[endpoint] ?: 0L)
        }
        if (saved != null) {
            requireSameKey(saved, candidate)
            return saved
        }
        if (!confirmFirstConnection(candidate)) {
            throw HostKeyVerificationException("未确认 $endpoint 的主机指纹，连接已取消；请手动重新连接")
        }
        synchronized(lock) {
            if ((resetGenerations[endpoint] ?: 0L) != resetGeneration) {
                throw HostKeyVerificationException("$endpoint 的主机密钥记录已重置，请重新连接并确认指纹")
            }
            // Two sessions may ask about the same endpoint concurrently; never replace a different key.
            val current = readEndpoint(endpoint)
            if (current != null) {
                requireSameKey(current, candidate)
            } else {
                val encoded = JSONObject().apply {
                    put("endpoint", candidate.endpoint)
                    put("algorithm", candidate.algorithm)
                    put("publicKey", candidate.publicKeyBase64)
                }.toString()
                if (!writeEntry(endpoint, encoded)) {
                    // SharedPreferences.commit() can update memory even when its disk write fails.
                    failedPersistence.add(endpoint)
                    throw HostKeyVerificationException("无法保存 $endpoint 的主机密钥，连接已阻止；请检查设备存储")
                }
            }
        }
        return candidate
    }

    private fun readEndpoint(endpoint: String): KnownHostKey? {
        if (endpoint in failedPersistence) {
            throw HostKeyVerificationException(
                "$endpoint 的主机密钥写入失败，连接已阻止。请检查设备存储，在编辑主机中清除记录后重新核对指纹"
            )
        }
        val raw = readEntry(endpoint) ?: return null
        try {
            val json = JSONObject(raw)
            val key = Base64.getDecoder().decode(json.getString("publicKey"))
            val record = KnownHostKey(
                endpoint = json.getString("endpoint"),
                algorithm = json.getString("algorithm"),
                publicKeyBase64 = Base64.getEncoder().encodeToString(key)
            )
            require(record.endpoint == endpoint && record.algorithm == HostKey(endpoint, key).type)
            return record
        } catch (error: Exception) {
            // Corrupt trust data must not turn a known endpoint into an unverified first connection.
            throw HostKeyVerificationException(
                "$endpoint 的主机密钥记录无法读取。请在编辑主机中清除记录，再核对服务器指纹后重新连接", error
            )
        }
    }

    private fun requireSameKey(saved: KnownHostKey, candidate: KnownHostKey) {
        if (saved.publicKeyBase64 != candidate.publicKeyBase64) {
            throw HostKeyVerificationException(
                "${candidate.endpoint} 的主机密钥已变化，连接已阻止。\n" +
                    "已保存 (${saved.algorithm})：${saved.fingerprintSha256}\n" +
                    "本次 (${candidate.algorithm})：${candidate.fingerprintSha256}\n" +
                    "请通过可信渠道核对服务器指纹；确认服务器更换密钥后，在编辑主机中清除旧记录并重新连接"
            )
        }
    }

    companion object {
        fun endpoint(host: String, port: Int): String =
            "[${host.trim().removePrefix("[").removeSuffix("]").lowercase(Locale.ROOT)}]:$port"
    }
}
