package com.xusu.sshlinkterminal.ssh

import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.UserInfo
import java.util.Base64

/** JSch calls check before sending any credentials; use with StrictHostKeyChecking=yes. */
internal class PinnedHostKeyRepository(
    private val store: KnownHostKeyStore,
    private val host: String,
    private val port: Int,
    private val confirmFirstConnection: (KnownHostKey) -> Boolean
) : HostKeyRepository {
    private val endpoint = KnownHostKeyStore.endpoint(host, port)
    private var verifiedKey: HostKey? = null

    override fun check(host: String, key: ByteArray): Int {
        if (host != endpoint) throw HostKeyVerificationException("主机密钥校验地址不一致，连接已阻止")
        val verified = store.verify(this.host, port, key, confirmFirstConnection)
        verifiedKey = HostKey(endpoint, Base64.getDecoder().decode(verified.publicKeyBase64))
        return HostKeyRepository.OK
    }

    override fun getHostKey(): Array<HostKey> = getHostKey(null, null)

    override fun getHostKey(host: String?, type: String?): Array<HostKey> {
        val key = verifiedKey ?: return emptyArray()
        return if ((host == null || host == endpoint) && (type == null || type == key.type)) {
            arrayOf(key)
        } else {
            emptyArray()
        }
    }

    override fun getKnownHostsRepositoryID(): String = "LinkTerm 已保存的主机指纹"

    // Automatic insertion/removal would bypass the explicit confirmation policy.
    override fun add(hostkey: HostKey, ui: UserInfo?) {
        throw UnsupportedOperationException("主机密钥只能通过显式指纹确认保存")
    }

    override fun remove(host: String?, type: String?) {
        throw UnsupportedOperationException("请通过编辑主机清除主机密钥")
    }

    override fun remove(host: String?, type: String?, key: ByteArray?) {
        throw UnsupportedOperationException("请通过编辑主机清除主机密钥")
    }
}
